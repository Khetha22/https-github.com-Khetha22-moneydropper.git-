package com.moneydropper.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.moneydropper.ui.screens.MoneyDropScreen
import com.moneydropper.ui.screens.MoneyDropHistoryScreen
import com.moneydropper.ui.screens.MoneyDropViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MoneyDropperNavGraph(navController: NavHostController) {
    val viewModel: MoneyDropViewModel = hiltViewModel()
    
    NavHost(navController = navController, startDestination = "drop_form") {
        composable("drop_form") {
            MoneyDropScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate("drop_history") }
            )
        }
        composable("drop_history") {
            MoneyDropHistoryScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
