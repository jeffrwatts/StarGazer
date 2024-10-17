package com.jeffrwatts.stargazer.ui.variablestardetail

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LabeledField
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.utils.AltitudePlot
import com.jeffrwatts.stargazer.utils.AppConstants.DATE_TIME_FORMATTER
import com.jeffrwatts.stargazer.utils.decimalDecToDmsString
import com.jeffrwatts.stargazer.utils.decimalRaToHmsString
import com.jeffrwatts.stargazer.utils.formatPeriodToDHH
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariableStarDetailScreen(
    variableStarId: Int,
    observationTime: LocalDateTime,
    onNavigateBack: () -> Unit,
    onDisplayEphemeris: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariableStarDetailViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(variableStarId, observationTime) {
        viewModel.initDetail(variableStarId, observationTime)
    }

    // Update the title when uiState changes
    var title by remember { mutableStateOf("Loading...") } // default title

    LaunchedEffect(uiState) {
        if (uiState is VariableStarDetailUiState.Success) {
            title = (uiState as VariableStarDetailUiState.Success).variableStarObj.displayName
        }
    }

    Scaffold(
        topBar = {
            VariableStarTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (uiState) {
            is VariableStarDetailUiState.Loading -> {
                LoadingScreen(modifier = contentModifier)
            }
            is VariableStarDetailUiState.Success -> {
                val success = (uiState as VariableStarDetailUiState.Success)
                VariableStarDetailContent(
                    variableStarObj = success.variableStarObj,
                    observationTime = observationTime,
                    currentTimeIndex = success.currentTimeIndex,
                    altitudeData = success.altitudeData,
                    moonAltitudeData = success.moonAltitudeData,
                    xAxisLabels = success.xAxisLabels,
                    onDisplayEphemeris = { oid-> onDisplayEphemeris(buildEphemerisUri(oid)) },
                    modifier = contentModifier)
            }
            else -> { //is VariableStarDetailUiState.Error -> {
                ErrorScreen(
                    message = (uiState as VariableStarDetailUiState.Error).message,
                    modifier = contentModifier,
                    onRetryClick = { viewModel.initDetail(variableStarId, observationTime) }
                )
            }
        }
    }
}

@Composable
fun VariableStarDetailContent(
    variableStarObj: VariableStarObj,
    observationTime: LocalDateTime,
    currentTimeIndex: Int,
    altitudeData: List<Pair<Double, Double>>,
    moonAltitudeData: List<Pair<Double, Double>>,
    xAxisLabels: List<String>,
    onDisplayEphemeris: (Long) -> Unit,
    modifier: Modifier = Modifier)
{
    Column(modifier = modifier) {
        // Banner Image
        Image(
            painter = painterResource(id = R.drawable.star),
            contentDescription = "Banner image for ${variableStarObj.displayName}",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Data Fields
        LabeledField(label = "RA", value = decimalRaToHmsString(variableStarObj.ra))
        LabeledField(label = "DEC", value = decimalDecToDmsString(variableStarObj.dec))
        LabeledField(label = "Magnitude High", value = variableStarObj.magnitudeHigh)
        LabeledField(label = "Magnitude Low", value = variableStarObj.magnitudeLow)
        LabeledField(label = "Period", value = formatPeriodToDHH(variableStarObj.period))

        Button(
            onClick = { onDisplayEphemeris(variableStarObj.OID) },
            colors = ButtonDefaults.buttonColors(contentColor = Color.White))
        {
            Text(text = "Display Ephemeris")
        }

        Spacer(modifier = Modifier.height(16.dp))
        LabeledField(label = "Obs Time", value = observationTime.format(DATE_TIME_FORMATTER))
        AltitudePlot(
            altitudeData = altitudeData,
            moonAltitudeData = moonAltitudeData,
            startJulianDate = altitudeData[0].first,
            endJulianDate = altitudeData[altitudeData.size-1].first,
            currentAltitudeIndex = currentTimeIndex,
            xAxisLabels = xAxisLabels
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun buildEphemerisUri(oid: Long): String {
    val url = "https://www.aavso.org/vsx/index.php?view=detail.ephemeris&nolayout=1&oid=${oid}"
    return Uri.encode(url)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariableStarTopAppBar(
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
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}