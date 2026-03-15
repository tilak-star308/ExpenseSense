package com.example.es

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {

    suspend fun getTopSpending(startTime: Long, endTime: Long, limit: Int = 5): List<Transaction> =
        withContext(Dispatchers.IO) {
            transactionDao.getTopSpending(startTime, endTime, limit)
        }

    suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<Transaction> =
        withContext(Dispatchers.IO) {
            transactionDao.getTransactionsInRange(startTime, endTime)
        }

    suspend fun getFirstTransactionTimestamp(): Long? =
        withContext(Dispatchers.IO) {
            transactionDao.getFirstTransactionTimestamp()
        }
}
