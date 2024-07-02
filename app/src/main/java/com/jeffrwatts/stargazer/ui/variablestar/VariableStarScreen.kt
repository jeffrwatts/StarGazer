package com.jeffrwatts.stargazer.ui.variablestar

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
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.ui.deepskyobjects.TimeControl
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.formatHoursToHoursMinutes
import com.jeffrwatts.stargazer.utils.formatPeriodToDHH
import com.jeffrwatts.stargazer.utils.formatToDegreeAndMinutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun VariableStarScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariableStarViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val variableStarUiState by viewModel.uiState.collectAsState()
    val isRefreshing = variableStarUiState is VariableStarUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.fetchObjects() })

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.variable_star_title),
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
            when (variableStarUiState) {
                is VariableStarUiState.Loading -> {
                    LoadingScreen(modifier = contentModifier)
                }

                is VariableStarUiState.Success -> {
                    val successState = variableStarUiState as VariableStarUiState.Success
                    Column(modifier = contentModifier) {
                        TimeControl(
                            currentTime = successState.currentTime,
                            onIncrementTime = { viewModel.incrementTime() },
                            onDecrementTime = { viewModel.decrementTime() },
                            onResetTime = { viewModel.resetTime() }
                        )
                        VariableStarBody(
                            variableStarObjs = successState.data,
                            onSightClick = onSightClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                else -> {
                    // The IDE shows an error if an else block is not present, but only on this screen for some reason.
                    // It will build and run fine, but adding this else here to avoid the annoying compile failure warnings.
                    //is RecommendedUiState.Error -> {
                    ErrorScreen(
                        message = (variableStarUiState as VariableStarUiState.Error).message,
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
private fun VariableStarBody(
    variableStarObjs: List<VariableStarObjPos>,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (variableStarObjs.isEmpty()) {
        Text(
            text = stringResource(R.string.no_item_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    } else {
        LazyColumn(modifier = modifier) {
            items(items = variableStarObjs, key = { it.variableStarObj.id }) { variableStarObj ->
                VariableStarItem(variableStarObjPos = variableStarObj,
                    onItemClick = {onSightClick(variableStarObj.variableStarObj.id)},
                    modifier = Modifier
                        .padding(8.dp))
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun VariableStarItem(
    variableStarObjPos: VariableStarObjPos,
    onItemClick:() -> Unit,
    modifier: Modifier = Modifier
) {
    val prominenceAlpha = if (variableStarObjPos.observable) 1f else 0.6f
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
            contentDescription = "Variable Star Object",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = variableStarObjPos.variableStarObj.displayName,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
            Text(
                text = "Dec: ${formatToDegreeAndMinutes(variableStarObjPos.variableStarObj.dec)}, RA: ${formatToDegreeAndMinutes(variableStarObjPos.variableStarObj.ra)}",
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = textColor
            )
            Text(
                text = "Period: ${formatPeriodToDHH(variableStarObjPos.variableStarObj.period)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "Alt: ${formatToDegreeAndMinutes(variableStarObjPos.alt)}, Azm: ${formatToDegreeAndMinutes(variableStarObjPos.azm)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "Meridian: ${formatHoursToHoursMinutes(variableStarObjPos.timeUntilMeridian)}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

