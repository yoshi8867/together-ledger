package com.yoshi0311.togetherledger.ui.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.CategoriesRepository
import com.yoshi0311.togetherledger.data.Category
import com.yoshi0311.togetherledger.data.SyncStatus
import com.yoshi0311.togetherledger.data.TransactionsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionsRepository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
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
            val existing = transactionsRepository.getTransactionById(transactionId) ?: return
            val details = transactionUiState.transactionDetails
            transactionsRepository.updateTransaction(
                existing.copy(
                    categoryId = details.categoryId,
                    content = details.content,
                    timeStamp = details.timeStamp,
                    amount = details.amount.toIntOrNull() ?: 0,
                    assetType = details.assetType,
                    isIncome = details.isIncome,
                    syncStatus = SyncStatus.PENDING,
                    localUpdatedAt = System.currentTimeMillis(),
                )
            )
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