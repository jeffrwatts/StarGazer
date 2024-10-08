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
        val currentRoute = navBackStackEntry?.destination?.route ?: StarGazerDestinations.SKY_TONIGHT_ROUTE

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        ModalNavigationDrawer(
            drawerContent = {
                AppDrawer(
                    currentRoute = currentRoute,
                    navigateToSkyTonight = navigationActions.navigateToSkyTonight,
                    navigateToVariableStar = navigationActions.navigateToVariableStar,
                    navigateToStars = navigationActions.navigateToStars,
                    navigateToInfo = navigationActions.navigateToInfo,
                    navigateToAltAzmTool = navigationActions.navigateToAltAzmTool,
                    navigateToUpdate = navigationActions.navigateToUpdate,
                    closeDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            },
            drawerState = drawerState,
            gesturesEnabled = false
        ) {
            StarGazerNavGraph(
                navController = navController,
                openDrawer = { coroutineScope.launch { drawerState.open() } },
            )
        }
    }
}