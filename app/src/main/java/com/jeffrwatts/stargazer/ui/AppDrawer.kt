package com.jeffrwatts.stargazer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Star
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
    navigateToSights: () -> Unit,
    navigateToSolarSystem: () -> Unit,
    navigateToVariableStar: () -> Unit,
    navigateToInfo: () -> Unit,
    navigateToStarFinder: () -> Unit,
    navigateToRecommended: () -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier) {
        StarGazerHeader(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.photo_planner_title)) },
            icon = { Icon(Icons.Filled.Camera, null) },
            selected = currentRoute == StarGazerDestinations.PHOTO_PLANNER_ROUTE,
            onClick = { navigateToSights(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.solarsystem)) },
            icon = { Icon(Icons.Filled.Adjust, null) },
            selected = currentRoute == StarGazerDestinations.SOLAR_SYSTEM_ROUTE,
            onClick = { navigateToSolarSystem(); closeDrawer() },
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
            label = { Text(stringResource(id = R.string.recommended)) },
            icon = { Icon(Icons.Filled.Recommend, null) },
            selected = currentRoute == StarGazerDestinations.RECOMMENDED_ROUTE,
            onClick = { navigateToRecommended(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.star_finder)) },
            icon = { Icon(Icons.Filled.AddCircleOutline, null) },
            selected = currentRoute == StarGazerDestinations.STAR_FINDER_ROUTE,
            onClick = { navigateToStarFinder(); closeDrawer() },
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

