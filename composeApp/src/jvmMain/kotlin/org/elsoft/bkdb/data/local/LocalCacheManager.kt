package org.elsoft.bkdb.data.local

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.elsoft.bkdb.EBook
import java.io.File

class LocalCacheManager(private val cacheDir: File): LocalDataSource {
    private val cacheFile = File(cacheDir, "library_cache.json")
    private val transactionFile = File(cacheDir, "pending_transactions.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val syncInfoFile = File(cacheDir, "sync_info.json")

    override fun getCachedBooks(): List<EBook> {
        if (!cacheFile.exists()) return emptyList()

        return try {
            val json = cacheFile.readText()
            val type = object : TypeToken<List<EBook>>() {}.type
            val baseList: List<EBook> = gson.fromJson(json, type) ?: emptyList()

            // Replay travel edits on top of the old snapshot
            applyPendingTransactions(baseList)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun logTransaction(tx: Transaction) {
        val transactions = getPendingTransactions().toMutableList()
        transactions.add(tx)
        transactionFile.writeText(gson.toJson(transactions))
    }

    override fun getPendingTransactions(): List<Transaction> {
        if (!transactionFile.exists()) return emptyList()

        return try {
            val json = transactionFile.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun clearTransactions() {
        if (transactionFile.exists()) transactionFile.delete()
    }

    // Persist the full list to the local filesystem.
    override fun saveCache(books: List<EBook>) {
        try {
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val json = gson.toJson(books)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            println("Failed to save local cache: ${e.message}")
        }
    }

    private fun applyPendingTransactions(cachedBooks: List<EBook>): List<EBook> {
        val pending = getPendingTransactions()
        if (pending.isEmpty()) return cachedBooks

        // Create a map for quick lookup
        val bookMap = cachedBooks.associateBy { it.id }.toMutableMap()

        pending.forEach { tx ->
            val book = bookMap[tx.bookId]
            if (book != null) {
                bookMap[tx.bookId] = when (tx.type) {
                    TransactionType.TOGGLE_READ -> book.copy(isRead = tx.newValue.toBoolean())
                    TransactionType.TOGGLE_FAVORITE -> book.copy(isFavorite = tx.newValue.toBoolean())
                    TransactionType.UPDATE_DESCRIPTION -> book.copy(description = tx.newValue)
                }
            }
        }
        return bookMap.values.toList()
    }

    override fun updateLastSyncTimestamp() {
        val now = System.currentTimeMillis()
        syncInfoFile.writeText(now.toString())
    }

    override fun getLastSyncTimestamp(): Long {
        return if (syncInfoFile.exists()) syncInfoFile.readText().toLongOrNull() ?: 0L else 0L
    }

    // Helper for the UI to get the count of "outbox" items
    override fun getPendingTransactionCount(): Int {
        return getPendingTransactions().size
    }

}
