package com.jeffrwatts.stargazer.ui.info

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun InfoScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InfoViewModel = hiltViewModel(),
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val uiState by viewModel.state.collectAsState()

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
            .nestedScroll(scrollBehavior.nestedScrollConnection)

        InfoContent(uiState = uiState,
            triggerUpdate = { viewModel.triggerImageUpdate() },
            modifier = contentModifier)
    }
}

@Composable
fun InfoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(textDecoration = TextDecoration.Underline)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun InfoContent(
    uiState: InfoUiState,
    triggerUpdate: () -> Unit,
    modifier: Modifier)
{
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date and Time
        item { InfoSectionHeader(title = "Current Date & Time") }
        item { Text(text = "Time: ${uiState.currentTime}", style = MaterialTheme.typography.bodyLarge) }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(text = "Date: ${uiState.currentDate}", style = MaterialTheme.typography.bodyLarge) }

        // Location
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { InfoSectionHeader(title = "Current Location") }
        item { Text(text = "Latitude: ${uiState.latitude}", style = MaterialTheme.typography.bodyLarge) }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(text = "Longitude: ${uiState.longitude}", style = MaterialTheme.typography.bodyLarge) }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(text = "Accuracy: ${uiState.accuracy}", style = MaterialTheme.typography.bodyLarge) }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(text = "Altitude: ${uiState.altitude}", style = MaterialTheme.typography.bodyLarge) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Images: ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (uiState.isDownloading) uiState.downloadStatus else "Last updated: ${uiState.lastUpdated}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Button(
                    onClick = { triggerUpdate() },
                    enabled = !uiState.isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White  // Sets the text and icon color
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("Update")
                }
            }
        }

        // Polar View - Note, currently just using finder scope for this, so hard code horizontal/vertical orientation.
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { InfoSectionHeader(title = "Polar View") }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            PolarView(polarisX = uiState.polarisX,
                polarisY = uiState.polarisY,
                isHorizontalFlip = false,
                isVerticalFlip = true)
        }
    }
}
@Composable
fun PolarView(polarisX: Double, polarisY: Double, isHorizontalFlip: Boolean, isVerticalFlip: Boolean, circleColor: Color = MaterialTheme.colorScheme.primary) {
    if (polarisX == 0.0 && polarisY == 0.0)
        return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val desiredSize = screenWidth - 128.dp // Subtract padding

    // Adjust polarisX and polarisY based on desiredSize
    val scale = desiredSize.value / maxOf(abs(polarisX), abs(polarisY))
    val adjustedPolarisX = polarisX * scale
    val adjustedPolarisY = polarisY * scale

    // Draw content
    Canvas(
        modifier = Modifier
            .size(desiredSize)
            .graphicsLayer(
                scaleX = if (isHorizontalFlip) -1f else 1f,
                scaleY = if (isVerticalFlip) -1f else 1f
            )
            .onGloballyPositioned {
                // Handle the positioning here if needed
            },
        onDraw = {
            drawPolarContent(adjustedPolarisX, adjustedPolarisY, circleColor)
        }
    )
}

fun DrawScope.drawPolarContent(polarisX: Double, polarisY: Double, circleColor: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = sqrt(polarisX * polarisX + polarisY * polarisY)

    drawCircle(
        color = circleColor,
        center = center,
        radius = radius.toFloat(),
        style = Stroke(width = 4f)
    )
    drawCircle(
        color = Color.Blue,
        center = center + Offset(polarisX.toFloat(), polarisY.toFloat()),
        radius = 10f,
        style = Stroke(width = 10f)
    )
    drawLine(
        color = Color.Red,
        start = center.copy(y = center.y - 25),
        end = center.copy(y = center.y + 25),
        strokeWidth = 10f
    )
    drawLine(
        color = Color.Red,
        start = center.copy(x = center.x - 25),
        end = center.copy(x = center.x + 25),
        strokeWidth = 10f
    )
}

@Preview
@Composable
fun PolarViewPreview() {
    PolarView(polarisX = -0.2, polarisY = 0.0, isHorizontalFlip = false, isVerticalFlip = true)
}