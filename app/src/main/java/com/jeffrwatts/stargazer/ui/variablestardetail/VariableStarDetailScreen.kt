package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestardetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.utils.AltitudeChart
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LabeledField
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.utils.formatPeriodToDHH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariableStarDetailScreen(
    variableStarId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariableStarDetailViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(variableStarId) {
        viewModel.fetchVariableStar(variableStarId)
    }

    // Update the title when uiState changes
    var title by remember { mutableStateOf("Loading...") } // default title

    LaunchedEffect(uiState) {
        if (uiState is VariableStarDetailUiState.Success) {
            title = (uiState as VariableStarDetailUiState.Success).data.variableStarObj.friendlyName
        }
    }

    Scaffold(
        topBar = {
            VariableStarTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (uiState) {
            is VariableStarDetailUiState.Loading -> {
                LoadingScreen(modifier = contentModifier)
            }
            is VariableStarDetailUiState.Success -> {
                val variableStarObjPos = (uiState as VariableStarDetailUiState.Success).data
                val altitudes = (uiState as VariableStarDetailUiState.Success).altitudes
                VariableStarDetailContent(variableStarObj = variableStarObjPos.variableStarObj, entries = altitudes, modifier = contentModifier)
            }
            else -> { //is VariableStarDetailUiState.Error -> {
                ErrorScreen(
                    message = (uiState as VariableStarDetailUiState.Error).message,
                    modifier = contentModifier,
                    onRetryClick = { viewModel.fetchVariableStar(variableStarId) }
                )
            }
        }
    }
}

@Composable
fun VariableStarDetailContent(variableStarObj: VariableStarObj, entries: List<Utils.AltitudeEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Banner Image
        Image(
            painter = painterResource(id = R.drawable.star),
            contentDescription = "Banner image for ${variableStarObj.friendlyName}",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Data Fields
        LabeledField(label = "Spectral Type", value = variableStarObj.spectralType)
        LabeledField(label = "Type", value = variableStarObj.type)
        LabeledField(label = "Magnitude High", value = variableStarObj.magnitudeHigh.toString())
        LabeledField(label = "Magnitude Low", value = variableStarObj.magnitudeLow.toString())
        LabeledField(label = "Period", value = formatPeriodToDHH(variableStarObj.period))
        LabeledField(label = "Constellation", value = variableStarObj.constellation)

        Spacer(modifier = Modifier.height(16.dp))
        AltitudeChart(entries)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Observation Notes",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariableStarTopAppBar(
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