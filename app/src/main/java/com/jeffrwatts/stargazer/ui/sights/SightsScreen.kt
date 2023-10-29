package com.jeffrwatts.stargazer.ui.sights


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.SkyItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightsScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SightsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val topAppBarState = rememberTopAppBarState()
    val sightsUiState by viewModel.uiState.collectAsState()

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
        when (sightsUiState) {
            is SightsUiState.Loading -> {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }
            is SightsUiState.Success -> {
                SightsBody(
                    celestialObjs = (sightsUiState as SightsUiState.Success).data,
                    onObservationStatusChanged = { item, newStatus ->
                        viewModel.updateObservationStatus(item.celestialObj, newStatus)
                    },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            }
            is SightsUiState.Error -> {
                ErrorScreen(
                    message = (sightsUiState as SightsUiState.Error).message,
                    modifier = Modifier.fillMaxSize(),
                    onRetryClick = { viewModel.fetchObjects() }
                )
            }
        }
    }
}


@Composable
private fun SightsBody(
    celestialObjs: List<CelestialObjPos>,
    onObservationStatusChanged: (CelestialObjPos, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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
                    SkyItem(
                        celestialObjPos = celestialObj,
                        highlight = highlight,
                        onObservationStatusChanged = onObservationStatusChanged,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
