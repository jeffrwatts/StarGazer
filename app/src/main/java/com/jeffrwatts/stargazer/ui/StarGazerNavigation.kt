package com.jeffrwatts.stargazer.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object StarGazerDestinations {
    const val INFO_ROUTE = "info"
    const val POLAR_ROUTE = "polar"
    const val SIGHTS_ROUTE = "sights"
}

/**
 * Models the navigation actions in the app.
 */
class StarGazerNavigationActions(navController: NavHostController) {
    val navigateToInfo: () -> Unit = {
        navController.navigate(StarGazerDestinations.INFO_ROUTE) {
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

    val navigateToPolar: () -> Unit = {
        navController.navigate(StarGazerDestinations.POLAR_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigateToSights: () -> Unit = {
        navController.navigate(StarGazerDestinations.SIGHTS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}
