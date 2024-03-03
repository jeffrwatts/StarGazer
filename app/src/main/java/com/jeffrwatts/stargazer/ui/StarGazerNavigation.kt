package com.jeffrwatts.stargazer.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object StarGazerDestinations {
    const val DEEP_SKY_OBJECTS_ROUTE = "deepSkyObjects"
    const val SOLAR_SYSTEM_ROUTE = "solarSystem"
    const val VARIABLE_STAR_ROUTE = "variableStar"
    const val INFO_ROUTE = "info"
    const val STAR_FINDER_ROUTE = "starFinder"
    const val DEEP_SKY_DETAIL_ROUTE = "deepSkyDetail"
    const val VARIABLE_STAR_DETAIL_ROUTE = "variableStarDetail"
}

/**
 * Models the navigation actions in the app.
 */
class StarGazerNavigationActions(navController: NavHostController) {
    val navigateToDeepSkyObjects: () -> Unit = {
        navController.navigate(StarGazerDestinations.DEEP_SKY_OBJECTS_ROUTE) {
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

    val navigateToSolarSystem: () -> Unit = {
        navController.navigate(StarGazerDestinations.SOLAR_SYSTEM_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
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

    val navigateToStarFinder: () -> Unit = {
        navController.navigate(StarGazerDestinations.STAR_FINDER_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToDeepSkyDetail: (Int) -> Unit = { deepSkyId ->
        navController.navigate("${StarGazerDestinations.DEEP_SKY_DETAIL_ROUTE}/$deepSkyId")
    }

    val navigateToVariableStarDetail: (Int) -> Unit = { variableStarId ->
        navController.navigate("${StarGazerDestinations.VARIABLE_STAR_DETAIL_ROUTE}/$variableStarId")
    }
}
