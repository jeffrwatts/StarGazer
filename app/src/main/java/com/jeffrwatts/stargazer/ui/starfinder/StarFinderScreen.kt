package com.jeffrwatts.stargazer.ui.starfinder

import android.graphics.Paint
import android.hardware.SensorManager
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFinderScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarFinderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val foundObjects by viewModel.foundObjects.collectAsState()
    val searchCompleted by viewModel.searchCompleted.collectAsState()
    val topAppBarState = rememberTopAppBarState()

    val accuracyDescription = when (uiState.accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
        SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
        else -> "Unknown"
    }

    DisposableEffect(Unit) {
        viewModel.startContinuousSearching()
        onDispose {
            viewModel.stopContinuousSearching()
        }
    }

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.star_finder),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "True Azimuth: ${uiState.trueAzimuth.roundToInt()}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Altitude: ${uiState.altitude.roundToInt()}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Declination: ${String.format("%.1f", uiState.magDeclination)}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Accuracy: $accuracyDescription", style = MaterialTheme.typography.bodyLarge)

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CameraPreview(Modifier.fillMaxSize())

                StarFinderOverlay(
                    azimuth = uiState.trueAzimuth.roundToInt(),
                    altitude = uiState.altitude.roundToInt(),
                    Modifier.align(Alignment.Center))

                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color(0xFF330000).copy(alpha = 0.6f)) // Dark red, semi-transparent
                ) {
                    items(foundObjects.size) { index ->
                        val celestialObject = foundObjects[index]
                        Text(
                            text = celestialObject.friendlyName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController = remember { LifecycleCameraController(context) }

    AndroidView(
        modifier = modifier, // Use the passed modifier for sizing
        factory = {
            PreviewView(context).apply {
                setBackgroundColor(Color.Black.toArgb())
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FILL_START
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }.also { previewView ->
                previewView.controller = cameraController
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        },
        onRelease = {
            cameraController.unbind()
        }
    )
}

@Composable
fun StarFinderOverlay(azimuth: Int, altitude: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = calculate5DegreeRadius(size, azimuth, altitude) // You'll need to implement this

        drawCircle(
            color = Color.White.copy(alpha = 0.3f), // Faint white circle
            radius = radius,
            center = center,
            style = Stroke(width = 2f) // Thin stroke for the circle
        )
    }
}

/**
 * Calculates the radius for a 5-degree field of view circle.
 * This might need adjustment based on actual camera field of view and screen size.
 */
fun calculate5DegreeRadius(size: Size, azimuth: Int, altitude: Int): Float {
    // Placeholder calculation. Adjust based on actual field of view.
    // The ratio here (e.g., 10f) will depend on how much of the screen's width/height
    // should be covered by the 5-degree field of view.
    val screenDiagonal = sqrt(size.width.pow(2) + size.height.pow(2))
    return screenDiagonal / 10f // Example calculation
}

@Composable
fun StarFinderOverlayRuler(azimuth: Int, altitude: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Align AzimuthRulerOverlay horizontally within the Box
        AzimuthRulerOverlay(azimuth, modifier.align(Alignment.Center))

        // Align AltitudeRulerOverlay vertically within the Box
        AltitudeRulerOverlay(altitude, modifier.align(Alignment.Center))

        // Add ReferenceLines
        ReferenceLines(modifier = Modifier.align(Alignment.Center))
    }
}


@Composable
fun AzimuthRulerOverlay(azimuth: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(100.dp)) {
        val canvasWidth = size.width
        val tickSpacing = 20f
        val center = canvasWidth / 2

        val startDegree = azimuth - 30
        val endDegree = azimuth + 30

        for (i in startDegree..endDegree) {
            val xPosition = center + (i - azimuth) * tickSpacing

            val tickHeight = if (i % 10 == 0) 60f else 30f
            val strokeWidth = if (i % 10 == 0) 4f else 2f

            drawLine(
                color = Color.White,
                start = Offset(xPosition, size.height - tickHeight),
                end = Offset(xPosition, size.height),
                strokeWidth = strokeWidth
            )

            if (i % 10 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "$i°",
                    xPosition,
                    size.height - 70f,
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 40f
                        textAlign = Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun AltitudeRulerOverlay(altitude: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(100.dp)) {
        val canvasHeight = size.height
        val tickSpacing = 20f
        val center = canvasHeight / 2

        val startDegree = altitude - 45
        val endDegree = altitude + 45

        for (i in startDegree..endDegree) {
            val yPosition = center + (i - altitude) * tickSpacing

            val tickWidth = if (i % 15 == 0) 60f else 30f
            val strokeWidth = if (i % 15 == 0) 4f else 2f

            drawLine(
                color = Color.White,
                start = Offset(size.width - tickWidth, yPosition),
                end = Offset(size.width, yPosition),
                strokeWidth = strokeWidth
            )

            if (i % 15 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "$i°",
                    size.width - 70f,
                    yPosition,
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 40f
                        textAlign = Paint.Align.RIGHT
                    }
                )
            }
        }
    }
}

@Composable
fun ReferenceLines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeWidth = 4f

        // Vertical line
        drawLine(
            color = Color.Red,
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = strokeWidth
        )

        // Horizontal line
        drawLine(
            color = Color.Red,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun ShowResultsDialog(celestialObjects: List<CelestialObj>, noObjectsFound: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (noObjectsFound) "Search Results" else "Found Celestial Objects") },
        text = {
            if (noObjectsFound) {
                Text("No celestial objects found at the selected location.")
            } else {
                Column {
                    celestialObjects.forEach { obj ->
                        Text(obj.friendlyName)
                        // Add more details as needed
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}



