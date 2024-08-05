package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.TimeControl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun VariableStarPlannerScreen(
    openDrawer: () -> Unit,
    onSightClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariableStarPlannerViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val variableStarPlannerUiState by viewModel.uiState.collectAsState()
    val isRefreshing = variableStarPlannerUiState is VariableStarPlannerUiState.Loading
    val pullRefreshState = rememberPullRefreshState(isRefreshing, {  })

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
            when (variableStarPlannerUiState) {
                is VariableStarPlannerUiState.Loading -> {
                    LoadingScreen(modifier = contentModifier)
                }

                is VariableStarPlannerUiState.Success -> {
                    val uiState = variableStarPlannerUiState as VariableStarPlannerUiState.Success
                    Column(modifier = Modifier.padding(innerPadding).fillMaxWidth()) {
                        TimeControl(
                            currentTime = uiState.currentTime,
                            onIncrementTime = { viewModel.incrementOffset() },
                            onDecrementTime = { viewModel.decrementOffset() },
                            onResetTime = { viewModel.resetOffset() }
                        )
                        Text(
                            "Night Start: ${uiState.nightStart}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            "Night End: ${uiState.nightEnd}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            "Is Night: ${uiState.isNight}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    // The IDE shows an error if an else block is not present, but only on this screen for some reason.
                    // It will build and run fine, but adding this else here to avoid the annoying compile failure warnings.
                    //is RecommendedUiState.Error -> {
                    ErrorScreen(
                        message = (variableStarPlannerUiState as VariableStarPlannerUiState.Error).message,
                        modifier = contentModifier,
                        onRetryClick = { viewModel.refresh() }
                    )
                }
            }
            PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}
