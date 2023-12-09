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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompassViewModel = hiltViewModel()
) {
    val compassData = viewModel.compassDirection.collectAsState().value
    val topAppBarState = rememberTopAppBarState()

    val accuracyDescription = when (compassData.accuracy) {
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
        viewModel.setupSensors()
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Compass Direction: ${String.format("%.1f", compassData.direction)}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Accuracy: $accuracyDescription", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
