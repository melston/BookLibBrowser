package org.elsoft.bkdb.data.local

import org.elsoft.bkdb.EBook

interface LocalDataSource {
    fun getCachedBooks(): List<EBook>
    fun saveCache(books: List<EBook>)
    fun logTransaction(tx: Transaction)
    fun getPendingTransactions(): List<Transaction>
    fun clearTransactions()
}
