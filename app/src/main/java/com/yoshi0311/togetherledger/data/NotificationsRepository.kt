package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {
    suspend fun insertNotification(notification: Notification)
    fun getUnprocessedNotifications(): Flow<List<Notification>>
    suspend fun markAsProcessed(id: String)
}