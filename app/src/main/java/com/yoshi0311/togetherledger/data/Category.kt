package com.yoshi0311.togetherledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val isIncome: Boolean = false,
    val serverId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncedAt: Long? = null,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
)