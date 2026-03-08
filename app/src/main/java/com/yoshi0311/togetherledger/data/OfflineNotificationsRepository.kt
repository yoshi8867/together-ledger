package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

class OfflineNotificationsRepository(private val notificationDao: NotificationDao) : NotificationsRepository {

    override suspend fun insertNotification(notification: Notification) {
        notificationDao.insert(notification)
    }

    override fun getUnprocessedNotifications(): Flow<List<Notification>> {
        return notificationDao.getUnprocessedNotifications()
    }

    override suspend fun markAsProcessed(id: String) {
        notificationDao.markAsProcessed(id)
    }
}