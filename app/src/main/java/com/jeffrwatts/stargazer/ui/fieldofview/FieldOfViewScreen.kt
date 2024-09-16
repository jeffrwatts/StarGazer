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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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
    // Define the image size in pixels that we want to request
    val imageSize = 1000 // in pixels, this is the desired width of the image

    // Observe the ViewModel state
    val imageData by viewModel.imageData.collectAsState()

    // Other states
    val selectedTelescope by viewModel.selectedTelescope.collectAsState()
    val selectedCamera by viewModel.selectedCamera.collectAsState()
    val selectedOpticalElement by viewModel.selectedOpticalElement.collectAsState()
    val fieldOfView by viewModel.fieldOfView.collectAsState()
    val size by viewModel.size.collectAsState()
    val telescopes by viewModel.telescopes.collectAsState()
    val cameras by viewModel.cameras.collectAsState()
    val opticalElements by viewModel.opticalElements.collectAsState()
    val scaling by viewModel.scaling.collectAsState()

    var sliderSize by remember { mutableStateOf(size) }
    var imageSizeInLayout by remember { mutableStateOf(IntSize(imageSize, imageSize)) } // Maintain the aspect ratio
    var title by remember { mutableStateOf("Loading...") }

    // Initialize detail and fetch image immediately
    LaunchedEffect(sightId) {
        viewModel.initDetail(sightId, imageSize)
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

        // Check if the lists have been loaded
        if (telescopes.isNotEmpty() && cameras.isNotEmpty() && opticalElements.isNotEmpty()) {
            Column(
                modifier = contentModifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Image Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Keep the image aspect ratio square (1:1)
                ) {
                    when {
                        imageData != null -> {
                            val painter = rememberAsyncImagePainter(imageData)
                            Image(
                                painter = painter,
                                contentDescription = "Sky View Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        imageSizeInLayout = size // Capture the size of the image layout
                                    }
                            )

                            // Draw FOV Box
                            fieldOfView?.let { (widthFov, heightFov) ->
                                if (imageSizeInLayout.width > 0 && imageSizeInLayout.height > 0) {
                                    // Use imageSizeInLayout.width instead of size
                                    val widthProportion = widthFov / size
                                    val heightProportion = heightFov / size
                                    val boxWidth = widthProportion * imageSizeInLayout.width
                                    val boxHeight = heightProportion * imageSizeInLayout.height

                                    Box(
                                        modifier = Modifier
                                            .width(boxWidth.dp)
                                            .height(boxHeight.dp)
                                            .align(Alignment.Center)
                                            .border(2.dp, Color.Red)
                                    )
                                }
                            }
                        }
                        imageData == null -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FOV Display
                fieldOfView?.let { (widthFov, heightFov) ->
                    Text(
                        text = String.format(Locale.US, "%.2f° x %.2f°", widthFov, heightFov),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Telescope Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Telescope:", color = Color.White, modifier = Modifier.width(120.dp))
                    DropdownMenu(
                        options = telescopes.map { it.displayName }, // Use displayName for the dropdown
                        selectedOption = selectedTelescope?.displayName ?: "",
                        onOptionSelected = { selectedName ->
                            val selectedTelescope = telescopes.find { it.displayName == selectedName }
                            selectedTelescope?.let {
                                viewModel.updateSelectedTelescope(it)
                                viewModel.refreshImage(imageSize)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Camera Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Camera:", color = Color.White, modifier = Modifier.width(120.dp))
                    DropdownMenu(
                        options = cameras.map { it.displayName }, // Use displayName for the dropdown
                        selectedOption = selectedCamera?.displayName ?: "",
                        onOptionSelected = { selectedName ->
                            val selectedCamera = cameras.find { it.displayName == selectedName }
                            selectedCamera?.let {
                                viewModel.updateSelectedCamera(it)
                                viewModel.refreshImage(imageSize)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Optical Elements Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Optical Element:", color = Color.White, modifier = Modifier.width(120.dp))
                    DropdownMenu(
                        options = opticalElements.map { it.displayName }, // Use displayName for the dropdown
                        selectedOption = selectedOpticalElement?.displayName ?: "",
                        onOptionSelected = { selectedName ->
                            val selectedOpticalElement = opticalElements.find { it.displayName == selectedName }
                            selectedOpticalElement?.let {
                                viewModel.updateSelectedOpticalElement(it)
                                viewModel.refreshImage(imageSize)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scaling Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Scaling:", color = Color.White, modifier = Modifier.width(120.dp))
                    DropdownMenu(
                        options = ScalingOption.values().map { it.name }, // Use name for the dropdown
                        selectedOption = scaling.name,
                        onOptionSelected = { selectedName ->
                            val selectedScaling = ScalingOption.valueOf(selectedName)
                            viewModel.updateScaling(selectedScaling, imageSize)
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
                        viewModel.updateSize(sliderSize, imageSize)
                    },
                    valueRange = 0.1f..5.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Show a loading indicator while data is being fetched
            Box(
                modifier = contentModifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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


