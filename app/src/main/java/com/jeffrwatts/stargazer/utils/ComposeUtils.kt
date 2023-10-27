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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjAltAzm
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
@Composable
fun SkyItemList(
    celestialObjs: List<CelestialObjAltAzm>,
    onItemClick: (CelestialObjAltAzm) -> Unit,
    onObservationStatusChanged: (CelestialObjAltAzm, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items = celestialObjs, key = { it.celestialObj.id }) { item ->
            SkyItem(celestialObjAltAzm = item,
                onObservationStatusChanged = onObservationStatusChanged,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onItemClick(item) })
        }
    }
}

@Composable
fun SkyItem(
    celestialObjAltAzm: CelestialObjAltAzm,
    onObservationStatusChanged: (CelestialObjAltAzm, ObservationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAboveHorizon = celestialObjAltAzm.alt > 10
    val backgroundColor = when {
        isAboveHorizon -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.background
    }
    val textColor = when {
        isAboveHorizon -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = celestialObjAltAzm.celestialObj.primaryName,
                    style = MaterialTheme.typography.bodyLarge
                )
                StarRating(
                    observationStatus = celestialObjAltAzm.celestialObj.observationStatus,
                    onStatusChanged = { newStatus -> onObservationStatusChanged(celestialObjAltAzm, newStatus) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = celestialObjAltAzm.celestialObj.ngcId ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = celestialObjAltAzm.celestialObj.type.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Alt: ${formatToDegreeAndMinutes(celestialObjAltAzm.alt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Azm: ${formatToDegreeAndMinutes(celestialObjAltAzm.azm)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun formatToDegreeAndMinutes(angle: Double): String {
    val degrees = angle.toInt()
    val minutes = ((angle - degrees) * 60).roundTo(2)
    return "$degrees° $minutes'"
}

private fun Double.roundTo(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return (this * multiplier).roundToInt() / multiplier
}

@Composable
fun StarRating(
    observationStatus: ObservationStatus,
    onStatusChanged: (ObservationStatus) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val shouldFill = index < observationStatus.ordinal
            Icon(
                imageVector = if (shouldFill) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null, // Provide suitable content description
                tint = if (shouldFill) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f/*ContentAlpha.medium*/),
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        val newStatus = when (index) {
                            0 -> if (observationStatus == ObservationStatus.POOR) ObservationStatus.NOT_OBSERVED else ObservationStatus.POOR
                            1 -> if (observationStatus == ObservationStatus.GOOD) ObservationStatus.NOT_OBSERVED else ObservationStatus.GOOD
                            2 -> if (observationStatus == ObservationStatus.GREAT) ObservationStatus.NOT_OBSERVED else ObservationStatus.GREAT
                            else -> ObservationStatus.NOT_OBSERVED
                        }
                        onStatusChanged(newStatus)
                    }
            )
        }
    }
}