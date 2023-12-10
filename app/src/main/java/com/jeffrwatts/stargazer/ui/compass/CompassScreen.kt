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
fun CompassScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompassViewModel = hiltViewModel()
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
                title = stringResource(R.string.compass_title),
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
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                // CameraPreview with weight modifier
                CameraPreview(modifier = Modifier.fillMaxWidth())

                // CompassOverlay on top of CameraPreview
                CompassRulerOverlay(azimuth = azimuth, magDeclination = uiState.magDeclination)
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
fun CompassRulerOverlay(azimuth: Int, magDeclination: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val canvasWidth = size.width
        val tickSpacing = 20f
        val center = canvasWidth / 2

        // Adjust heading for true north
        val trueNorthHeading = (azimuth + magDeclination).roundToInt()

        val startDegree = trueNorthHeading - 30
        val endDegree = trueNorthHeading + 30

        for (i in startDegree..endDegree) {
            val xPosition = center + (i - trueNorthHeading) * tickSpacing

            val isZeroDegree = i % 360 == 0 // Check if the degree is zero
            val tickHeight = if (i % 10 == 0 || isZeroDegree) 90f else 45f
            val tickColor = if (isZeroDegree) Color.Red else Color.Blue
            val strokeWidth = if (isZeroDegree) 8f else if (i % 10 == 0) 4f else 2f

            // Extend the 0-degree line full height if it's the zero degree
            val startY = if (isZeroDegree) 0f else size.height - tickHeight

            drawLine(
                color = tickColor,
                start = Offset(xPosition, startY),
                end = Offset(xPosition, size.height),
                strokeWidth = strokeWidth
            )

            // Draw degree number for long ticks, skip for 0 as it will be obvious
            if (i % 10 == 0 && !isZeroDegree) {
                drawContext.canvas.nativeCanvas.drawText(
                    "$i°",
                    xPosition,
                    size.height - 100f,
                    Paint().apply {
                        color = android.graphics.Color.BLUE
                        textSize = 60f
                        textAlign = Paint.Align.CENTER
                    }
                )
            }
        }
    }
}
