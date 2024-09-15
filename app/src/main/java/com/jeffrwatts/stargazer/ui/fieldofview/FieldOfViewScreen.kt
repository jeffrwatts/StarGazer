package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview.ScalingOption
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldOfViewScreen(
    sightId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldOfViewViewModel = hiltViewModel()
) {
    // Observe the ViewModel state
    val uiState by viewModel.uiState.collectAsState()

    // State for size, scaling, and rotation
    val size by viewModel.size.collectAsState()
    val scaling by viewModel.scaling.collectAsState()
    val rotation by viewModel.rotation.collectAsState()

    // Intermediate states to avoid immediate update on change
    var sliderSize by remember { mutableStateOf(size) }
    var sliderRotation by remember { mutableStateOf(rotation) }
    var sliderScaling by remember { mutableStateOf(scaling.ordinal.toFloat()) }

    // Update the title when uiState changes
    var title by remember { mutableStateOf("Loading...") }

    LaunchedEffect(sightId) {
        viewModel.initDetail(sightId)
    }

    LaunchedEffect(uiState) {
        if (uiState is FieldOfViewUiState.Success) {
            title = (uiState as FieldOfViewUiState.Success).celestialObjWithImage.celestialObj.displayName
        }
    }

    Scaffold(
        topBar = {
            FieldOfViewTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = modifier
            .padding(innerPadding)
            .fillMaxSize()

        // Determine if controls should be enabled
        val controlsEnabled = uiState !is FieldOfViewUiState.Loading

        Column(
            modifier = contentModifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Display
            if (uiState is FieldOfViewUiState.Success) {
                val success = uiState as FieldOfViewUiState.Success
                val painter = rememberAsyncImagePainter(success.imageData)
                Image(
                    painter = painter,
                    contentDescription = "Sky View Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                )
            } else if (uiState is FieldOfViewUiState.Loading) {
                CircularProgressIndicator()
            } else if (uiState is FieldOfViewUiState.Error) {
                Text(text = (uiState as FieldOfViewUiState.Error).message)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Size Slider
            Text(text = "Size: ${String.format(Locale.US, "%.1f", sliderSize)} degrees")
            Slider(
                value = sliderSize.toFloat(),
                onValueChange = { sliderSize = it.toDouble() },
                onValueChangeFinished = {
                    viewModel.updateSize(sliderSize)
                },
                valueRange = 0.1f..5.0f,
                modifier = Modifier.fillMaxWidth(),
                enabled = controlsEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scaling Slider
            Text(text = "Scaling: ${ScalingOption.entries[sliderScaling.toInt()].name}")
            Slider(
                value = sliderScaling,
                onValueChange = { sliderScaling = it },
                onValueChangeFinished = {
                    viewModel.updateScaling(ScalingOption.entries[sliderScaling.toInt()])
                },
                valueRange = 0f..(ScalingOption.entries.size - 1).toFloat(),
                steps = ScalingOption.entries.size - 2,
                modifier = Modifier.fillMaxWidth(),
                enabled = controlsEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rotation Slider
            Text(text = "Rotation: ${sliderRotation}°")
            Slider(
                value = sliderRotation.toFloat(),
                onValueChange = { sliderRotation = it.toInt() },
                onValueChangeFinished = {
                    val step = 45
                    val roundedValue = (sliderRotation / step) * step
                    viewModel.updateRotation(roundedValue)
                },
                valueRange = 0f..135f,
                steps = 2, // Steps indicate discrete intervals for 0, 45, 90, 135
                modifier = Modifier.fillMaxWidth(),
                enabled = controlsEnabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldOfViewTopAppBar(
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


