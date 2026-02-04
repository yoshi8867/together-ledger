package com.yoshi0311.togetherledger.ui.transaction

import android.R.attr.name
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionsRepository
import kotlin.Int
import kotlin.text.toIntOrNull

class TransactionEntryViewModel(private val transactionsRepository: TransactionsRepository) : ViewModel() {

    var transactionUiState by mutableStateOf(TransactionUiState())
        private set

    fun updateUiState(transactionDetails: TransactionDetails) {
        transactionUiState =
            TransactionUiState(transactionDetails = transactionDetails, isEntryValid = validateInput(transactionDetails))
    }

    suspend fun saveTransaction() {
        if (validateInput()) {
            Log.d("test", "hello")
            transactionsRepository.insertTransaction(transactionUiState.transactionDetails.toTransaction())
        }
    }

    private fun validateInput(uiState: TransactionDetails = transactionUiState.transactionDetails): Boolean {
        return with(uiState) {
            content.isNotBlank() && timeStamp.isNotBlank() && amount.isNotBlank()
        }
    }
}

data class TransactionUiState(
    val transactionDetails: TransactionDetails = TransactionDetails(),
    val isEntryValid: Boolean = false,
)

data class TransactionDetails(
    val id: Int = 0,
    val category: String = "",
    val content: String = "",
    val timeStamp: String = "",
    val amount: String = "",
    val assetType: String = "",
    val isIncome: Boolean = false,
)

fun TransactionDetails.toTransaction(): Transaction = Transaction(
    id = id,
    category = category,
    content = content,
    timeStamp = timeStamp,
    amount = amount.toIntOrNull() ?: 0,
    assetType = assetType,
    isIncome = isIncome,
)

fun Transaction.toTransactionUiState(isEntryValid: Boolean = false): TransactionUiState = TransactionUiState(
    transactionDetails = this.toTransactionDetails(),
    isEntryValid = isEntryValid
)

fun Transaction.toTransactionDetails(): TransactionDetails = TransactionDetails(
    id = id,
    category = category,
    content = content,
    timeStamp = timeStamp,
    amount = amount.toString(),
    assetType = assetType,
    isIncome = isIncome,
)
