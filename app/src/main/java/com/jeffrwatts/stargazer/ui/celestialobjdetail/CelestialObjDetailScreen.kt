package com.jeffrwatts.stargazer.ui.celestialobjdetail

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.celestialobject.getDefaultImageResource
import com.jeffrwatts.stargazer.utils.AltitudePlot
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
                    altitudesData = success.altitudeData,
                    moonAltitudeData = success.moonAltitudeData,
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
    altitudesData: List<Pair<Double, Double>>,
    moonAltitudeData: List<Pair<Double, Double>>,
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
            altitudeData = altitudesData,
            moonAltitudeData = moonAltitudeData,
            startJulianDate = altitudesData[0].first,
            endJulianDate = altitudesData[altitudesData.size-1].first,
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
