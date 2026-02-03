package com.yoshi0311.togetherledger.ui.transaction
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination

object TransactionEditDestination : NavigationDestination {
    override val route = "transaction_edit"
    override val titleRes = R.string.transaction_edit_title
    const val transactionIdArg = "transactionId"
    val routeWithArgs = "$route/{$transactionIdArg}"
}

class TransactionEditScreen {
}