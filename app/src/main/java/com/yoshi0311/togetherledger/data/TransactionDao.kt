package com.yoshi0311.togetherledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TransactionDao {
    @Query("""
        SELECT t.id, t.content, t.timeStamp, t.amount, t.assetType, t.isIncome, t.categoryId, 
               COALESCE(c.name, ' ') AS categoryName
        FROM transactions AS t
        LEFT JOIN categories AS c ON t.categoryId = c.id
        ORDER BY t.timeStamp DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionInfo>>

    @Query("""
        SELECT t.id, t.content, t.timeStamp, t.amount, t.assetType, t.isIncome, t.categoryId, 
               COALESCE(c.name, ' ') AS categoryName
        FROM transactions AS t
        LEFT JOIN categories AS c ON t.categoryId = c.id
        WHERE t.id = :id
    """)
    fun getTransaction(id: Int): Flow<TransactionInfo>

    @Query("""
        SELECT t.id, t.content, t.timeStamp, t.amount, t.assetType, t.isIncome, t.categoryId, 
               COALESCE(c.name, ' ') AS categoryName
        FROM transactions AS t
        LEFT JOIN categories AS c ON t.categoryId = c.id
        WHERE t.timeStamp >= :start AND t.timeStamp < :end
        ORDER BY DATE(t.timeStamp) DESC, TIME(t.timeStamp) ASC
    """)
    fun getTransactionsByPeriod(
        start: String, // LocalDateTime, // 나중에 String에서 LocalDateTime으로 바꿀 것
        end: String, // LocalDateTime,
    ): Flow<List<TransactionInfo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}