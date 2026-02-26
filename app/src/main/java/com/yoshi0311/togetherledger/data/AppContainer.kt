package com.yoshi0311.togetherledger.data

import android.content.Context

interface AppContainer {
    val transactionsRepository: TransactionsRepository
    val categoriesRepository: CategoriesRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val transactionsRepository: TransactionsRepository by lazy {
        OfflineTransactionsRepository(LedgerDatabase.getDatabase(context).transactionDao())
    }
    override val categoriesRepository: CategoriesRepository by lazy {
        OfflineCategoriesRepository(LedgerDatabase.getDatabase(context).categoryDao())
    }

}