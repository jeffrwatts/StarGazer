package com.jeffrwatts.stargazer.ui.solarsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.solarsystem.PlanetObjPos
import com.jeffrwatts.stargazer.data.solarsystem.getImageResource
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.ui.deepskyobjects.TimeControl
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.formatHoursToHoursMinutes
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SolarSystemScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SolarSystemViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val solarSystemUiState by viewModel.uiState.collectAsState()
    val isRefreshing = solarSystemUiState is SolarSystemUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.fetchObjects() })

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.solarsystem),
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
            when (solarSystemUiState) {
                is SolarSystemUiState.Loading -> {
                    LoadingScreen(modifier = contentModifier)
                }

                is SolarSystemUiState.Success -> {
                    val successState = solarSystemUiState as SolarSystemUiState.Success
                    Column(modifier = contentModifier) {
                        TimeControl(
                            currentTime = successState.currentTime,
                            onIncrementTime = { viewModel.incrementTime() },
                            onDecrementTime = { viewModel.decrementTime() },
                            onResetTime = { viewModel.resetTime() }
                        )
                        SolarSystemBody(
                            planetObjs = successState.data,
                            successState.expirationDate,
                            onSightClick = onSightClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                else -> {
                    // The IDE shows an error if an else block is not present, but only on this screen for some reason.
                    // It will build and run fine, but adding this else here to avoid the annoying compile failure warnings.
                    //is SolarSystemUiState.Error -> {
                    ErrorScreen(
                        message = (solarSystemUiState as SolarSystemUiState.Error).message,
                        modifier = contentModifier,
                        onRetryClick = { viewModel.fetchObjects() }
                    )
                }
            }
            PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SolarSystemBody(
    planetObjs: List<PlanetObjPos>,
    expirationDate: String?,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (planetObjs.isEmpty()) {
        Text(
            text = stringResource(R.string.no_item_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    } else {
        LazyColumn(modifier = modifier) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ephemeris_expires_on, expirationDate ?: "N/A"),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }
            items(items = planetObjs, key = { it.planetObj.id }) { planetObjPos ->
                SolarSystemItem(
                    planetObjPos = planetObjPos,
                    onItemClick = {onSightClick(planetObjPos.planetObj.id)},
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
fun SolarSystemItem(
    planetObjPos: PlanetObjPos,
    onItemClick:() -> Unit,
    modifier: Modifier = Modifier
) {
    val prominenceAlpha = if (planetObjPos.observable) 1f else 0.6f
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
            painter = painterResource(id = planetObjPos.planetObj.getImageResource()),
            contentDescription = "Celestial Object",
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = planetObjPos.planetObj.planetName,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
            Text(
                text = "Alt: ${formatToDegreeAndMinutes(planetObjPos.alt)}, Azm: ${formatToDegreeAndMinutes(planetObjPos.azm)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "Meridian: ${formatHoursToHoursMinutes(planetObjPos.timeUntilMeridian)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
