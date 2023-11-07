package com.jeffrwatts.stargazer.ui.sightdetail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.ui.polar.PolarAlignUiState
import com.jeffrwatts.stargazer.ui.polar.PolarAlignViewModel
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightDetailScreen(
    sightId: Int,
    sightName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SightDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sightId) {
        viewModel.fetchSightDetail(sightId)
    }

    Scaffold(
        topBar = {
            SightDetailTopAppBar(
                title = sightName,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        // Your content here
        when (val state = uiState) {
            is SightDetailUiState.Loading -> {
                LoadingScreen(modifier = contentModifier)
            }
            is SightDetailUiState.Success -> {
                val celestialObj = (uiState as SightDetailUiState.Success).data
                Text(text = celestialObj.friendlyName)
            }
            is SightDetailUiState.Error -> {
                ErrorScreen(
                    message = (uiState as SightDetailUiState.Error).message,
                    modifier = contentModifier,
                    onRetryClick = { viewModel.fetchSightDetail(sightId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightDetailTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
) {
    CenterAlignedTopAppBar(
        title = {
            Text(title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = { onNavigateBack() }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}
