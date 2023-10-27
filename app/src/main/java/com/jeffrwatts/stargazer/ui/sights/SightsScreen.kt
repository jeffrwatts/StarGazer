package com.jeffrwatts.stargazer.ui.sights

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
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.ui.theme.StarGazerTheme
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
    val sightsUiState by viewModel.sightsUiState.collectAsState()

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
                    enhancedItemList = (sightsUiState as SightsUiState.Success).enhancedItemList,
                    onItemClick = {_, _->},
                    onObservationStatusChanged = { item, newStatus ->
                        viewModel.updateObservationStatus(item, newStatus)
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
                    onRetryClick = { viewModel.refreshItems(forceRefresh = true) }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(
    message: String,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.onError)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryClick) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}
@Composable
private fun SightsBody(
    enhancedItemList: List<EnhancedCelestialObj>,
    onItemClick: (Int, String) -> Unit,
    onObservationStatusChanged: (EnhancedCelestialObj, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (enhancedItemList.isEmpty()) {
            Text(
                text = stringResource(R.string.no_item_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        } else {
            SkyItemList(
                enhancedItemList = enhancedItemList,
                onItemClick = { onItemClick(it.celestialObj.id, it.celestialObj.primaryName) },
                onObservationStatusChanged = onObservationStatusChanged,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun SkyItemList(
    enhancedItemList: List<EnhancedCelestialObj>,
    onItemClick: (EnhancedCelestialObj) -> Unit,
    onObservationStatusChanged: (EnhancedCelestialObj, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items = enhancedItemList, key = { it.celestialObj.id }) { item ->
            SkyItem(item = item,
                onObservationStatusChanged = onObservationStatusChanged,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onItemClick(item) })
        }
    }
}

@Composable
private fun SkyItem(
    item: EnhancedCelestialObj,
    onObservationStatusChanged: (EnhancedCelestialObj, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.celestialObj.primaryName,
                    style = MaterialTheme.typography.bodyLarge
                )
                StarRating(
                    observationStatus = item.celestialObj.observationStatus,
                    onStatusChanged = { newStatus -> onObservationStatusChanged(item, newStatus) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.celestialObj.ngcId ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.celestialObj.type.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Alt: ${formatToDegreeAndMinutes(item.alt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Azm: ${formatToDegreeAndMinutes(item.azm)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun formatToDegreeAndMinutes(angle: Double): String {
    val degrees = angle.toInt()
    val minutes = ((angle - degrees) * 60).roundTo(2)
    return "$degrees° $minutes'"
}

private fun Double.roundTo(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return (this * multiplier).roundToInt() / multiplier
}


fun Double.format(digits: Int) = "%.${digits}f".format(this)


@Preview(showBackground = true)
@Composable
fun HomeBodyPreview() {
    StarGazerTheme {
        SightsBody(listOf(
            EnhancedCelestialObj(
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
            EnhancedCelestialObj(
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
            EnhancedCelestialObj(
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
