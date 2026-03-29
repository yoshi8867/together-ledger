package com.yoshi0311.togetherledger.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryId: Int? = null,
    val content: String = "",
    val timeStamp: String = "",
    val amount: Int = 0,
    val assetType: String = "",
    val isIncome: Boolean = false,
    val serverId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncedAt: Long? = null,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
)

data class TransactionInfo(
    val id: Int,
    val content: String,
    val timeStamp: String,
    val amount: Int,
    val assetType: String,
    val isIncome: Boolean,
    val categoryId: Int? = null,
    val categoryName: String?,
)