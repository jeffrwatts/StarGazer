package com.jeffrwatts.stargazer.ui.sightdetail

import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.getImageResource
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightDetailScreen(
    sightId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SightDetailViewModel = hiltViewModel(),
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
            SightDetailTopAppBar(
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
                SightDetailContent(celestialObj = celestialObj, entries = altitudes, modifier = contentModifier)
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
fun SightDetailContent(celestialObj: CelestialObj, entries: List<AltitudeEntry>, modifier: Modifier = Modifier) {
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
        LabeledField(label = "Catalog ID", value = celestialObj.catalogId ?: "N/A")
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

@Composable
fun LabeledField(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
fun AltitudeChart(entries: List<AltitudeEntry>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(start = 50.dp), // Leave space for y-axis labels
        onDraw = {
            drawAltitudeChart(entries, size.width, size.height)
        }
    )
}

fun DrawScope.drawAltitudeChart(entries: List<AltitudeEntry>, chartWidth: Float, chartHeight: Float) {
    val yScale = chartHeight / 180f // Altitude range is 180 degrees (-90 to 90)
    val startTime = entries.firstOrNull()?.time ?: LocalDateTime.now()
    val endTime = entries.lastOrNull()?.time ?: startTime.plusHours(24)
    val duration = Duration.between(startTime, endTime).toMillis().toFloat()
    val xScale = chartWidth / duration // Time scale based on duration

    // Draw grid lines for hours and labels
    val hours = Duration.between(startTime, endTime).toHours().toInt()
    for (hour in 0..hours) {
        val x = hour * xScale * 3600000 // Convert hours to milliseconds and scale
        val time = startTime.plusHours(hour.toLong())
        val label = when (time.hour) {
            0 -> time.format(DateTimeFormatter.ofPattern("MMM d"))
            6 -> "6AM"
            12 -> "12PM"
            18 -> "6PM"
            else -> ""
        }
        drawLine(
            color = if (label.isEmpty()) Color.LightGray.copy(alpha = 0.3f) else Color.White, // Lighter color for non-labeled lines
            start = Offset(x, 0f),
            end = Offset(x, chartHeight),
            strokeWidth = if (hour % 6 == 0) 2f else 1f // Thicker lines for labeled hours
        )
        if (label.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                chartHeight + 30f, // Position slightly below the chart
                Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 35f // Larger text size
                }
            )
        }
    }

    // Draw grid lines for altitude and labels
    for (altitude in -90..90 step 30) {
        val y = chartHeight - (altitude + 90) * yScale
        val label = if (altitude == 90 || altitude == -90) "${altitude}°" else ""
        drawLine(
            color = Color.White,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = if (altitude == 0) 2f else 1f // Thicker line for 0 degrees
        )
        if (label.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                label,
                -45f, // Position to the left of the chart
                y + 15f, // Center vertically with the line
                Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 35f // Larger text size
                }
            )
        }
    }

    // Draw the altitude path
    val path = Path().apply {
        entries.forEachIndexed { index, entry ->
            val x = Duration.between(startTime, entry.time).toMillis() * xScale
            val y = chartHeight - (entry.alt + 90) * yScale
            if (index == 0) {
                moveTo(x, y.toFloat())
            } else {
                lineTo(x, y.toFloat())
            }
        }
    }

    drawPath(
        path = path,
        color = Color.Blue,
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw a white border along the right side of the grid
    drawLine(
        color = Color.White,
        start = Offset(chartWidth, 0f),
        end = Offset(chartWidth, chartHeight),
        strokeWidth = 2f
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightDetailTopAppBar(
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
