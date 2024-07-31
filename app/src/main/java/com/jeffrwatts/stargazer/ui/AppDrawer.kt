package com.jeffrwatts.stargazer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jeffrwatts.stargazer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currentRoute: String,
    navigateToDeepSkyObjects: () -> Unit,
    navigateToVariableStar: () -> Unit,
    navigateToInfo: () -> Unit,
    navigateToAltAzmTool: () -> Unit,
    navigateToUpdate:()->Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier) {
        StarGazerHeader(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.sky_tonight)) },
            icon = { Icon(Icons.Filled.Radar, null) },
            selected = currentRoute == StarGazerDestinations.SKY_TONIGHT_ROUTE,
            onClick = { navigateToDeepSkyObjects(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.variable_star_title)) },
            icon = { Icon(Icons.Filled.Star, null) },
            selected = currentRoute == StarGazerDestinations.VARIABLE_STAR_ROUTE,
            onClick = { navigateToVariableStar(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.info_title)) },
            icon = { Icon(Icons.Filled.Navigation, null) },
            selected = currentRoute == StarGazerDestinations.INFO_ROUTE,
            onClick = { navigateToInfo(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.alt_azm_tool)) },
            icon = { Icon(Icons.Filled.AddCircleOutline, null) },
            selected = currentRoute == StarGazerDestinations.ALT_AZM_TOOL_ROUTE,
            onClick = { navigateToAltAzmTool(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.update_title)) },
            icon = { Icon(Icons.Filled.Update, null) },
            selected = currentRoute == StarGazerDestinations.UPDATE_ROUTE,
            onClick = { navigateToUpdate(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
private fun StarGazerHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), // Adjust padding as needed
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Replace "logo_placeholder" with your actual logo resource name
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.app_name),
            // Modifier to center the Image and add padding
            modifier = Modifier.padding(8.dp)
        )
    }
}

