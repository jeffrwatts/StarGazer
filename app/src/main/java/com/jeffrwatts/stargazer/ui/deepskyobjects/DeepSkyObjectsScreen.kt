package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.deepskyobjects

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.getImageResource
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.formatHoursToHoursMinutes
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalPermissionsApi::class
)
@Composable
fun DeepSkyObjectsScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeepSkyObjectsViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val deepSkyObjectsUiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val isRefreshing = deepSkyObjectsUiState is DeepSkyObjectsUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.fetchObjects() })

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
    )

    Scaffold(
        topBar = {
            DeepSkyObjectsTopAppBar(
                title = stringResource(R.string.deepskyobjects),
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

        LaunchedEffect(key1 = permissionsState) {
            if (!permissionsState.allPermissionsGranted) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }

        if (permissionsState.allPermissionsGranted) {
            viewModel.startLocationUpdates()
            Box(Modifier.pullRefresh(pullRefreshState)) {
                when (deepSkyObjectsUiState) {
                    is DeepSkyObjectsUiState.Loading -> {
                        LoadingScreen(modifier = contentModifier)
                    }

                    is DeepSkyObjectsUiState.Success -> {
                        DeepSkyObjectsBody(celestialObjs = (deepSkyObjectsUiState as DeepSkyObjectsUiState.Success).data,
                            onSightClick = onSightClick,
                            modifier = contentModifier)
                    }

                    else -> {
                        // The IDE shows an error if an else block is not present, but only on this screen for some reason.
                        // It will build and run fine, but adding this else here to avoid the annoying compile failure warnings.
                        //is DeepSkyObjectsUiState.Error -> {
                        ErrorScreen(
                            message = (deepSkyObjectsUiState as DeepSkyObjectsUiState.Error).message,
                            modifier = contentModifier,
                            onRetryClick = { viewModel.fetchObjects() }
                        )
                    }
                }
                PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
            }
        } else {
            Column(
                modifier = contentModifier.padding(start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Need location permissions to provide the star-gazing experience.")
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
private fun DeepSkyObjectsBody(
    celestialObjs: List<CelestialObjPos>,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (celestialObjs.isEmpty()) {
        Text(
            text = stringResource(R.string.no_item_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    } else {
        LazyColumn(modifier = modifier) {
            items(items = celestialObjs, key = { it.celestialObj.id }) { celestialObj ->
                DeepSkyObjectsItem(
                    celestialObjPos = celestialObj,
                    onItemClick = {onSightClick(celestialObj.celestialObj.id)},
                    modifier = Modifier
                        .padding(8.dp)
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun DeepSkyObjectsItem(
    celestialObjPos: CelestialObjPos,
    onItemClick:() -> Unit,
    modifier: Modifier = Modifier
) {
    val prominenceAlpha = if (celestialObjPos.observable) 1f else 0.6f
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = prominenceAlpha)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(prominenceAlpha)
            .clickable(onClick = onItemClick), // Apply the alpha here
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = celestialObjPos.celestialObj.getImageResource()),
            contentDescription = "Celestial Object",
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = celestialObjPos.celestialObj.friendlyName,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
            Text(
                text = listOfNotNull(
                    celestialObjPos.celestialObj.ngcId,
                    celestialObjPos.celestialObj.objectId.uppercase(Locale.getDefault())
                )
                    .joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
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
            Text(
                text = "Obs Tags: ${celestialObjPos.celestialObj.tags}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSkyObjectsTopAppBar(
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
        tint = Color.Green // Choose an appropriate color for the check icon
    )
}
