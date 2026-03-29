package com.yoshi0311.togetherledger.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.TransactionsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TransactionDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionsRepository: TransactionsRepository,
) : ViewModel() {

    private val transactionId: Int = checkNotNull(savedStateHandle[TransactionDetailsDestination.transactionIdArg])

    val uiState: StateFlow<TransactionDetailsUiState> =
        transactionsRepository.getTransactionStream(transactionId)
            .filterNotNull()
            .map {
                TransactionDetailsUiState(transactionDetails = it.toTransactionDetails())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = TransactionDetailsUiState()
            )

    suspend fun deleteItem() {
        val transaction = transactionsRepository.getTransactionById(transactionId) ?: return
        transactionsRepository.deleteTransaction(transaction)
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class TransactionDetailsUiState(
    val transactionDetails: TransactionDetails = TransactionDetails(),
)
