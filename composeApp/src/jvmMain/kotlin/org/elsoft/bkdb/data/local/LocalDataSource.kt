package org.elsoft.bkdb.data.local

import org.elsoft.bkdb.Category
import org.elsoft.bkdb.EBook

interface LocalDataSource {
    fun getCachedCategories(): List<Category>
    fun saveCategoryCache(categories: List<Category>)
    fun getCachedBooks(): List<EBook>
    fun saveCache(books: List<EBook>)
    fun logTransaction(tx: Transaction)
    fun getPendingTransactions(): List<Transaction>
    fun clearTransactions()
    fun updateLastSyncTimestamp()
    fun getLastSyncTimestamp(): Long
    fun getPendingTransactionCount(): Int
}
