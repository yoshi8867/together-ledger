package com.yoshi0311.togetherledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String = "",
    val content: String = "",
    val timeStamp: String = "",
    val amount: Int = 0,
    val assetType: String = "",
    val isIncome: Boolean = false,
)