package com.yoshi0311.togetherledger.data

import android.content.Context

interface AppContainer {
    val transactionsRepository: TransactionsRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val transactionsRepository: TransactionsRepository by lazy {
        OfflineTransactionsRepository(LedgerDatabase.getDatabase(context).transactionDao())
    }

}