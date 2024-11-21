package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.jupiterdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.jeffrwatts.stargazer.utils.AppConstants
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LabeledField
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.TimeControl
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.decimalDecToDmsString
import com.jeffrwatts.stargazer.utils.decimalRaToHmsString
import io.github.cosinekitty.astronomy.Equatorial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JupiterDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JupiterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }

    // Button action that starts/stops the time offset updates
    fun togglePlayStop() {
        isPlaying = !isPlaying
        if (isPlaying) {
            viewModel.startIncrementingOffset()
        } else {
            viewModel.stopIncrementingOffset()
        }
    }

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
            is JupiterDetailUIState.Success -> {
                val successState = uiState as JupiterDetailUIState.Success
                Column(modifier = contentModifier) {
                    TimeControl(
                        currentTime = successState.time.format(AppConstants.DATE_TIME_FORMATTER),
                        onIncrementHour = { viewModel.incrementOffset(1) },
                        onDecrementHour = { viewModel.decrementOffset(1) },
                        onIncrementDay = { viewModel.incrementOffset(24) },
                        onDecrementDay = { viewModel.decrementOffset(24) },
                        onResetTime = { viewModel.resetOffset() })

                    LabeledField(label = "Dist:", value = "${successState.jupiterPos.dist} AU")
                    LabeledField(label = "Angular Diameter:", value = decimalDecToDmsString(successState.jupiterAngularRadius*2))

                    // Play/Stop Button
                    Button(
                        onClick = { togglePlayStop() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = if (isPlaying) "Stop" else "Play",
                            color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp) // Set height for the grid area
                            .padding(vertical = 32.dp) // Added vertical padding
                    ) {
                        JupiterCelestialGrid(uiState = successState)
                    }
                    JovianMoonEventList(events = successState.jovianMoonEvents)

                }
            }
            else /*JupiterDetailUIState.Error*/ -> { ErrorScreen((uiState as JupiterDetailUIState.Error).message, modifier = contentModifier, {}) }
            }
        }
}

@Composable
fun JupiterCelestialGrid(
    uiState: JupiterDetailUIState.Success,
    modifier: Modifier = Modifier,
) {
    // Define the fixed ranges in degrees for RA and Dec around Jupiter
    val raRange = 0.025 // ±6 arc minutes in degrees
    val decRange = 0.02 // ±3 arc minutes in degrees

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            // Centered position for Jupiter
            val jupiterPos = Offset(centerX, centerY)

            // Calculate screen positions for each celestial object based on the fixed RA/Dec range
            val ioPos = calculateFixedPosition(uiState.ioPos, uiState.jupiterPos, width, height, raRange, decRange, centerX, centerY)
            val europaPos = calculateFixedPosition(uiState.europaPos, uiState.jupiterPos, width, height, raRange, decRange, centerX, centerY)
            val ganymedePos = calculateFixedPosition(uiState.ganymedePos, uiState.jupiterPos, width, height, raRange, decRange, centerX, centerY)
            val callistoPos = calculateFixedPosition(uiState.callistoPos, uiState.jupiterPos, width, height, raRange, decRange, centerX, centerY)

            // Draw RA/Dec Axis (horizontal line for RA at center Y)
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

// Helper to calculate screen positions using fixed RA/Dec ranges with Jupiter as the center
private fun calculateFixedPosition(
    moonPos: Equatorial,
    jupiterPos: Equatorial,
    width: Float,
    height: Float,
    raRange: Double,
    decRange: Double,
    centerX: Float,
    centerY: Float
): Offset {
    // Calculate RA and Dec offsets relative to Jupiter's position, using fixed ranges
    val raOffset = (jupiterPos.ra - moonPos.ra) / raRange * width // RA decreases left-to-right
    val decOffset = (moonPos.dec - jupiterPos.dec) / decRange * height

    // Position relative to the center
    val x = centerX + raOffset
    val y = centerY - decOffset

    return Offset(x.toFloat(), y.toFloat())
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
@Composable
fun JovianMoonEventList(events: List<JovianMoonEvent>, modifier: Modifier = Modifier) {
    val sortedEvents = events.sortedBy { it.julianTime }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.LightGray) // Background for visibility
            .padding(4.dp)
    ) {
        items(sortedEvents) { event ->
            JovianMoonEventRow(event)
        }
    }
}

@Composable
fun JovianMoonEventRow(event: JovianMoonEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp) // Reduced padding for a thinner look
            .background(Color.DarkGray) // Adjusted background transparency
    ) {
        Text(
            text = "${event.moon} - ${eventTypeToFriendlyText(event.type)}",
            color = Color.White
        )
        Text(
            text = "Local: ${Utils.julianDateToLocalTime(event.julianTime).format(AppConstants.DATE_TIME_FORMATTER)}",
            color = Color.White
        )
        Text(
            text = "UTC Time: ${Utils.julianDateToUTC(event.julianTime).format(AppConstants.DATE_TIME_FORMATTER_24)}",
            color = Color.White
        )
    }
}

// Helper function to convert EventType to friendly text
private fun eventTypeToFriendlyText(type: EventType): String {
    return when (type) {
        EventType.MOON_ENTERS_JUPITER_TRANSIT -> "begins transit of Jupiter"
        EventType.MOON_EXITS_JUPITER_TRANSIT -> "ends transit of Jupiter"
        EventType.MOON_ENTERS_JUPITER_OCCLUSION -> "enters occultation behind Jupiter"
        EventType.MOON_EXITS_JUPITER_OCCLUSION -> "exits occultation behind Jupiter"
        EventType.MOON_SHADOW_BEGINS_JUPITER_DISK -> "shadow begins to cross Jupiter"
        EventType.MOON_SHADOW_LEAVES_JUPITER_DISK -> "shadow leaves Jupiter's disk"
        EventType.MOON_ENTERS_ECLIPSE_OF_JUPITER -> "enters eclipse by Jupiter's shadow."
        EventType.MOON_EXITS_ECLIPSE_OF_JUPITER -> "exits eclipse by Jupiter's shadow."
    }
}