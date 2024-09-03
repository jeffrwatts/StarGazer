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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
                title = stringResource(R.string.variable_star_title),
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
                        //TimeControl(
                        //    currentTime = successState.localDateTime.format(AppConstants.DATE_TIME_FORMATTER),
                        //    onIncrementTime = { viewModel.incrementOffset() },
                        //    onDecrementTime = { viewModel.decrementOffset() },
                        //    onResetTime = { viewModel.resetOffset() }
                        //)
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
fun InfoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun InfoContent(
    localDateTime: LocalDateTime,
    location: Location?,
    lhaPolaris: Double,
    modifier: Modifier = Modifier)
{
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date and Time
        item { InfoSectionHeader(title = "Current Date & Time") }
        val time = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        item { Text(text = "Time: $time", style = MaterialTheme.typography.bodyLarge) }
        val date = localDateTime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(text = "Date: $date", style = MaterialTheme.typography.bodyLarge) }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item { InfoSectionHeader(title = "Current Location") }
        // Location
        location?.let { loc->
            item { Text(text ="Latitude: ${decimalToDMS(loc.latitude, "N", "S")}", style = MaterialTheme.typography.bodyLarge ) }
            item { Text(text ="Longitude: ${decimalToDMS(loc.longitude, "E", "W")}", style = MaterialTheme.typography.bodyLarge ) }
            item { Text(text = "Altitude: ${loc.altitude} meters", style = MaterialTheme.typography.bodyLarge) }
            item { Text(text = "Accuracy: ${loc.accuracy} meters", style = MaterialTheme.typography.bodyLarge) }

            item {Spacer(modifier = Modifier.height(16.dp))}
            item { InfoSectionHeader(title = "Position of Polaris") }
            item {
                PolarisPlot(lhaPolaris)
            }
        } ?: run {
            item { Text(text ="Getting Position...", style = MaterialTheme.typography.bodyLarge ) }
        }
    }
}

@Composable
fun PolarisPlot(lhaPolaris: Double) {
    // Constants for plotting
    val scaleFactor = 1f  // Use full scale to ensure the dot is on the circle
    val angularSeparation = 1.0  // Distance on the celestial sphere, use radius directly

    // Convert LHA from degrees to radians
    val lhaRadians = Math.toRadians(lhaPolaris*15.0)

    // Convert to Cartesian coordinates for a circular plot (polar coordinates to Cartesian)
    val x = -scaleFactor * angularSeparation * sin(lhaRadians)  // Negate x to correct reflection
    val y = scaleFactor * angularSeparation * cos(lhaRadians)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Keep the aspect ratio 1:1 to maintain a square shape
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Adjust the radius to make the circle slightly smaller
            val radius = size.minDimension / 2.5f // Smaller radius for the circle

            // Draw the celestial circle (celestial north pole)
            drawCircle(
                color = Color.Blue,
                radius = radius,
                style = Stroke(width = 3f)
            )

            // Draw a red plus sign at the center of the circle to represent the celestial north pole
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

            // Plot the position of Polaris on the blue circle
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Place the red dot exactly on the circle's circumference
            val polarisX = centerX + x * radius
            val polarisY = centerY - y * radius  // Invert y-axis for correct positioning

            drawCircle(
                color = Color.Red,
                radius = 12f, // Make the red dot larger
                center = Offset(polarisX.toFloat(), polarisY.toFloat())
            )
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