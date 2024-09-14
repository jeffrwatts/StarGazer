package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.tooling.preview.Preview
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar.VariableStarEvent
import com.jeffrwatts.stargazer.ui.celestialobjdetail.CelestialObjDetailTopAppBar
import com.jeffrwatts.stargazer.ui.celestialobjdetail.CelestialObjDetailUiState
import com.jeffrwatts.stargazer.utils.ErrorScreen
import com.jeffrwatts.stargazer.utils.LabeledField
import com.jeffrwatts.stargazer.utils.LoadingScreen
import com.jeffrwatts.stargazer.utils.decimalDecToDmsString
import com.jeffrwatts.stargazer.utils.decimalRaToHmsString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldOfViewScreen(
    sightId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldOfViewViewModel = hiltViewModel()
) {
    // Observe the ViewModel state
    val uiState by viewModel.uiState.collectAsState()

    // Trigger data loading
    LaunchedEffect(sightId) {
        viewModel.initDetail(sightId)
    }

    // Update the title when uiState changes
    var title by remember { mutableStateOf("Loading...") } // default title

    LaunchedEffect(uiState) {
        if (uiState is FieldOfViewUiState.Success) {
            title = (uiState as FieldOfViewUiState.Success).celestialObjWithImage.celestialObj.displayName
        }
    }

    Scaffold(
        topBar = {
            FieldOfViewTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        val contentModifier = modifier
            .padding(innerPadding)
            .fillMaxSize()

        // Display content based on the state
        when (uiState) {
            is FieldOfViewUiState.Loading -> {
                LoadingScreen(modifier = contentModifier)
            }

            is FieldOfViewUiState.Success -> {
                // Create the UI with image and text fields
                val success = (uiState as FieldOfViewUiState.Success)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display the image
                    val painter = rememberAsyncImagePainter(success.imageData)
                    Image(
                        painter = painter,
                        contentDescription = "Sky View Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display RA, Dec, and Name
                    LabeledField(label = "RA", value = decimalRaToHmsString(success.celestialObjWithImage.celestialObj.ra))
                    LabeledField(label = "DEC", value = decimalDecToDmsString(success.celestialObjWithImage.celestialObj.dec))
                }
            }

            else -> {
                ErrorScreen(
                    message = (uiState as FieldOfViewUiState.Error).message,
                    modifier = contentModifier,
                    onRetryClick = { viewModel.initDetail(sightId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldOfViewTopAppBar(
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


