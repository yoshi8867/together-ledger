package com.yoshi0311.togetherledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: Notification): Long

    // 아직 처리되지 않은 알림만 가져오기
    @Query("SELECT * FROM notifications WHERE isProcessed = 0 ORDER BY timestamp DESC")
    fun getUnprocessedNotifications(): Flow<List<Notification>>

    // 처리 완료 상태로 변경
    @Query("UPDATE notifications SET isProcessed = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: String)
}