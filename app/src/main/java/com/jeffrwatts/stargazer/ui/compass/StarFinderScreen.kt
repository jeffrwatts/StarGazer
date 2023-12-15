package com.jeffrwatts.stargazer.ui.compass

import android.graphics.Paint
import android.hardware.SensorManager
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFinderScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarFinderViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val topAppBarState = rememberTopAppBarState()

    val accuracyDescription = when (uiState.orientationData.accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
        SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
        else -> "Unknown"
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
            val azimuth = uiState.orientationData.azimuth.roundToInt()
            val altitude = uiState.orientationData.altitude.roundToInt()

            Text(text = "Azimuth: ${azimuth}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Altitude: ${altitude}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Declination: ${String.format("%.1f", uiState.magDeclination)}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Accuracy: $accuracyDescription", style = MaterialTheme.typography.bodyLarge)

            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                CameraPreview({
                    viewModel.findObjects(
                        uiState.orientationData.altitude,
                        uiState.orientationData.azimuth)},
                    modifier = Modifier.fillMaxWidth())

                StarFinderOverlay(azimuth = azimuth, altitude = altitude,
                    magDeclination = uiState.magDeclination, Modifier.align(Alignment.Center))
            }

        }
    }
}

@Composable
fun CameraPreview(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                setOnClickListener { onClick() }
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
fun StarFinderOverlay(azimuth: Int, altitude: Int, magDeclination: Float, modifier: Modifier = Modifier) {
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
    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
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

