package com.jeffrwatts.stargazer.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview.FieldOfViewScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.stars.StarsScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar.VariableStarPlannerScreen
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.webviewscreen.WebViewScreen
import com.jeffrwatts.stargazer.ui.altazmtool.AltAzmToolScreen
import com.jeffrwatts.stargazer.ui.celestialobjdetail.CelestialObjDetailScreen
import com.jeffrwatts.stargazer.ui.info.InfoScreen
import com.jeffrwatts.stargazer.ui.skytonight.SkyTonightScreen
import com.jeffrwatts.stargazer.ui.updatescreen.UpdateScreen
import com.jeffrwatts.stargazer.ui.variablestardetail.VariableStarDetailScreen
import java.time.LocalDateTime

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
            VariableStarPlannerScreen(openDrawer = openDrawer,
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
        composable(
            route = "${StarGazerDestinations.CELESTIAL_OBJ_DETAIL_ROUTE}/{objectId}/{observationTime}",
            arguments = listOf(
                navArgument("objectId") { type = NavType.IntType },
                navArgument("observationTime") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            val objectId = backStackEntry.arguments?.getInt("objectId")
            val observationTimeString = backStackEntry.arguments?.getString("observationTime")
            val observationTime = observationTimeString?.let {
                Uri.decode(it)?.let { decodedTime ->
                    LocalDateTime.parse(decodedTime)
                }
            } ?: LocalDateTime.now()

            objectId?.let {
                CelestialObjDetailScreen(
                    sightId = it,
                    observationTime = observationTime,
                    onNavigateBack = { navController.popBackStack() },
                    onMoreInfo = { url -> actions.navigateToWebViewAdditionalInfo(url) },
                    onFieldOfView = { id -> actions.navigateToFieldOfView(id) }
                )
            }
        }

        composable(route = "${StarGazerDestinations.VARIABLE_STAR_DETAIL_ROUTE}/{variableStarId}/{observationTime}",
            arguments = listOf(
                navArgument("variableStarId") { type = NavType.IntType },
                navArgument("observationTime") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val actions = remember(navController) { StarGazerNavigationActions(navController) }
            val variableStarId = backStackEntry.arguments?.getInt("variableStarId")
            val observationTimeString = backStackEntry.arguments?.getString("observationTime")
            val observationTime = observationTimeString?.let {
                Uri.decode(it)?.let { decodedTime ->
                    LocalDateTime.parse(decodedTime)
                }
            } ?: LocalDateTime.now()

            // Only navigate to the detail screen if both sightId is not null
            variableStarId?.let {
                VariableStarDetailScreen(
                    variableStarId = it,
                    observationTime = observationTime,
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
        composable("${StarGazerDestinations.FIELD_OF_VIEW_ROUTE}/{objectId}") { backStackEntry->
            val objectId = backStackEntry.arguments?.getString("objectId")?.toIntOrNull()

            objectId?.let {
                FieldOfViewScreen(sightId = it, onNavigateBack = { navController.popBackStack() })
            }
        }
        composable(StarGazerDestinations.STARS_ROUTE) {
            StarsScreen(openDrawer = openDrawer,
                onSightClick = {},
                modifier = modifier)
        }
    }
}
