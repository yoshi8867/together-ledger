package com.yoshi0311.togetherledger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yoshi0311.togetherledger.ui.menu.DailyDestination
import com.yoshi0311.togetherledger.ui.menu.DailyScreen
import com.yoshi0311.togetherledger.ui.menu.MonthlyDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetailsDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetailsScreen
import com.yoshi0311.togetherledger.ui.transaction.TransactionEditDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionEditScreen
import com.yoshi0311.togetherledger.ui.transaction.TransactionEntryDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionEntryScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DailyDestination.route,
        modifier = modifier,
    ) {
        composable(route = DailyDestination.route) {
            DailyScreen(
                navigateToTransactionEntry = { navController.navigate(TransactionEntryDestination.route) },
                navigateToTransactionUpdate = {
                    navController.navigate("${TransactionDetailsDestination.route}/${it}")
                },
            )
        }
        composable(
            route = TransactionDetailsDestination.routeWithArgs,
            arguments = listOf(navArgument(TransactionDetailsDestination.transactionIdArg) {
                type = NavType.IntType
            })
        ) {
            TransactionDetailsScreen(
                navigateToEdit = {
                    navController.navigate("${TransactionEditDestination.route}/$it")
                 },
                navigateBack = { navController.navigateUp() },
            )
        }
        composable(
            route = TransactionEditDestination.routeWithArgs,
            arguments = listOf(navArgument(TransactionEditDestination.transactionIdArg) {
                type = NavType.IntType
            })
        ) {
            TransactionEditScreen(
                navigateBack = { navController.popBackStack() },
                onNavigateUp = { navController.navigateUp() },
            )
        }
        composable(route = TransactionEntryDestination.route) {
            TransactionEntryScreen(
                navigateBack = { navController.popBackStack() },
                onNavigateUp = { navController.navigateUp() },
            )
        }
    }
}