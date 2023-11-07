package com.jeffrwatts.stargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jeffrwatts.stargazer.data.AppContainer
import com.jeffrwatts.stargazer.ui.info.InfoScreen
import com.jeffrwatts.stargazer.ui.polar.PolarAlignScreen
import com.jeffrwatts.stargazer.ui.sightdetail.SightDetailScreen
import com.jeffrwatts.stargazer.ui.sights.SightsScreen

@Composable
fun StarGazerNavGraph(
    appContainer: AppContainer,
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
        composable("${StarGazerDestinations.SIGHT_DETAIL_ROUTE}/{sightId}") { backStackEntry ->
            // You can retrieve the sightId from the backStackEntry.arguments
            val sightId = backStackEntry.arguments?.getString("sightId")
            // Use sightId to get the details and pass it to the SightDetailScreen
            SightDetailScreen(sightId = sightId!!) { navController.popBackStack() }
        }
    }
}
