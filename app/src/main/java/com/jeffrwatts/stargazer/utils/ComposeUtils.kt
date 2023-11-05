package com.jeffrwatts.stargazer.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
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

private fun Double.roundTo(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return (this * multiplier).roundToInt() / multiplier
}