package com.winlator.cmod.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.winlator.cmod.ui.screens.ContainersScreen
import com.winlator.cmod.ui.screens.GamesScreen
import com.winlator.cmod.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Games : Screen("games")
    object Settings : Screen("settings")
    object Containers : Screen("containers")
}

@Composable
fun WinlatorNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Games.route
    ) {
        composable(Screen.Games.route) {
            GamesScreen(navController = navController)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        
        composable(Screen.Containers.route) {
            ContainersScreen(navController = navController)
        }
    }
}
