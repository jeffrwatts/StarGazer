package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.updatescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val statusMessages by viewModel.statusMessages.collectAsState()

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
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Last Updated: ${uiState.lastUpdated}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Update Button
                Button(
                    onClick = { viewModel.triggerImageUpdate() },
                    enabled = !uiState.isDownloading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.White)
                ) {
                    Text("Update")
                }

                Spacer(modifier = Modifier.width(8.dp)) // Space between buttons

                // Clear Button
                Button(
                    onClick = { viewModel.clearStatus() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.White)
                ) {
                    Text("Clear")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(statusMessages) { message ->
                    Text(text = message, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    }
}
