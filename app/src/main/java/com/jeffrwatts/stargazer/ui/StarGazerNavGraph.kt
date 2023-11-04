package com.jeffrwatts.stargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.jeffrwatts.stargazer.data.AppContainer
import com.jeffrwatts.stargazer.ui.info.InfoScreen
import com.jeffrwatts.stargazer.ui.polar.PolarAlignScreen
import com.jeffrwatts.stargazer.ui.sights.SightsScreen

@Composable
fun StarGazerNavGraph(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit = {},
    startDestination: String = StarGazerDestinations.POLAR_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(StarGazerDestinations.INFO_ROUTE) {
            InfoScreen(openDrawer = openDrawer)
        }
        composable(StarGazerDestinations.POLAR_ROUTE) {
            PolarAlignScreen(openDrawer = openDrawer)
        }
        composable(route = StarGazerDestinations.SIGHTS_ROUTE) {
            SightsScreen(openDrawer = openDrawer)
        }
    }
}
