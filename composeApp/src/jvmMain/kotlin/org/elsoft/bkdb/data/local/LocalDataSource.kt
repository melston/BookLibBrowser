package org.elsoft.bkdb.data.local

import org.elsoft.bkdb.Category
import org.elsoft.bkdb.EBook

interface LocalDataSource {
    /**
     * Get a list of currently available categories from the cache file
     */
    fun getCachedCategories(): List<Category>

    /**
     * Save the list of currently available catogories to the cache file
     */
    fun saveCategoryCache(categories: List<Category>)

    /**
     * Get the list of EBooks from the cache file
     */
    fun getCachedBooks(): List<EBook>

    /**
     * Save the list of EBooks to the cache file
     */
    fun saveCache(books: List<EBook>)

    /**
     * Log a transaction to the pending list.
     */
    fun logTransaction(tx: Transaction)

    /**
     * Get the list of currently pending transactions
     */
    fun getPendingTransactions(): List<Transaction>

    /**
     * Clear all pending transactions.
     */
    fun clearTransactions()

    /**
     * Update the timestamp of the latest sync of the cache file(s)
     */
    fun updateLastSyncTimestamp()

    /**
     * Get the time we last sync'd the cache file(s).  This should also be
     * the time of the most recent application of all pending transactions.
     */
    fun getLastSyncTimestamp(): Long

    /**
     * Get the number of transactions that remain to be performed.
     */
    fun getPendingTransactionCount(): Int

    /**
     * Remove the transaction with the specified transaction id value
     */
    fun removeTransaction(id: String)
}
