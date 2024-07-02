package com.jeffrwatts.stargazer.utils

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jeffrwatts.stargazer.R
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

object AppConstants {
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    permissions: List<String>,
    rationaleMessage: String,
    content: @Composable () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    LaunchedEffect(key1 = permissionsState) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = rationaleMessage)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(
    message: String,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.onError)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryClick) {
                Text(text = stringResource(R.string.retry))
            }
        }
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
fun AltitudeChart(entries: List<Utils.AltitudeEntry>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(start = 30.dp), // Leave space for y-axis labels
        onDraw = {
            drawAltitudeChart(entries, size.width, size.height)
        }
    )
}

fun DrawScope.drawAltitudeChart(entries: List<Utils.AltitudeEntry>, chartWidth: Float, chartHeight: Float) {
    val yScale = chartHeight / 180f // Altitude range is 180 degrees (-90 to 90)
    val startTime = entries.firstOrNull()?.time ?: LocalDateTime.now()
    val endTime = entries.lastOrNull()?.time ?: startTime.plusHours(24)
    val duration = Duration.between(startTime, endTime).toMillis().toFloat()
    val xScale = chartWidth / duration // Time scale based on duration

    // Draw light gray shading for daylight hours (6 AM to 6 PM) for each day
    var dayTime = startTime.withHour(6).withMinute(0).withSecond(0)
    while (dayTime.isBefore(endTime)) {
        val daylightStart = Duration.between(startTime, dayTime).toMillis() * xScale
        val daylightEnd = Duration.between(startTime, dayTime.plusHours(12)).toMillis() * xScale
        val adjustedDaylightStart = maxOf(daylightStart, 0f) // Adjust to start at the chart edge if needed
        drawRect(
            color = Color.LightGray.copy(alpha = 0.3f),
            topLeft = Offset(adjustedDaylightStart, 0f),
            size = androidx.compose.ui.geometry.Size(daylightEnd - adjustedDaylightStart, chartHeight)
        )
        dayTime = dayTime.plusDays(1)
    }

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




fun formatToDegreeAndMinutes(angle: Double): String {
    val degrees = angle.toInt()
    val minutes = ((angle - degrees) * 60).roundTo(2)
    return "$degrees° $minutes'"
}

fun formatHoursToHoursMinutes(hoursDecimal: Double): String {
    val hours = hoursDecimal.toInt() // Extract whole hours
    val minutes = ((hoursDecimal - hours) * 60).toInt() // Convert the decimal part to minutes
    return String.format("%dh %02dm", hours, minutes) // Format and return the string
}

fun formatPeriodToDHH(periodInDays: Double): String {
    val days = periodInDays.toInt() // Extract whole days
    val fractionalDay = periodInDays - days // Fraction of a day
    val hoursAsDecimal = fractionalDay * 24 // Convert fraction to hours as a decimal

    // Check if days are 0 and format the output accordingly
    return if (days > 0) {
        "${days}d ${String.format("%.2f", hoursAsDecimal)}h"
    } else {
        "${String.format("%.2f", hoursAsDecimal)}h"
    }
}

private fun Double.roundTo(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return (this * multiplier).roundToInt() / multiplier
}