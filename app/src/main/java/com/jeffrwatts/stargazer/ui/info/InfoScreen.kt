package com.jeffrwatts.stargazer.ui.info

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

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
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally // Center children horizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Add some space at the top

            PolarView(polarisX = -0.2f, polarisY = 0.0f) // Your PolarView

            Spacer(modifier = Modifier.height(16.dp)) // Add some space between PolarView and LazyColumn

            LazyColumn(
                modifier = Modifier.fillMaxWidth() // Fill the maximum width for LazyColumn
            ) {
                val items = List(50) { "Info #$it" }

                items(items) { item ->
                    Text(text = item, modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp))
                    Divider(thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun PolarView(polarisX: Float, polarisY: Float, circleColor: Color = MaterialTheme.colorScheme.primary) {
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

fun DrawScope.drawPolarContent(polarisX: Float, polarisY: Float, circleColor: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = sqrt(polarisX * polarisX + polarisY * polarisY)

    drawCircle(
        color = circleColor,
        center = center,
        radius = radius,
        style = Stroke(width = 4f)
    )
    drawCircle(
        color = Color.Blue,
        center = center + Offset(polarisX, polarisY),
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
    PolarView(polarisX = -0.2f, polarisY = 0.0f)
}