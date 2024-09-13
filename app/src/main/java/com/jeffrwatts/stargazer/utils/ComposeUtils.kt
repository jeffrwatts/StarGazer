package com.jeffrwatts.stargazer.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jeffrwatts.stargazer.R
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
fun TimeControl(
    currentTime: String,
    onIncrementTime: () -> Unit,
    onDecrementTime: () -> Unit,
    onResetTime: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDecrementTime) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrement Time")
        }
        Text(
            text = currentTime,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onIncrementTime) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Increment Time")
        }
        IconButton(onClick = onResetTime) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Time")
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
fun AltitudePlot(
    altitudeData: List<Pair<Double, Double>>, // Julian date and altitude in degrees
    moonAltitudeData: List<Pair<Double, Double>>, // Julian date and altitude in degrees
    startJulianDate: Double, // Julian date for the start of night
    endJulianDate: Double,   // Julian date for the end of night
    currentAltitudeIndex: Int, // Index into the altitude data for the current time (-1 if not during the night)
    xAxisLabels: List<String> // Pre-calculated x-axis labels
) {
    val maxHeight = 90f // Fixed y-axis range from -90 to 90 degrees
    val minHeight = -90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF0E1A28)) // Background color
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val leftPadding = 30.dp.toPx() // Adjusted padding only on the left side
            val rightPadding = 20.dp.toPx() // Keep default padding on the right

            // Draw grid lines (vertical and horizontal)
            val numVerticalLines = 4
            val numHorizontalLines = 6

            val xStep = (width - leftPadding - rightPadding) / numVerticalLines
            val yStep = (height - 2 * rightPadding) / numHorizontalLines

            // Horizontal grid lines
            for (i in 0..numHorizontalLines) {
                val y = rightPadding + i * yStep
                drawLine(
                    color = if (i == numHorizontalLines / 2) Color.White else Color.Gray, // Emphasize the 0-degree line
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = if (i == numHorizontalLines / 2) 2.dp.toPx() else 1.dp.toPx() // Thicker line for 0°
                )
            }

            // Vertical grid lines
            for (i in 0..numVerticalLines) {
                val x = leftPadding + i * xStep
                drawLine(
                    color = Color.Gray,
                    start = Offset(x, rightPadding),
                    end = Offset(x, height - rightPadding),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Create a path for the altitude curve
            val path = Path()
            val totalNightDuration = endJulianDate - startJulianDate

            // Draw altitude curve
            altitudeData.forEachIndexed { index, (julianDate, altitude) ->
                val x = leftPadding + ((julianDate - startJulianDate) / totalNightDuration).toFloat() * (width - leftPadding - rightPadding)
                val y = rightPadding + (1 - (altitude.toFloat() - minHeight) / (maxHeight - minHeight)) * (height - 2 * rightPadding)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw the path
            drawPath(
                path = path,
                color = Color(0xFF70D2FF),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Create the moon path for the altitude curve
            if (moonAltitudeData.isNotEmpty()) {
                val moonPath = Path()

                // Draw altitude curve
                moonAltitudeData.forEachIndexed { index, (julianDate, altitude) ->
                    val x = leftPadding + ((julianDate - startJulianDate) / totalNightDuration).toFloat() * (width - leftPadding - rightPadding)
                    val y = rightPadding + (1 - (altitude.toFloat() - minHeight) / (maxHeight - minHeight)) * (height - 2 * rightPadding)

                    if (index == 0) {
                        moonPath.moveTo(x, y)
                    } else {
                        moonPath.lineTo(x, y)
                    }
                }

                // Draw the path
                drawPath(
                    path = moonPath,
                    color = Color(0xFFFFFFFF),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw the current altitude position, if the index is valid
            if (currentAltitudeIndex >= 0 && currentAltitudeIndex < altitudeData.size) {
                val (currentJulianDate, currentAltitude) = altitudeData[currentAltitudeIndex]
                val currentX = leftPadding + ((currentJulianDate - startJulianDate) / totalNightDuration).toFloat() * (width - leftPadding - rightPadding)
                val currentY = rightPadding + (1 - (currentAltitude.toFloat() - minHeight) / (maxHeight - minHeight)) * (height - 2 * rightPadding)
                drawCircle(
                    color = Color(0xFF70D2FF),
                    radius = 5.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
            }
        }

        // X-axis labels positioned below the grid line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 16.dp), // Positioning below the lower grid line
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            xAxisLabels.forEach { label ->
                Text(label, color = Color.White, fontSize = 12.sp)
            }
        }

        // Y-axis labels: 90° at the top, 0° in the middle
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight() // Fill height to vertically center the label
                .padding(start = 8.dp)
        ) {
            Text(
                "0°",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center) // Center the text within the Box
            )
        }
    }
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