package com.yoshi0311.togetherledger.ui.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.TransactionsRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TransactionEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionsRepository: TransactionsRepository,
) : ViewModel() {

    var transactionUiState by mutableStateOf(TransactionUiState())
        private set

    private val transactionId: Int = checkNotNull(savedStateHandle[TransactionDetailsDestination.transactionIdArg])

    init {
        viewModelScope.launch {
            transactionUiState = transactionsRepository.getTransactionStream(transactionId)
                .filterNotNull()
                .first()
                .toTransactionUiState(true)
        }
    }

    suspend fun updateTransaction() {
        if (validateInput(transactionUiState.transactionDetails)) {
            transactionsRepository.updateTransaction(transactionUiState.transactionDetails.toTransaction())
        }
    }

    fun updateUiState(transactionDetails: TransactionDetails) {
        transactionUiState =
            TransactionUiState(
                transactionDetails = transactionDetails,
                isEntryValid = validateInput(transactionDetails),
            )
    }

    private fun validateInput(uiState: TransactionDetails = transactionUiState.transactionDetails): Boolean {
        return with(uiState) {
            content.isNotBlank() && timeStamp.isNotBlank() && amount.isNotBlank()
        }
    }
}