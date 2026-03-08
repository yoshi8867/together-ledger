package com.yoshi0311.togetherledger.data

import android.content.Context

interface AppContainer {
    val transactionsRepository: TransactionsRepository
    val categoriesRepository: CategoriesRepository
    val notificationsRepository: NotificationsRepository
    val appSettingsRepository: AppSettingsRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val transactionsRepository: TransactionsRepository by lazy {
        OfflineTransactionsRepository(LedgerDatabase.getDatabase(context).transactionDao())
    }
    override val categoriesRepository: CategoriesRepository by lazy {
        OfflineCategoriesRepository(LedgerDatabase.getDatabase(context).categoryDao())
    }
    override val notificationsRepository: NotificationsRepository by lazy {
        OfflineNotificationsRepository(LedgerDatabase.getDatabase(context).notificationDao())
    }
    override val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(context.dataStore) // context.dataStore 확장 함수 사용
    }
}