package com.jeffrwatts.stargazer.ui.info

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.AppViewModelProvider
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import com.jeffrwatts.stargazer.ui.polar.PolarAlignViewModel
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InfoViewModel = viewModel(factory = AppViewModelProvider.Factory),
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

        Column(
            modifier = contentModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Current Time
            InfoSectionHeader(title = "Current Time")
            Text(text = uiState.currentTime, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // Current Date
            InfoSectionHeader(title = "Current Date")
            Text(text = uiState.currentDate, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // Current Location
            InfoSectionHeader(title = "Current Location")
            Text(text = "Latitude: ${uiState.latitude}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Longitude: ${uiState.longitude}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // PolarView
            InfoSectionHeader(title = "Polar View")
            PolarView(polarisX = uiState.polarisX, polarisY = uiState.polarisY)
        }
    }
}

@Composable
fun InfoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(textDecoration = TextDecoration.Underline)
    )
    Spacer(modifier = Modifier.height(8.dp))
}


@Composable
fun PolarView(polarisX: Double, polarisY: Double, circleColor: Color = MaterialTheme.colorScheme.primary) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val desiredSize = screenWidth - 32.dp // Subtract padding

    // Adjust polarisX and polarisY based on desiredSize
    val scale = desiredSize.value / maxOf(abs(polarisX), abs(polarisY))
    val adjustedPolarisX = polarisX * scale
    val adjustedPolarisY = polarisY * scale

    // Draw content
    Canvas(
        modifier = Modifier
            .size(desiredSize)
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
    PolarView(polarisX = -0.2, polarisY = 0.0)
}