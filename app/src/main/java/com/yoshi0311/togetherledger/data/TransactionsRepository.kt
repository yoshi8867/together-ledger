package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

interface TransactionsRepository {
    fun getAllTransactionsStream(): Flow<List<TransactionInfo>>
    fun getTransactionStream(id: Int): Flow<TransactionInfo?>
    fun getTransactionByPeriodStream(start: String, end: String): Flow<List<TransactionInfo>>
    suspend fun findTransaction(timeStamp: String, isIncome: Boolean, amount: Int, content: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
}