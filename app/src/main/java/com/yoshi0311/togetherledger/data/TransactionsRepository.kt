package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

interface TransactionsRepository {
    fun getAllTransactionsStream(): Flow<List<TransactionInfo>>
    fun getTransactionStream(id: Int): Flow<TransactionInfo?>
    fun getTransactionByPeriodStream(start: String, end: String): Flow<List<TransactionInfo>>
    suspend fun findTransaction(timeStamp: String, isIncome: Boolean, amount: Int, content: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)  // serverId 유무에 따라 소프트/하드 딜리트
    suspend fun updateTransaction(transaction: Transaction)

    suspend fun getTransactionById(id: Int): Transaction?
    suspend fun hardDeleteById(id: Int)
    suspend fun getPendingTransactions(): List<Transaction>
    suspend fun syncUpdateStatus(id: Int, serverId: String, status: String, syncedAt: Long)
    suspend fun getTransactionByServerId(serverId: String): Transaction?
}