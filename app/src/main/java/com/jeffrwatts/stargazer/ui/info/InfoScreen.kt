package com.jeffrwatts.stargazer.ui.info

import android.Manifest
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.PermissionWrapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jeffrwatts.stargazer.utils.AppConstants
import com.jeffrwatts.stargazer.utils.TimeControl
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun InfoScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InfoViewModel = hiltViewModel(),
) {
    val topAppBarState = rememberTopAppBarState()
    val infoUiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.info_title),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        PermissionWrapper(
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            ),
            rationaleMessage = stringResource(id = R.string.permission_rationale)
        ) {
            when (infoUiState) {
                is InfoUiState.Loading -> {
                    LoadingScreen(modifier = contentModifier)
                }

                is InfoUiState.Success -> {
                    val successState = infoUiState as InfoUiState.Success
                    Column(modifier = contentModifier) {
                        TimeControl(
                            currentTime = successState.localDateTime.format(AppConstants.DATE_TIME_FORMATTER),
                            onIncrementTime = { viewModel.incrementOffset() },
                            onDecrementTime = { viewModel.decrementOffset() },
                            onResetTime = { viewModel.resetOffset() }
                        )
                        InfoContent(
                            successState.localDateTime,
                            successState.location,
                            successState.lhaPolaris
                        )
                    }
                }

                else -> {
                    ErrorScreen(
                        message = (infoUiState as InfoUiState.Error).message,
                        modifier = contentModifier,
                        onRetryClick = {  }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoContent(
    localDateTime: LocalDateTime,
    location: Location?,
    lhaPolaris: Double,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start // Align content to the start (left)
    ) {
        // Date and Time
        item {
            InfoSectionHeader(
                title = "Time and Location",
                modifier = Modifier.padding(start = 16.dp) // Slightly less padding for the header
            )
        }

        val time = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        item {
            Text(
                text = "Time: $time",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
            )
        }
        val date = localDateTime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            Text(
                text = "Date: $date",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Location
        location?.let { loc ->
            item {
                Text(
                    text = "Latitude: ${decimalToDMS(loc.latitude, "N", "S")}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
                )
            }
            item {
                Text(
                    text = "Longitude: ${decimalToDMS(loc.longitude, "E", "W")}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
                )
            }
            item {
                Text(
                    text = "Altitude: ${"%.2f".format(loc.altitude)} meters", // Format altitude to 2 decimal places
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
                )
            }
            item {
                Text(
                    text = "Accuracy: ${loc.accuracy} meters",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSectionHeader(
                    title = "Position of Polaris",
                    modifier = Modifier.padding(start = 16.dp) // Slightly less padding for the header
                )
            }

            item {
                // Center the PolarisPlot by wrapping it in a Box with centered alignment
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center // Center PolarisPlot within the Box
                ) {
                    PolarisPlot(lhaPolaris, 6.0)
                }
            }
        } ?: run {
            item {
                Text(
                    text = "Getting Position...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 32.dp) // Increase padding for fields
                )
            }
        }
    }
}

@Composable
fun InfoSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline),
        modifier = modifier // Use the modifier provided
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PolarisPlot(lhaPolaris: Double, finderScopeFovDegrees: Double) {
    // State to control the checkbox
    var isFinderScopeView by remember { mutableStateOf(false) }

    // Constants for plotting
    val scaleFactor = 1f  // Use full scale to ensure the dot is on the circle
    val angularSeparation = 0.7  // Polaris is approximately 0.7 degrees from the celestial north pole

    // Convert LHA from degrees to radians
    val lhaRadians = Math.toRadians(lhaPolaris * 15.0)

    // Convert to Cartesian coordinates for a circular plot (polar coordinates to Cartesian)
    val x = -scaleFactor * sin(lhaRadians)  // Negate x to correct reflection
    val y = scaleFactor * cos(lhaRadians)

    // Determine the plotting coordinates based on the checkbox state
    val adjustedX = if (isFinderScopeView) -x else x
    val adjustedY = if (isFinderScopeView) -y else y

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Checkbox to toggle view mode
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isFinderScopeView,
                onCheckedChange = { isChecked ->
                    isFinderScopeView = isChecked
                }
            )
            Text(text = "View through Finder Scope")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Keep the aspect ratio 1:1 to maintain a square shape
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Adjust the radius to represent the field of view of the finder scope
                val outerRadius = size.minDimension / 2.2f  // Larger radius for the outer circle (total field of view)
                val innerRadius = outerRadius * (angularSeparation / (finderScopeFovDegrees / 2)) // Scaled radius for Polaris' 0.7 degree separation

                // Draw the outer circle representing the total field of view of the finder scope
                drawCircle(
                    color = Color.Gray,
                    radius = outerRadius,
                    style = Stroke(width = 2f)
                )

                // Draw the celestial north pole circle (smaller inner circle)
                drawCircle(
                    color = Color.Blue,
                    radius = innerRadius.toFloat(),
                    style = Stroke(width = 2f)
                )

                // Draw a red plus sign at the center of the inner circle to represent the celestial north pole
                drawLine(
                    color = Color.Red,
                    start = Offset(size.width / 2, size.height / 2 - 10),
                    end = Offset(size.width / 2, size.height / 2 + 10),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.Red,
                    start = Offset(size.width / 2 - 10, size.height / 2),
                    end = Offset(size.width / 2 + 10, size.height / 2),
                    strokeWidth = 3f
                )

                // Plot the position of Polaris on the inner circle
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Place the red dot on the inner circle's circumference
                val polarisX = centerX + adjustedX * innerRadius
                val polarisY = centerY - adjustedY * innerRadius  // Invert y-axis for correct positioning

                drawCircle(
                    color = Color.Red,
                    radius = 12f, // Make the red dot larger
                    center = Offset(polarisX.toFloat(), polarisY.toFloat())
                )
            }
        }
    }
}


fun decimalToDMS(decimal: Double, dirPos: String, dirNeg: String): String {
    val degrees = decimal.toInt()
    val minutes = ((decimal - degrees) * 60).toInt()
    val seconds = (((decimal - degrees) * 60 - minutes) * 60)
    val direction = if (decimal >= 0) dirPos else dirNeg
    return "%02d°%02d'%05.2f\"%s".format(abs(degrees), abs(minutes), abs(seconds), direction)
}