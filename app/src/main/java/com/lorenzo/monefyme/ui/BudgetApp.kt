package com.lorenzo.monefyme.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.screen.AddTransactionScreen
import com.lorenzo.monefyme.ui.screen.HomeScreen
import com.lorenzo.monefyme.ui.screen.RestoreScreen
import com.lorenzo.monefyme.ui.screen.TransactionsScreen
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel

private object Routes {
    const val Home = "home"
    const val Restore = "restore"
    const val Transactions = "transactions"
    const val AddExpense = "add-expense"
    const val AddIncome = "add-income"
}

@Composable
fun BudgetApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenTransactions = { navController.navigate(Routes.Transactions) },
                    onOpenRestore = { navController.navigate(Routes.Restore) },
                    onAddTransaction = { transactionType ->
                        if (transactionType == TransactionType.EXPENSE) {
                            navController.navigate(Routes.AddExpense)
                        } else {
                            navController.navigate(Routes.AddIncome)
                        }
                    },
                )
            }
            composable(Routes.Transactions) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenRestore = { navController.navigate(Routes.Restore) },
                )
            }
            composable(Routes.Restore) {
                RestoreScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.AddExpense) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    type = TransactionType.EXPENSE,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.AddIncome) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    type = TransactionType.INCOME,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
