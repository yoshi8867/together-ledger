package com.yoshi0311.togetherledger.ui.transaction

import android.R.attr.category
import android.R.attr.name
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.CategoriesRepository
import com.yoshi0311.togetherledger.data.Category
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionInfo
import com.yoshi0311.togetherledger.data.TransactionsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.text.toIntOrNull

class TransactionEntryViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
    ) : ViewModel() {

    var transactionUiState by mutableStateOf(TransactionUiState())
        private set

    fun updateUiState(transactionDetails: TransactionDetails) {
        transactionUiState =
            TransactionUiState(transactionDetails = transactionDetails, isEntryValid = validateInput(transactionDetails))
    }

    suspend fun saveTransaction() {
        if (validateInput()) {
            transactionsRepository.insertTransaction(transactionUiState.transactionDetails.toTransaction())
        }
    }

    private fun validateInput(uiState: TransactionDetails = transactionUiState.transactionDetails): Boolean {
        return with(uiState) {
            content.isNotBlank() && timeStamp.isNotBlank() && amount.isNotBlank()
        }
    }


    val categoriesUiState: StateFlow<List<Category>> =
        categoriesRepository.getAllCategoriesStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addCategory(name: String, isIncome: Boolean) {
        viewModelScope.launch {
            categoriesRepository.insertCategory(Category(name = name, isIncome = isIncome))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            // TODO: 여기서 transactionsRepository를 조회해 해당 카테고리 사용 여부 체크 로직 추가 가능
            categoriesRepository.deleteCategory(category)
        }
    }

    fun updateCategory(category: Category, newName: String) {
        viewModelScope.launch {
            categoriesRepository.updateCategory(category.copy(name = newName))
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
    val categoryId: Int = 0,
    val content: String = "",
    val timeStamp: String = "",
    val amount: String = "",
    val assetType: String = "",
    val isIncome: Boolean = false,
)

fun TransactionDetails.toTransaction(): Transaction = Transaction(
    id = id,
    categoryId = categoryId,
    content = content,
    timeStamp = timeStamp,
    amount = amount.toIntOrNull() ?: 0,
    assetType = assetType,
    isIncome = isIncome,
)

fun TransactionInfo.toTransactionUiState(isEntryValid: Boolean = false): TransactionUiState = TransactionUiState(
    transactionDetails = this.toTransactionDetails(),
    isEntryValid = isEntryValid
)

fun TransactionInfo.toTransactionDetails(): TransactionDetails = TransactionDetails(
    id = id,
    category = category.toString(),
    content = content,
    timeStamp = timeStamp,
    amount = amount.toString(),
    assetType = assetType,
    isIncome = isIncome,
)
