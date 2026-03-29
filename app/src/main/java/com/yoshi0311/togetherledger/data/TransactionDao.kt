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
        WHERE t.isDeleted = 0
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
        WHERE t.timeStamp >= :start AND t.timeStamp < :end AND t.isDeleted = 0
        ORDER BY DATE(t.timeStamp) DESC, TIME(t.timeStamp) ASC
    """)
    fun getTransactionsByPeriod(
        start: String,
        end: String,
    ): Flow<List<TransactionInfo>>

    @Query("""
        SELECT * FROM transactions
        WHERE timeStamp = :timeStamp
        AND isIncome = :isIncome
        AND (amount = :amount OR content = :content)
        AND isDeleted = 0
        LIMIT 1
    """)
    suspend fun findTransaction(timeStamp: String, isIncome: Boolean, amount: Int, content: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun hardDelete(transaction: Transaction)

    @Query("UPDATE transactions SET isDeleted = 1, syncStatus = 'PENDING', localUpdatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Int, now: Long)

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingTransactions(): List<Transaction>

    @Query("UPDATE transactions SET serverId = :serverId, syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, serverId: String, status: String, syncedAt: Long)

    @Query("SELECT * FROM transactions WHERE serverId = :serverId LIMIT 1")
    suspend fun getTransactionByServerId(serverId: String): Transaction?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)
}
