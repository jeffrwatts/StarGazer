package com.jeffrwatts.stargazer.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.webviewscreen.WebViewScreen
import com.jeffrwatts.stargazer.ui.altazmtool.AltAzmToolScreen
import com.jeffrwatts.stargazer.ui.celestialobjdetail.CelestialObjDetailScreen
import com.jeffrwatts.stargazer.ui.info.InfoScreen
import com.jeffrwatts.stargazer.ui.skytonight.SkyTonightScreen
import com.jeffrwatts.stargazer.ui.updatescreen.UpdateScreen
import com.jeffrwatts.stargazer.ui.variablestar.VariableStarScreen
import com.jeffrwatts.stargazer.ui.variablestardetail.VariableStarDetailScreen

@Composable
fun StarGazerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit = {},
    startDestination: String = StarGazerDestinations.SKY_TONIGHT_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = StarGazerDestinations.SKY_TONIGHT_ROUTE) {
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            SkyTonightScreen(openDrawer = openDrawer,
                onSightClick = actions.navigateToCelestialObjDetail,
                modifier = modifier)
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
        composable("${StarGazerDestinations.CELESTIAL_OBJ_DETAIL_ROUTE}/{objectId}") { backStackEntry ->
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            val objectId = backStackEntry.arguments?.getString("objectId")?.toIntOrNull()

            // Only navigate to the detail screen if both sightId is not null
            objectId?.let {
                CelestialObjDetailScreen(
                    sightId = it,
                    onNavigateBack = { navController.popBackStack() },
                    onMoreInfo = { url -> actions.navigateToWebViewAdditionalInfo(url) })
            }
        }
        composable("${StarGazerDestinations.VARIABLE_STAR_DETAIL_ROUTE}/{variableStarId}") { backStackEntry ->
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            val variableStarId = backStackEntry.arguments?.getString("variableStarId")?.toIntOrNull()

            // Only navigate to the detail screen if both sightId is not null
            variableStarId?.let {
                VariableStarDetailScreen(
                    variableStarId = it,
                    onNavigateBack = { navController.popBackStack() },
                    onDisplayEphemeris = {url-> actions.navigateToWebViewAdditionalInfo(url)})
            }
        }
        composable("${StarGazerDestinations.WEBVIEW_ADDITIONAL_INFO_ROUTE}/{url}") { backStackEntry->
            val url = backStackEntry.arguments?.getString("url")?.let { Uri.decode(it) }

            url?.let {
                WebViewScreen(title = "", url = it , onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
