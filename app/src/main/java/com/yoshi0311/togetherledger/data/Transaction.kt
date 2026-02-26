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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryId: Int = 0, // 구분 값이 필수 필드는 아니니까 일단 이렇게 둬볼까.
    val content: String = "",
    val timeStamp: String = "",
    val amount: Int = 0,
    val assetType: String = "",
    val isIncome: Boolean = false,
)

data class TransactionInfo(
    val id: Int,
    val content: String,
    val timeStamp: String,
    val amount: Int,
    val assetType: String,
    val isIncome: Boolean,
    val categoryId: Int = 0,
    val categoryName: String,
)