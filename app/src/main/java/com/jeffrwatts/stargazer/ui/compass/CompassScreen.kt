package com.jeffrwatts.stargazer.ui.compass

import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
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

    val accuracyDescription = when (uiState.compassData.accuracy) {
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
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val magHeading = uiState.compassData.direction
            if (uiState.isMagDeclinationValid) {
                val trueHeading = (((magHeading + uiState.magDeclination) % 360) + 360) % 360
                Text(text = "True Heading: ${trueHeading.roundToInt()}°", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(text = "True Heading: Not Available", style = MaterialTheme.typography.bodyLarge)
            }
            Text(text = "Magnetic Heading: ${magHeading.roundToInt()}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = if (uiState.isMagDeclinationValid) "Declination: ${String.format("%.1f", uiState.magDeclination)}°" else "Declination: Not Available", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Accuracy: $accuracyDescription", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
