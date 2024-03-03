package com.jeffrwatts.stargazer.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jeffrwatts.stargazer.ui.theme.StarGazerTheme
import kotlinx.coroutines.launch

@Composable
fun StarGazerApp() {
    StarGazerTheme {
        val navController = rememberNavController()
        val navigationActions = remember(navController) {
            StarGazerNavigationActions(navController)
        }

        val coroutineScope = rememberCoroutineScope()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: StarGazerDestinations.DEEP_SKY_OBJECTS_ROUTE

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        ModalNavigationDrawer(
            drawerContent = {
                AppDrawer(
                    currentRoute = currentRoute,
                    navigateToDeepSkyObjects = navigationActions.navigateToDeepSkyObjects,
                    navigateToSolarSystem = navigationActions.navigateToSolarSystem,
                    navigateToVariableStar = navigationActions.navigateToVariableStar,
                    navigateToInfo = navigationActions.navigateToInfo,
                    navigateToStarFinder = navigationActions.navigateToStarFinder,
                    closeDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            },
            drawerState = drawerState
        ) {
            StarGazerNavGraph(
                navController = navController,
                openDrawer = { coroutineScope.launch { drawerState.open() } },
            )
        }
    }
}