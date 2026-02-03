package com.yoshi0311.togetherledger.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DailyViewModel(trasactionsRepository: TransactionsRepository): ViewModel() {

    val dailyUiState: StateFlow<DailyUiState> =
        trasactionsRepository.getAllTransactionsStream().map { DailyUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = DailyUiState()
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class DailyUiState(val transactionList: List<Transaction> = listOf())