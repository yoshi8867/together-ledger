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

    // FOR TEST
//    val dailyUiState: StateFlow<DailyUiState> =
//        trasactionsRepository.getTransactionByPeriodStream("2026-02-01 00:00:00", "2026-02-05 00:00:00").map { DailyUiState(it) }
//            .stateIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
//                initialValue = DailyUiState()
//            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class DailyUiState(
    val transactionList: List<Transaction> = listOf(),
    val selectedYear: Int = 0, // 현재 날짜의 연+월을 set 하는 건 어느 타이밍에 어느 부분에서 하는 거지?
    val selectedMonth: Int = 0,
)