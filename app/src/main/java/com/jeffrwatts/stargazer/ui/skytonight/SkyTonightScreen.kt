package com.jeffrwatts.stargazer.ui.skytonight

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.getDefaultImageResource
import com.jeffrwatts.stargazer.utils.AppConstants.DATE_TIME_FORMATTER
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.PermissionWrapper
import com.jeffrwatts.stargazer.utils.TimeControl
import com.jeffrwatts.stargazer.utils.formatHoursToHoursMinutes
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes
import java.io.File
import java.time.LocalDateTime
import java.util.Locale


@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class
)
@Composable
fun SkyTonightScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int, LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SkyTonightViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val skyTonightUiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.recommendedFilter.collectAsState()
    val isRefreshing = skyTonightUiState is SkyTonightUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.refresh() })

    Scaffold(
        topBar = {
            SkyTonightTopAppBar(
                title = stringResource(R.string.sky_tonight),
                openDrawer = openDrawer,
                onFilterSelected = { newFilter ->
                    viewModel.setRecommendedFilter(newFilter)
                },
                currentFilter = currentFilter,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        PermissionWrapper(
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            ),
            rationaleMessage = stringResource(id = R.string.permission_rationale)
        ) {
            LaunchedEffect(Unit) {
                viewModel.startLocationUpdates()
            }

            Box(Modifier.pullRefresh(pullRefreshState)) {
                when (skyTonightUiState) {
                    is SkyTonightUiState.Loading -> {
                        LoadingScreen(modifier = contentModifier)
                    }

                    is SkyTonightUiState.Success -> {
                        val successState = skyTonightUiState as SkyTonightUiState.Success
                        Column(modifier = contentModifier) {
                            TimeControl(
                                currentTime = successState.currentTime.format(DATE_TIME_FORMATTER),
                                onIncrementHour = { viewModel.incrementOffset(1) },
                                onDecrementHour = { viewModel.decrementOffset(1) },
                                onIncrementDay = { viewModel.incrementOffset(24) },
                                onDecrementDay = { viewModel.decrementOffset(24) },
                                onResetTime = { viewModel.resetOffset() })
                            SkyTonightBody(
                                celestialObjPosList = successState.data,
                                moonIllumination = successState.moonIllumination,
                                locationAvailable = successState.locationAvailable,
                                onSightClick = {objectId-> onSightClick(objectId, successState.currentTime)},
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    else -> {
                        // The IDE shows an error if an else block is not present, but only on this screen for some reason.
                        // It will build and run fine, but adding this else here to avoid the annoying compile failure warnings.
                        //is SkyTonightUiState.Error -> {
                        ErrorScreen(
                            message = (skyTonightUiState as SkyTonightUiState.Error).message,
                            modifier = contentModifier,
                            onRetryClick = { viewModel.refresh() }
                        )
                    }
                }
                PullRefreshIndicator(
                    isRefreshing,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun SkyTonightBody(
    celestialObjPosList: List<CelestialObjPos>,
    moonIllumination: Int,
    locationAvailable: Boolean,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (celestialObjPosList.isEmpty()) {
        Text(
            text = stringResource(id = if (locationAvailable) R.string.no_item_description else R.string.getting_location),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    } else {
        Text(
            text = "Moon Illumination: $moonIllumination%",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        LazyColumn(modifier = modifier) {
            items(items = celestialObjPosList, key = { it.celestialObjWithImage.celestialObj.id }) { celestialObjPos ->
                CelestialObjItem(
                    celestialObjPos = celestialObjPos,
                    onItemClick = {onSightClick(celestialObjPos.celestialObjWithImage.celestialObj.id)},
                    modifier = Modifier
                        .padding(8.dp)
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun CelestialObjItem(
    celestialObjPos: CelestialObjPos,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prominenceAlpha = if (celestialObjPos.observable) 1f else 0.6f
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = prominenceAlpha)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(prominenceAlpha)
            .clickable(onClick = onItemClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageFile = celestialObjPos.celestialObjWithImage.image?.let { image ->
            File(image.thumbFilename)
        }

        val defaultImagePainter: Painter = painterResource(id = celestialObjPos.celestialObjWithImage.celestialObj.getDefaultImageResource())

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageFile)
                .error(celestialObjPos.celestialObjWithImage.celestialObj.getDefaultImageResource())
                .build(),
            contentDescription = "Celestial Object",
            modifier = Modifier.size(72.dp),
            placeholder = defaultImagePainter,  // Use as a placeholder as well
            onError = {
                // Handle errors if necessary, for instance logging
            }
        )

        val isPlanet = celestialObjPos.celestialObjWithImage.celestialObj.type == ObjectType.PLANET

        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = celestialObjPos.celestialObjWithImage.celestialObj.displayName,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
            if (!isPlanet) {
                Text(
                    text = listOfNotNull(
                        celestialObjPos.celestialObjWithImage.celestialObj.ngcId,
                        celestialObjPos.celestialObjWithImage.celestialObj.objectId.uppercase(Locale.getDefault())
                    ).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = textColor
                )
            }

            Text(
                text = "Alt: ${formatToDegreeAndMinutes(celestialObjPos.alt)}, Azm: ${
                    formatToDegreeAndMinutes(
                        celestialObjPos.azm
                    )
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "Meridian: ${formatHoursToHoursMinutes(celestialObjPos.timeUntilMeridian)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            if (!isPlanet) {
                var tags = celestialObjPos.celestialObjWithImage.celestialObj.field
                if (celestialObjPos.celestialObjWithImage.celestialObj.UHC) tags += "; UHC"
                if (celestialObjPos.celestialObjWithImage.celestialObj.OIII) tags += "; O-III"
                Text(
                    text = "Obs Tags: $tags",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyTonightTopAppBar(
    title: String,
    openDrawer: () -> Unit,
    onFilterSelected: (Boolean) -> Unit,
    currentFilter: Boolean,
    modifier: Modifier = Modifier,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
) {
    var showMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open navigation drawer"
                )
            }
        },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Filter"
                )
            }
            // Dropdown menu for filter options
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Show Recommended") },
                    onClick = {
                        onFilterSelected(true)
                        showMenu = false
                    },
                    leadingIcon = { if (currentFilter) FilledCheckIcon() }
                )
                DropdownMenuItem(
                    text = { Text("Show All") },
                    onClick = {
                        onFilterSelected(false)
                        showMenu = false
                    },
                    leadingIcon = { if (!currentFilter) FilledCheckIcon() }
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

@Composable
fun FilledCheckIcon() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "Selected",
        tint = Color.Green
    )
}
