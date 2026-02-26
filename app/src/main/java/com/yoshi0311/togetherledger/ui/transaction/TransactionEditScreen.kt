package com.yoshi0311.togetherledger.ui.transaction
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import kotlinx.coroutines.launch

object TransactionEditDestination : NavigationDestination {
    override val route = "transaction_edit"
    override val titleRes = R.string.transaction_edit_title
    const val transactionIdArg = "transactionId"
    val routeWithArgs = "$route/{$transactionIdArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val categories by viewModel.categoriesUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            LedgerTopAppBar(
                title = stringResource(TransactionEditDestination.titleRes),
                canNavigateBack = true,
                navigateUp = onNavigateUp,
            )
        },
        modifier = modifier
    ) { innerPadding ->
        TransactionEntryBody(
            transactionUiState = viewModel.transactionUiState,
            onTransactionValueChange = viewModel::updateUiState,
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.updateTransaction()
                    navigateBack()
                }
            },
            categories = categories,
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
                .verticalScroll(rememberScrollState()),
            onAddCategory = { name -> viewModel.addCategory(name, isIncome = false) },
            onDeleteCategory = viewModel::deleteCategory,
            onUpdateCategory = viewModel::updateCategory,
        )
    }
}