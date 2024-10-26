package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.jupiterdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.celestialobjdetail.CelestialObjDetailTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import io.github.cosinekitty.astronomy.Equatorial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JupiterDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JupiterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CelestialObjDetailTopAppBar(
                title = stringResource(R.string.jupiter_details),
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (uiState) {
            is JupiterDetailUIState.Loading -> { LoadingScreen(modifier = contentModifier) }
            is JupiterDetailUIState.Success -> { JupiterCelestialGrid(uiState = uiState as JupiterDetailUIState.Success, modifier)}
            else /*JupiterDetailUIState.Error*/ -> { ErrorScreen((uiState as JupiterDetailUIState.Error).message, modifier = contentModifier, {}) }
            }
        }
}
@Composable
fun JupiterCelestialGrid(
    uiState: JupiterDetailUIState.Success,
    modifier: Modifier = Modifier,
    maxDecHeight: Float = 200f // Define a maximum height for Dec scaling
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2

            // Calculate screen positions for each celestial object based on RA and Dec
            val jupiterPos = calculatePosition(uiState.jupiterPos, uiState, width, centerY, maxDecHeight)
            val ioPos = calculatePosition(uiState.ioPos, uiState, width, centerY, maxDecHeight)
            val europaPos = calculatePosition(uiState.europaPos, uiState, width, centerY, maxDecHeight)
            val ganymedePos = calculatePosition(uiState.ganymedePos, uiState, width, centerY, maxDecHeight)
            val callistoPos = calculatePosition(uiState.callistoPos, uiState, width, centerY, maxDecHeight)

            // Draw RA/Dec Axis (horizontal line for RA)
            drawLine(
                color = Color.Gray,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 4f
            )

            // Plot Jupiter and moons along the RA/Dec axis with shorthand labels and color coding
            drawCelestialBody(jupiterPos, "J", Color.Yellow)
            drawCelestialBody(ioPos, "I", Color.Red)
            drawCelestialBody(europaPos, "E", Color.Cyan)
            drawCelestialBody(ganymedePos, "G", Color.Green)
            drawCelestialBody(callistoPos, "C", Color.Magenta)
        }
    }
}

// Helper to convert RA/Dec to screen positions with independent scales for RA and Dec
private fun calculatePosition(
    pos: Equatorial,
    uiState: JupiterDetailUIState.Success,
    width: Float,
    centerY: Float,
    maxDecHeight: Float
): Offset {
    // Calculate x position based on RA spanning full width
    val maxRa = maxOf(uiState.jupiterPos.ra, uiState.ioPos.ra, uiState.europaPos.ra, uiState.ganymedePos.ra, uiState.callistoPos.ra)
    val minRa = minOf(uiState.jupiterPos.ra, uiState.ioPos.ra, uiState.europaPos.ra, uiState.ganymedePos.ra, uiState.callistoPos.ra)
    val x = ((maxRa - pos.ra) / (maxRa - minRa) * width).toFloat()

    // Calculate y position based on scaled Dec, with Jupiter at centerY
    val maxDec = maxOf(uiState.jupiterPos.dec, uiState.ioPos.dec, uiState.europaPos.dec, uiState.ganymedePos.dec, uiState.callistoPos.dec)
    val minDec = minOf(uiState.jupiterPos.dec, uiState.ioPos.dec, uiState.europaPos.dec, uiState.ganymedePos.dec, uiState.callistoPos.dec)
    val y = centerY - ((pos.dec - minDec) / (maxDec - minDec) * maxDecHeight - maxDecHeight / 2)

    return Offset(x, y.toFloat())
}

// Helper to draw a celestial body label with color coding
private fun DrawScope.drawCelestialBody(position: Offset, label: String, color: Color) {
    drawCircle(color = color, radius = 8f, center = position)
    drawContext.canvas.nativeCanvas.drawText(label, position.x, position.y - 20f, android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = 40f // Increased text size for readability
        isFakeBoldText = true // Adds bold effect for better visibility
    })
}
