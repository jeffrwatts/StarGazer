package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Camera
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.OpticalElement
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Telescope
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

    // State for equipment lists
    val telescopes by viewModel.telescopes.collectAsState()
    val cameras by viewModel.cameras.collectAsState()
    val opticalElements by viewModel.opticalElements.collectAsState()

    // State for selected telescope, camera, optical element, and scaling
    var selectedTelescope by remember { mutableStateOf(viewModel.selectedTelescope ?: telescopes.firstOrNull()) }
    var selectedCamera by remember { mutableStateOf(viewModel.selectedCamera ?: cameras.firstOrNull()) }
    var selectedOpticalElement by remember { mutableStateOf(viewModel.selectedOpticalElement ?: opticalElements.firstOrNull()) }
    var selectedScaling by remember { mutableStateOf(viewModel.scaling.value) }

    // State for size
    val size by viewModel.size.collectAsState()

    // Intermediate state to avoid immediate update on size change
    var sliderSize by remember { mutableStateOf(size) }

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
            modifier = contentModifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(), // Ensure full width
            horizontalAlignment = Alignment.Start // Align children to the start
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
                        .height(400.dp) // Adjust height if necessary
                )
            } else if (uiState is FieldOfViewUiState.Loading) {
                CircularProgressIndicator()
            } else if (uiState is FieldOfViewUiState.Error) {
                Text(text = (uiState as FieldOfViewUiState.Error).message)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telescope Row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Telescope:", color = Color.White, modifier = Modifier.width(120.dp))
                DropdownMenu(
                    options = telescopes,
                    selectedOption = selectedTelescope,
                    onOptionSelected = {
                        selectedTelescope = it
                        viewModel.selectedTelescope = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera Row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Camera:", color = Color.White, modifier = Modifier.width(120.dp))
                DropdownMenu(
                    options = cameras,
                    selectedOption = selectedCamera,
                    onOptionSelected = {
                        selectedCamera = it
                        viewModel.selectedCamera = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Optical Elements Row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Optical Element:", color = Color.White, modifier = Modifier.width(120.dp))
                DropdownMenu(
                    options = opticalElements,
                    selectedOption = selectedOpticalElement,
                    onOptionSelected = {
                        selectedOpticalElement = it
                        viewModel.selectedOpticalElement = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scaling Row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Scaling:", color = Color.White, modifier = Modifier.width(120.dp))
                DropdownMenu(
                    options = ScalingOption.values().toList(),
                    selectedOption = selectedScaling,
                    onOptionSelected = {
                        selectedScaling = it
                        viewModel.updateScaling(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Size Slider
            Text(text = "Size: ${String.format(Locale.US, "%.1f", sliderSize)} degrees", color = Color.White)
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
        }
    }
}

@Composable
fun <T> DropdownMenu(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(selectedOption?.toString() ?: "") }

    Box(
        modifier = Modifier
            .border(1.dp, Color.White, shape = RoundedCornerShape(4.dp)) // Add border
            .background(Color.DarkGray) // Set background color
            .clickable { expanded = true } // Make it clickable
            .padding(8.dp) // Add padding
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedText,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Icon",
                tint = Color.White
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        // Display the displayName of the object
                        Text(
                            text = when (option) {
                                is Camera -> option.displayName
                                is OpticalElement -> option.displayName
                                is Telescope -> option.displayName
                                is ScalingOption -> option.name
                                else -> option.toString()
                            },
                            color = Color.White
                        )
                    },
                    onClick = {
                        selectedText = when (option) {
                            is Camera -> option.displayName
                            is OpticalElement -> option.displayName
                            is Telescope -> option.displayName
                            is ScalingOption -> option.name
                            else -> option.toString()
                        }
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
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
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Back"
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}


