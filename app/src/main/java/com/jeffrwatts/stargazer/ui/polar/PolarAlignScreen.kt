package com.jeffrwatts.stargazer.ui.polar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjAltAzm
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.SkyItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolarAlignScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PolarAlignViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val topAppBarState = rememberTopAppBarState()
    val polarAlignUiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.polar_title),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (polarAlignUiState) {
            is PolarAlignUiState.Loading -> {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }
            is PolarAlignUiState.Success -> {
                PolarAlignBody(
                    celestialObjs = (polarAlignUiState as PolarAlignUiState.Success).data,
                    onObservationStatusChanged = { item, newStatus ->
                        viewModel.updateObservationStatus(item.celestialObj, newStatus)
                    },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            }
            is PolarAlignUiState.Error -> {
                ErrorScreen(
                    message = (polarAlignUiState as PolarAlignUiState.Error).message,
                    modifier = Modifier.fillMaxSize(),
                    onRetryClick = { viewModel.fetchObjects() }
                )
            }
        }
    }
}


@Composable
private fun PolarAlignBody(
    celestialObjs: List<CelestialObjAltAzm>,
    onObservationStatusChanged: (CelestialObjAltAzm, ObservationStatus) -> Unit,
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
                    val highlight = celestialObj.polarAlignCandidate
                    SkyItem(celestialObjAltAzm = celestialObj,
                        highlight = highlight,
                        onObservationStatusChanged = onObservationStatusChanged,
                        modifier = Modifier
                            .padding(8.dp))
                }
            }
        }
    }
}
