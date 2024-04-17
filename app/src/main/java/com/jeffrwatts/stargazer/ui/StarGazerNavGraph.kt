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
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.updatescreen.UpdateScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar.VariableStarScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestardetail.VariableStarDetailScreen
import com.jeffrwatts.stargazer.ui.altazmtool.AltAzmToolScreen
import com.jeffrwatts.stargazer.ui.deepskydetail.DeepSkyDetailScreen
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
                onSightClick = actions.navigateToDeepSkyDetail,
                modifier = modifier)
        }
        composable(StarGazerDestinations.SOLAR_SYSTEM_ROUTE) {
            SolarSystemScreen(openDrawer = openDrawer, {}, modifier = modifier)
        }
        composable(StarGazerDestinations.VARIABLE_STAR_ROUTE) {
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            VariableStarScreen(openDrawer = openDrawer,
                onSightClick = actions.navigateToVariableStarDetail,
                modifier = modifier)
        }
        composable(StarGazerDestinations.INFO_ROUTE) {
            InfoScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable(route = StarGazerDestinations.ALT_AZM_TOOL_ROUTE) {
            AltAzmToolScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable(route = StarGazerDestinations.UPDATE_ROUTE) {
            UpdateScreen(openDrawer = openDrawer, modifier = modifier)
        }
        composable("${StarGazerDestinations.DEEP_SKY_DETAIL_ROUTE}/{deepSkyId}") { backStackEntry ->
            val deepSkyId = backStackEntry.arguments?.getString("deepSkyId")?.toIntOrNull()

            // Only navigate to the detail screen if both sightId is not null
            deepSkyId?.let {
                DeepSkyDetailScreen(
                    sightId = it,
                    onNavigateBack = { navController.popBackStack() })
            }
        }
        composable("${StarGazerDestinations.VARIABLE_STAR_DETAIL_ROUTE}/{variableStarId}") { backStackEntry ->
            val variableStarId = backStackEntry.arguments?.getString("variableStarId")?.toIntOrNull()

            // Only navigate to the detail screen if both sightId is not null
            variableStarId?.let {
                VariableStarDetailScreen(
                    variableStarId = it,
                    onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
