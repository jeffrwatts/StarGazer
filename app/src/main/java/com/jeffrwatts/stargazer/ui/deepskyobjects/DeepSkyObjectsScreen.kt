package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.deepskyobjects

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
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.getImageResource
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DeepSkyObjectsScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeepSkyObjectsViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val deepSkyObjectsUiState by viewModel.uiState.collectAsState()
    val isRefreshing = deepSkyObjectsUiState is DeepSkyObjectsUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.fetchObjects() })

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.deepskyobjects),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

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
                    celestialObjPos.celestialObj.catalogId
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
                text = "Obs Tags: ${celestialObjPos.celestialObj.tags}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
