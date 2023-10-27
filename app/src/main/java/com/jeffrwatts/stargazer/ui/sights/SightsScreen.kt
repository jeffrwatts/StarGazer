package com.jeffrwatts.stargazer.ui.sights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjAltAzm
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.ui.theme.StarGazerTheme
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.SkyItem
import com.jeffrwatts.stargazer.utils.SkyItemList
import com.jeffrwatts.stargazer.utils.StarRating
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightsScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SightsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
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
                    onItemClick = {_, _->},
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
    celestialObjs: List<CelestialObjAltAzm>,
    onItemClick: (Int, String) -> Unit,
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
            SkyItemList(
                celestialObjs = celestialObjs,
                onItemClick = { onItemClick(it.celestialObj.id, it.celestialObj.primaryName) },
                onObservationStatusChanged = onObservationStatusChanged,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeBodyPreview() {
    StarGazerTheme {
        SightsBody(listOf(
            CelestialObjAltAzm(
                celestialObj = CelestialObj(
                    id = 1,
                    primaryName = "M31",
                    ngcId = "NGC 224, Andromeda Galaxy",
                    ra = 10.6847083,
                    dec = 41.26904,
                    mag = 3.44,
                    type = ObjectType.GALAXY,
                    constellation = "Andromeda",
                    observationStatus = ObservationStatus.NOT_OBSERVED
                ),
                alt = 45.6,
                azm = 60.4
            ),
            CelestialObjAltAzm(
                celestialObj = CelestialObj(
                    id = 2,
                    primaryName = "M42",
                    ngcId = "NGC 1976, Orion Nebula",
                    ra = 5.595,
                    dec = -5.394,
                    mag = 4.0,
                    type = ObjectType.NEBULA,
                    constellation = "Orion",
                    observationStatus = ObservationStatus.NOT_OBSERVED
                ),
                alt = 30.2,
                azm = 120.5
            )
        ), onItemClick = {_, _ ->}, onObservationStatusChanged={ _, _ ->  })
    }
}

@Preview(showBackground = true)
@Composable
fun HomeBodyEmptyListPreview() {
    StarGazerTheme {
        SightsBody(listOf(), onItemClick = {_, _ ->}, onObservationStatusChanged={ _, _ ->  })
    }
}

@Preview(showBackground = true)
@Composable
fun SkyItemPreview() {
    StarGazerTheme {
        SkyItem(
            CelestialObjAltAzm(
                celestialObj = CelestialObj(
                    id = 1,
                    primaryName = "M31",
                    ngcId = "NGC 224, Andromeda Galaxy",
                    ra = 10.6847083,
                    dec = 41.26904,
                    mag = 3.44,
                    type = ObjectType.GALAXY,
                    constellation = "Andromeda",
                    observationStatus = ObservationStatus.NOT_OBSERVED
                ),
                alt = 45.6,
                azm = 60.4
            ), onObservationStatusChanged={ _, _ ->  }
        )
    }
}
