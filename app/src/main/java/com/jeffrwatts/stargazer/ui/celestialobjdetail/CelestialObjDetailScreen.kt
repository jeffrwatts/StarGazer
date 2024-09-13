package com.jeffrwatts.stargazer.ui.celestialobjdetail

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.celestialobject.getDefaultImageResource
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LabeledField
import com.jeffrwatts.stargazer.utils.LoadingScreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelestialObjDetailScreen(
    sightId: Int,
    onNavigateBack: () -> Unit,
    onMoreInfo:(String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CelestialObjDetailViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sightId) {
        viewModel.initDetail(sightId)
    }

    // Update the title when uiState changes
    var title by remember { mutableStateOf("Loading...") } // default title

    LaunchedEffect(uiState) {
        if (uiState is CelestialObjDetailUiState.Success) {
            title = (uiState as CelestialObjDetailUiState.Success).celestialObjWithImage.celestialObj.displayName
        }
    }

    Scaffold(
        topBar = {
            CelestialObjDetailTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (uiState) {
            is CelestialObjDetailUiState.Loading -> {
                LoadingScreen(modifier = contentModifier)
            }
            is CelestialObjDetailUiState.Success -> {
                val success = (uiState as CelestialObjDetailUiState.Success)
                CelestialObjDetailContent(
                    celestialObjWithImage = success.celestialObjWithImage,
                    currentTimeIndex = success.currentTimeIndex,
                    altitudes = success.altitudes,
                    xAxisLabels= success.xAxisLabels,
                    onMoreInfo= { onMoreInfo(buildMoreInfoUri(success.celestialObjWithImage.celestialObj.objectId, success.celestialObjWithImage.celestialObj.displayName))},
                    modifier = contentModifier)
            }
            else -> {//is SightDetailUiState.Error -> {
                ErrorScreen(
                    message = (uiState as CelestialObjDetailUiState.Error).message,
                    modifier = contentModifier,
                    onRetryClick = { viewModel.initDetail(sightId) }
                )
            }
        }
    }
}

@Composable
fun CelestialObjDetailContent(
    celestialObjWithImage: CelestialObjWithImage,
    currentTimeIndex: Int,
    altitudes: List<Pair<Double, Double>>,
    xAxisLabels: List<String>,
    onMoreInfo:() -> Unit,
    modifier: Modifier = Modifier)
{
    Column(modifier = modifier) {
        // Banner Image
        val imageFile = celestialObjWithImage.image?.let { image ->
            File(image.filename)
        }

        val defaultImagePainter: Painter = painterResource(id = celestialObjWithImage.celestialObj.getDefaultImageResource())

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageFile)
                .error(celestialObjWithImage.celestialObj.getDefaultImageResource())
                .build(),
            contentDescription = "Celestial Object",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop,
            placeholder = defaultImagePainter,  // Use as a placeholder as well
            onError = {
                // Handle errors if necessary, for instance logging
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Data Fields
        LabeledField(label = "NGC ID", value = celestialObjWithImage.celestialObj.ngcId ?: "N/A")
        LabeledField(label = "Subtype", value = celestialObjWithImage.celestialObj.subType ?: "N/A")
        LabeledField(label = "Constellation", value = celestialObjWithImage.celestialObj.constellation ?: "N/A")
        LabeledField(label = "Magnitude", value = celestialObjWithImage.celestialObj.magnitude?.toString() ?: "N/A")
        Button(
            onClick = onMoreInfo,
            colors = ButtonDefaults.buttonColors(contentColor = Color.White))
        {
            Text(text = "Display More Info")
        }
        Spacer(modifier = Modifier.height(16.dp))
        AltitudePlot(
            altitudeData = altitudes,
            startJulianDate = altitudes[0].first,
            endJulianDate = altitudes[altitudes.size-1].first,
            currentAltitudeIndex = currentTimeIndex,
            xAxisLabels = xAxisLabels
        )
    }
}

fun buildMoreInfoUri(objectId: String, displayName: String): String {
    val baseUrl = "https://en.wikipedia.org/wiki/"

    val url =  when {
        objectId.startsWith("m", ignoreCase = true) -> {
            val number = objectId.substring(1)
            baseUrl + "Messier_$number"
        }
        objectId.startsWith("ngc", ignoreCase = true) -> {
            val number = objectId.substring(3)
            baseUrl + "NGC_$number"
        }
        else -> {
            val formattedName = displayName.replace(' ', '_')
            "$baseUrl$formattedName"
        }
    }

    return Uri.encode(url)
}

@Composable
fun AltitudePlot(
    altitudeData: List<Pair<Double, Double>>, // Julian date and altitude in degrees
    startJulianDate: Double, // Julian date for the start of night
    endJulianDate: Double,   // Julian date for the end of night
    currentAltitudeIndex: Int, // Index into the altitude data for the current time (-1 if not during the night)
    xAxisLabels: List<String> // Pre-calculated x-axis labels
) {
    val maxHeight = 90f // Fixed y-axis range from -90 to 90 degrees
    val minHeight = -90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF0E1A28)) // Background color
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val leftPadding = 30.dp.toPx() // Adjusted padding only on the left side
            val rightPadding = 20.dp.toPx() // Keep default padding on the right

            // Draw grid lines (vertical and horizontal)
            val numVerticalLines = 4
            val numHorizontalLines = 6

            val xStep = (width - leftPadding - rightPadding) / numVerticalLines
            val yStep = (height - 2 * rightPadding) / numHorizontalLines

            // Horizontal grid lines
            for (i in 0..numHorizontalLines) {
                val y = rightPadding + i * yStep
                drawLine(
                    color = if (i == numHorizontalLines / 2) Color.White else Color.Gray, // Emphasize the 0-degree line
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = if (i == numHorizontalLines / 2) 2.dp.toPx() else 1.dp.toPx() // Thicker line for 0°
                )
            }

            // Vertical grid lines
            for (i in 0..numVerticalLines) {
                val x = leftPadding + i * xStep
                drawLine(
                    color = Color.Gray,
                    start = Offset(x, rightPadding),
                    end = Offset(x, height - rightPadding),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Create a path for the altitude curve
            val path = Path()
            val totalNightDuration = endJulianDate - startJulianDate

            // Draw altitude curve
            altitudeData.forEachIndexed { index, (julianDate, altitude) ->
                val x = leftPadding + ((julianDate - startJulianDate) / totalNightDuration).toFloat() * (width - leftPadding - rightPadding)
                val y = rightPadding + (1 - (altitude.toFloat() - minHeight) / (maxHeight - minHeight)) * (height - 2 * rightPadding)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw the path
            drawPath(
                path = path,
                color = Color(0xFF70D2FF),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw the current altitude position, if the index is valid
            if (currentAltitudeIndex >= 0 && currentAltitudeIndex < altitudeData.size) {
                val (currentJulianDate, currentAltitude) = altitudeData[currentAltitudeIndex]
                val currentX = leftPadding + ((currentJulianDate - startJulianDate) / totalNightDuration).toFloat() * (width - leftPadding - rightPadding)
                val currentY = rightPadding + (1 - (currentAltitude.toFloat() - minHeight) / (maxHeight - minHeight)) * (height - 2 * rightPadding)
                drawCircle(
                    color = Color(0xFF70D2FF),
                    radius = 5.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
            }
        }

        // X-axis labels positioned below the grid line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 16.dp), // Positioning below the lower grid line
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            xAxisLabels.forEach { label ->
                Text(label, color = Color.White, fontSize = 12.sp)
            }
        }

        // Y-axis labels: 90° at the top, 0° in the middle
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight() // Fill height to vertically center the label
                .padding(start = 8.dp)
        ) {
            Text(
                "0°",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center) // Center the text within the Box
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelestialObjDetailTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
) {
    CenterAlignedTopAppBar(
        title = {
            Text(title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = { onNavigateBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}
