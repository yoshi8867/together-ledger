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
    @Query("SELECT * from transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * from transactions WHERE id = :id")
    fun getTransaction(id: Int): Flow<Transaction>

    @Query("SELECT * from transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY DATE(timestamp) DESC, TIME(timestamp) ASC")
    fun getTransactionsByPeriod(
        start: String, // LocalDateTime, // 나중에 String에서 LocalDateTime으로 바꿀 것
        end: String, // LocalDateTime,
    ): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}