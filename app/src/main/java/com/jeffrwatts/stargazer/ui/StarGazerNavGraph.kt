package com.jeffrwatts.stargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.deepskyobjects.DeepSkyObjectsScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.solarsystem.SolarSystemScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar.VariableStarScreen
import com.jeffrwatts.stargazer.ui.deepskydetail.DeepSkyDetailScreen
import com.jeffrwatts.stargazer.ui.starfinder.StarFinderScreen
import com.jeffrwatts.stargazer.ui.info.InfoScreen

@Composable
fun StarGazerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit = {},
    startDestination: String = StarGazerDestinations.DEEP_SKY_OBJECTS_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = StarGazerDestinations.DEEP_SKY_OBJECTS_ROUTE) {
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            DeepSkyObjectsScreen(openDrawer = openDrawer,
                onSightClick = actions.navigateToSightDetail,
                modifier = modifier)
        }
        composable(StarGazerDestinations.SOLAR_SYSTEM_ROUTE) {
            SolarSystemScreen(openDrawer = openDrawer, {}, modifier = modifier)
        }
        composable(StarGazerDestinations.VARIABLE_STAR_ROUTE) {
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            VariableStarScreen(openDrawer = openDrawer,
                onSightClick = actions.navigateToSightDetail,
                modifier = modifier)
        }
        composable(StarGazerDestinations.INFO_ROUTE) {
            InfoScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable(route = StarGazerDestinations.STAR_FINDER_ROUTE) {
            StarFinderScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable("${StarGazerDestinations.SIGHT_DETAIL_ROUTE}/{sightId}") { backStackEntry ->
            val sightId = backStackEntry.arguments?.getString("sightId")?.toIntOrNull()

            // Only navigate to the detail screen if both sightId is not null
            sightId?.let {
                DeepSkyDetailScreen(
                    sightId = it,
                    onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
