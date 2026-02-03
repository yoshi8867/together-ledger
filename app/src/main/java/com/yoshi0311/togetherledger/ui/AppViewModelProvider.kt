package com.yoshi0311.togetherledger.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yoshi0311.togetherledger.LedgerApplication
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.ui.daily.DailyViewModel
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetailsViewModel
import com.yoshi0311.togetherledger.ui.transaction.TransactionEntryViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for DailyViewModel
        initializer {
            DailyViewModel(ledgerApplication().container.transactionsRepository)
        }
        // Initializer for TransactionEntryViewModel
        initializer {
            TransactionEntryViewModel(ledgerApplication().container.transactionsRepository)
        }
        // Initializer for TransactionDetailsViewModel
        initializer {
            TransactionDetailsViewModel(
                this.createSavedStateHandle(),
                ledgerApplication().container.transactionsRepository
            )
        }
    }
}

fun CreationExtras.ledgerApplication(): LedgerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as LedgerApplication)