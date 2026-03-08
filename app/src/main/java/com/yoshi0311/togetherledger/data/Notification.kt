package com.yoshi0311.togetherledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    // 알림의 고유값 (키값)
    // 알림 서비스에서 제공하는 key, timestamp, packageName을 조합하여 생성
    @PrimaryKey val id: String,
    val packageName: String,
    val content: String,
    val timestamp: Long,
    val isProcessed: Boolean = false // Transaction으로 변환되었는지 여부
)