package com.yoshi0311.togetherledger.data

import android.R.attr.end
import kotlinx.coroutines.flow.Flow

class OfflineTransactionsRepository(private val transactionDao: TransactionDao) : TransactionsRepository {
    override fun getAllTransactionsStream(): Flow<List<TransactionInfo>> = transactionDao.getAllTransactions()

    override fun getTransactionStream(id: Int): Flow<TransactionInfo?> = transactionDao.getTransaction(id)

    override fun getTransactionByPeriodStream(start: String, end: String): Flow<List<TransactionInfo>> = transactionDao.getTransactionsByPeriod(start, end)

    override suspend fun findTransaction(timeStamp: String, isIncome: Boolean, amount: Int, content: String): Transaction? = transactionDao.findTransaction(timeStamp, isIncome, amount, content)

    override suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)

    override suspend fun deleteTransaction(transaction: Transaction) {
        if (transaction.serverId == null) {
            transactionDao.hardDelete(transaction)
        } else {
            transactionDao.softDelete(transaction.id, System.currentTimeMillis())
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction)

    override suspend fun getTransactionById(id: Int): Transaction? = transactionDao.getTransactionById(id)
    override suspend fun hardDeleteById(id: Int) = transactionDao.deleteById(id)

    override suspend fun getPendingTransactions(): List<Transaction> =
        transactionDao.getPendingTransactions()

    override suspend fun syncUpdateStatus(id: Int, serverId: String, status: String, syncedAt: Long) =
        transactionDao.updateSyncStatus(id, serverId, status, syncedAt)

    override suspend fun getTransactionByServerId(serverId: String): Transaction? =
        transactionDao.getTransactionByServerId(serverId)
}