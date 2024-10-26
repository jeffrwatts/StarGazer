package com.jeffrwatts.stargazer.ui

import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import java.time.LocalDateTime

object StarGazerDestinations {
    const val SKY_TONIGHT_ROUTE = "skyTonight"
    const val VARIABLE_STAR_ROUTE = "variableStar"
    const val INFO_ROUTE = "info"
    const val ALT_AZM_TOOL_ROUTE = "altazmTool"
    const val UPDATE_ROUTE = "update"
    const val CELESTIAL_OBJ_DETAIL_ROUTE = "celestialObjDetail"
    const val VARIABLE_STAR_DETAIL_ROUTE = "variableStarDetail"
    const val WEBVIEW_ADDITIONAL_INFO_ROUTE = "webViewAdditionalInfo"
    const val FIELD_OF_VIEW_ROUTE = "fieldOfView"
    const val STARS_ROUTE = "stars"
    const val JUPITER_DETAIL = "jupiterDetail"
}

/**
 * Models the navigation actions in the app.
 */
class StarGazerNavigationActions(navController: NavHostController) {
    val navigateToSkyTonight: () -> Unit = {
        navController.navigate(StarGazerDestinations.SKY_TONIGHT_ROUTE) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    val navigateToVariableStar: () -> Unit = {
        navController.navigate(StarGazerDestinations.VARIABLE_STAR_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToInfo: () -> Unit = {
        navController.navigate(StarGazerDestinations.INFO_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToAltAzmTool: () -> Unit = {
        navController.navigate(StarGazerDestinations.ALT_AZM_TOOL_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToUpdate: () -> Unit = {
        navController.navigate(StarGazerDestinations.UPDATE_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToCelestialObjDetail: (Int, LocalDateTime) -> Unit = { objectId, observationTime ->
        val dateTimeString = observationTime.toString()
        val encodedDateTime = Uri.encode(dateTimeString)
        navController.navigate("${StarGazerDestinations.CELESTIAL_OBJ_DETAIL_ROUTE}/$objectId/$encodedDateTime")
    }

    val navigateToVariableStarDetail: (Int, LocalDateTime) -> Unit = { variableStarId, observationTime ->
        val dateTimeString = observationTime.toString()
        val encodedDateTime = Uri.encode(dateTimeString)
        navController.navigate("${StarGazerDestinations.VARIABLE_STAR_DETAIL_ROUTE}/$variableStarId/$encodedDateTime")
    }

    val navigateToWebViewAdditionalInfo:(String) -> Unit = { url ->
        navController.navigate("${StarGazerDestinations.WEBVIEW_ADDITIONAL_INFO_ROUTE}/$url")
    }

    val navigateToFieldOfView: (Int) -> Unit = { objectId ->
        navController.navigate("${StarGazerDestinations.FIELD_OF_VIEW_ROUTE}/$objectId")
    }

    val navigateToStars: () -> Unit = {
        navController.navigate(StarGazerDestinations.STARS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToJupiterDetail: () -> Unit = {
        navController.navigate(StarGazerDestinations.JUPITER_DETAIL)
    }
}
