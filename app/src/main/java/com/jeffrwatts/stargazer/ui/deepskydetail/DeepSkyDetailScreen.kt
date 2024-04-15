package com.jeffrwatts.stargazer.ui.deepskydetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.getImageResource
import com.jeffrwatts.stargazer.utils.AltitudeChart
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LabeledField
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSkyDetailScreen(
    sightId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeepSkyDetailViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sightId) {
        viewModel.fetchSightDetail(sightId)
    }

    // Update the title when uiState changes
    var title by remember { mutableStateOf("Loading...") } // default title

    LaunchedEffect(uiState) {
        if (uiState is SightDetailUiState.Success) {
            title = (uiState as SightDetailUiState.Success).data.friendlyName
        }
    }

    Scaffold(
        topBar = {
            DeepSkyDetailTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (uiState) {
            is SightDetailUiState.Loading -> {
                LoadingScreen(modifier = contentModifier)
            }
            is SightDetailUiState.Success -> {
                val celestialObj = (uiState as SightDetailUiState.Success).data
                val altitudes = (uiState as SightDetailUiState.Success).altitudes
                DeepSkyDetailContent(celestialObj = celestialObj, entries = altitudes, modifier = contentModifier)
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

@Composable
fun DeepSkyDetailContent(celestialObj: CelestialObj, entries: List<Utils.AltitudeEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Banner Image
        Image(
            painter = painterResource(id = celestialObj.getImageResource()),
            contentDescription = "Banner image for ${celestialObj.friendlyName}",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Data Fields
        LabeledField(label = "NGC ID", value = celestialObj.ngcId ?: "N/A")
        LabeledField(label = "Subtype", value = celestialObj.subType ?: "N/A")
        LabeledField(label = "Constellation", value = celestialObj.constellation ?: "N/A")
        LabeledField(label = "Magnitude", value = celestialObj.magnitude?.toString() ?: "N/A")

        Spacer(modifier = Modifier.height(16.dp))
        AltitudeChart(entries)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Observation Notes",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Text(
            text = celestialObj.observationNotes ?: "No notes available.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSkyDetailTopAppBar(
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
