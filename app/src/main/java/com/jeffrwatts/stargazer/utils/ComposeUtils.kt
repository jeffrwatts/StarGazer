package com.jeffrwatts.stargazer.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jeffrwatts.stargazer.R
import kotlin.math.pow
import kotlin.math.roundToInt

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