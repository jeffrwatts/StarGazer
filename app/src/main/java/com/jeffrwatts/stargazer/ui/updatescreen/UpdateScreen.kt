package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.updatescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.ui.StarGazerTopAppBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun UpdateScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val uiState by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            StarGazerTopAppBar(
                title = stringResource(R.string.update_title),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection)

        UpdateContent(uiState = uiState,
            triggerUpdate = { viewModel.triggerImageUpdate() },
            modifier = contentModifier)
    }
}

@Composable
fun UpdateContent(
    uiState: UpdateUiState,
    triggerUpdate: () -> Unit,
    modifier: Modifier)
{
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
    }
}