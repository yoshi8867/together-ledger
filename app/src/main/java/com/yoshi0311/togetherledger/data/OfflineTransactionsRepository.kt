package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

class OfflineTransactionsRepository(private val transactionDao: TransactionDao) : TransactionsRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    override fun getTransactionStream(id: Int): Flow<Transaction?> = transactionDao.getTransaction(id)

    override fun getTransactionByPeriodStream(start: String, end: String): Flow<List<Transaction>> = transactionDao.getTransactionsByPeriod(start, end)

    override suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)

    override suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)

    override suspend fun updateTransaction(transaction: Transaction) =  transactionDao.update(transaction)
}