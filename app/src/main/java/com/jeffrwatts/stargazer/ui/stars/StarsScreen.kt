package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.stars

import android.Manifest
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjPos
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.PermissionWrapper
import com.jeffrwatts.stargazer.utils.TimeControl
import com.jeffrwatts.stargazer.utils.formatHoursToHoursMinutes
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes


@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class
)
@Composable
fun StarsScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarsViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val starsUiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val isRefreshing = starsUiState is StarsUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.refresh() })

    Scaffold(
        topBar = {
            StarsTopAppBar(
                title = stringResource(R.string.stars),
                openDrawer = openDrawer,
                onFilterSelected = { newFilter ->
                    viewModel.setFilter(newFilter)
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
                when (starsUiState) {
                    is StarsUiState.Loading -> {
                        LoadingScreen(modifier = contentModifier)
                    }

                    is StarsUiState.Success -> {
                        val successState = starsUiState as StarsUiState.Success
                        Column(modifier = contentModifier) {
                            TimeControl(
                                currentTime = successState.currentTime,
                                onIncrementHour = { viewModel.incrementOffset(1) },
                                onDecrementHour = { viewModel.decrementOffset(1) },
                                onIncrementDay = { viewModel.incrementOffset(24) },
                                onDecrementDay = { viewModel.decrementOffset(24) },
                                onResetTime = { viewModel.resetOffset() })
                            StarsBody(
                                starObjPosList = successState.data,
                                locationAvailable = successState.locationAvailable,
                                onSightClick = onSightClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    else -> {
                        // The IDE shows an error if an else block is not present, but only on this screen for some reason.
                        // It will build and run fine, but adding this else here to avoid the annoying compile failure warnings.
                        //is SkyTonightUiState.Error -> {
                        ErrorScreen(
                            message = (starsUiState as StarsUiState.Error).message,
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
private fun StarsBody(
    starObjPosList: List<StarObjPos>,
    locationAvailable: Boolean,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (starObjPosList.isEmpty()) {
        Text(
            text = stringResource(id = if (locationAvailable) R.string.no_item_description else R.string.getting_location),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    } else {
        LazyColumn(modifier = modifier) {
            items(items = starObjPosList, key = { it.starObj.id }) { starObjPos ->
                StarObjItem(
                    starObjPos = starObjPos,
                    onItemClick = {onSightClick(starObjPos.starObj.id)},
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
fun StarObjItem(
    starObjPos: StarObjPos,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prominenceAlpha = if (starObjPos.observable) 1f else 0.6f
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = prominenceAlpha)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(prominenceAlpha)
            .clickable(onClick = onItemClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.star),
            contentDescription = "Star Object",
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = starObjPos.starObj.displayName,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )

            Text(
                text = "Alt: ${formatToDegreeAndMinutes(starObjPos.alt)}, Azm: ${
                    formatToDegreeAndMinutes(
                        starObjPos.azm
                    )
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "Magnitude: ${"%.2f".format(starObjPos.starObj.magnitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "Meridian: ${formatHoursToHoursMinutes(starObjPos.timeUntilMeridian)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarsTopAppBar(
    title: String,
    openDrawer: () -> Unit,
    onFilterSelected: (StarFilter) -> Unit,
    currentFilter: StarFilter,
    modifier: Modifier = Modifier,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
) {
    var showMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
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
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Near Venus") },
                    onClick = {
                        onFilterSelected(StarFilter.NEAR_VENUS)
                        showMenu = false
                    },
                    leadingIcon = { if (currentFilter == StarFilter.NEAR_VENUS) FilledCheckIcon() }
                )
                DropdownMenuItem(
                    text = { Text("Near Mars") },
                    onClick = {
                        onFilterSelected(StarFilter.NEAR_MARS)
                        showMenu = false
                    },
                    leadingIcon = { if (currentFilter == StarFilter.NEAR_MARS) FilledCheckIcon() }
                )
                DropdownMenuItem(
                    text = { Text("Near Jupiter") },
                    onClick = {
                        onFilterSelected(StarFilter.NEAR_JUPITER)
                        showMenu = false
                    },
                    leadingIcon = { if (currentFilter == StarFilter.NEAR_JUPITER) FilledCheckIcon() }
                )
                DropdownMenuItem(
                    text = { Text("Near Saturn") },
                    onClick = {
                        onFilterSelected(StarFilter.NEAR_SATURN)
                        showMenu = false
                    },
                    leadingIcon = { if (currentFilter == StarFilter.NEAR_SATURN) FilledCheckIcon() }
                )
                DropdownMenuItem(
                    text = { Text("Show All") },
                    onClick = {
                        onFilterSelected(StarFilter.ALL)
                        showMenu = false
                    },
                    leadingIcon = { if (currentFilter == StarFilter.ALL) FilledCheckIcon() }
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