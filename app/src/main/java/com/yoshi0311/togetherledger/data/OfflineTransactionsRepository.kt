package com.yoshi0311.togetherledger.data

import android.R.attr.end
import kotlinx.coroutines.flow.Flow

class OfflineTransactionsRepository(private val transactionDao: TransactionDao) : TransactionsRepository {
    override fun getAllTransactionsStream(): Flow<List<TransactionInfo>> = transactionDao.getAllTransactions()

    override fun getTransactionStream(id: Int): Flow<TransactionInfo?> = transactionDao.getTransaction(id)

    override fun getTransactionByPeriodStream(start: String, end: String): Flow<List<TransactionInfo>> = transactionDao.getTransactionsByPeriod(start, end)

    override suspend fun findTransaction(timeStamp: String, isIncome: Boolean, amount: Int, content: String): Transaction? = transactionDao.findTransaction(timeStamp, isIncome, amount, content)

    override suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)

    override suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)

    override suspend fun updateTransaction(transaction: Transaction) =  transactionDao.update(transaction)
}