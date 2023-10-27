package com.jeffrwatts.stargazer.utils

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
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import kotlin.math.pow
import kotlin.math.roundToInt

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