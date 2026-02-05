package com.yoshi0311.togetherledger.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionsRepository
import com.yoshi0311.togetherledger.ui.transaction.toTransactionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.text.format

class DailyViewModel(private val transactionsRepository: TransactionsRepository): ViewModel() {

    private val today = LocalDate.now()
    private val _selectedDate = MutableStateFlow(Pair(today.year, today.monthValue))

    private val _dailyUiState = MutableStateFlow(DailyUiState())

    val dailyUiState: StateFlow<DailyUiState> =
        combine(
//            transactionsRepository.getAllTransactionsStream(),
            _dailyUiState,
            // transactionsRepository.getTransactionByPeriodStream(startDateTime.format(formatter), endDateTime.format(formatter)),
            _selectedDate,
        ) { uiState, (year, month) ->
            DailyUiState(
                transactionList = uiState.transactionList,
                selectedYear = year,
                selectedMonth = month
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = DailyUiState()
        )

    init {
        selectMonth(today.year, today.monthValue)
    }

    suspend fun updatePeriod(start: String, end: String) {
        transactionsRepository.getTransactionByPeriodStream(start, end)
            .collect { list ->
                _dailyUiState.value = _dailyUiState.value.copy(
                    transactionList = list
                )
            }
    }

    fun selectMonth(year: Int, month: Int) {
        _selectedDate.value = year to month
        val (start, end) = getMonthToMonth(year, month)
        viewModelScope.launch {
            updatePeriod(start, end)
        }
    }

    fun getMonthToMonth(year: Int, month: Int): Pair<String, String> {
        val endYear = if (month == 12) year+1 else year
        val endMonth = if (month == 12) 1 else month+1
        val startDateTime = LocalDateTime.of(year, month, 1, 0, 0, 0)
        val endDateTime = LocalDateTime.of(endYear, endMonth, 1, 0, 0, 0)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return Pair(
            startDateTime.format(formatter),
            endDateTime.format(formatter)
        )
    }

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