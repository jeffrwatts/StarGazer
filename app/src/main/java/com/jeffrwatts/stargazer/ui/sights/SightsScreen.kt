package com.jeffrwatts.stargazer.ui.sights


import androidx.compose.foundation.Image
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.StarRating
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SightsScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SightsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val topAppBarState = rememberTopAppBarState()
    val sightsUiState by viewModel.uiState.collectAsState()
    val isRefreshing = sightsUiState is SightsUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.fetchObjects() })

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.sights_title),
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
            when (sightsUiState) {
                is SightsUiState.Loading -> {
                    LoadingScreen(modifier = contentModifier)
                }
                is SightsUiState.Success -> {
                    SightsBody(
                        celestialObjs = (sightsUiState as SightsUiState.Success).data,
                        onObservationStatusChanged = { item, newStatus ->
                            viewModel.updateObservationStatus(item.celestialObj, newStatus)
                        },
                        modifier = contentModifier)
                }
                is SightsUiState.Error -> {
                    ErrorScreen(
                        message = (sightsUiState as SightsUiState.Error).message,
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
private fun SightsBody(
    celestialObjs: List<CelestialObjPos>,
    onObservationStatusChanged: (CelestialObjPos, ObservationStatus) -> Unit,
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
                val highlight = celestialObj.alt > 10
                SightItem(
                    celestialObjPos = celestialObj,
                    onObservationStatusChanged = onObservationStatusChanged,
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
fun SightItem(
    celestialObjPos: CelestialObjPos,
    onObservationStatusChanged: (CelestialObjPos, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val prominenceAlpha = if (celestialObjPos.alt >= 20) 1f else 0.6f  // More prominent if alt >= 20
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = prominenceAlpha)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(prominenceAlpha), // Apply the alpha here
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder image, replace with actual resource
        Image(
            painter = painterResource(id = R.drawable.star), // replace with actual drawable resource
            contentDescription = "Celestial Object",
            modifier = Modifier.size(48.dp)
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
                text = listOfNotNull(celestialObjPos.celestialObj.ngcId, celestialObjPos.celestialObj.catalogId)
                    .joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
            celestialObjPos.celestialObj.desc?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    color = textColor
                )
            }
            Text(
                text = "Alt: ${formatToDegreeAndMinutes(celestialObjPos.alt)}, Azm: ${formatToDegreeAndMinutes(celestialObjPos.azm)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            StarRating(
                observationStatus = celestialObjPos.celestialObj.observationStatus,
                onStatusChanged = { newStatus -> onObservationStatusChanged(celestialObjPos, newStatus) }
            )
        }
    }
}