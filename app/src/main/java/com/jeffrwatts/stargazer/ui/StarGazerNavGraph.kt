package com.jeffrwatts.stargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jeffrwatts.stargazer.ui.compass.CompassScreen
import com.jeffrwatts.stargazer.ui.info.InfoScreen
import com.jeffrwatts.stargazer.ui.polar.PolarAlignScreen
import com.jeffrwatts.stargazer.ui.sightdetail.SightDetailScreen
import com.jeffrwatts.stargazer.ui.sights.SightsScreen

@Composable
fun StarGazerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit = {},
    startDestination: String = StarGazerDestinations.INFO_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(StarGazerDestinations.INFO_ROUTE) {
            InfoScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable(StarGazerDestinations.POLAR_ROUTE) {
            PolarAlignScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable(route = StarGazerDestinations.SIGHTS_ROUTE) {
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            SightsScreen(openDrawer = openDrawer,
                onSightClick = actions.navigateToSightDetail,
                modifier = modifier)
        }
        composable(route = StarGazerDestinations.COMPASS_ROUTE) {
            CompassScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable("${StarGazerDestinations.SIGHT_DETAIL_ROUTE}/{sightId}") { backStackEntry ->
            val sightId = backStackEntry.arguments?.getString("sightId")?.toIntOrNull()

            // Only navigate to the detail screen if both sightId is not null
            sightId?.let {
                SightDetailScreen(
                    sightId = it,
                    onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
