package org.elsoft.bkdb.data.local

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.elsoft.bkdb.Category
import org.elsoft.bkdb.EBook
import java.io.File
import kotlin.collections.forEach

class LocalCacheManager(private val cacheDir: File): LocalDataSource {
    private val cacheFile = File(cacheDir, "library_cache.json")
    private val transactionFile = File(cacheDir, "pending_transactions.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val syncInfoFile = File(cacheDir, "sync_info.json")
    private val categoryCacheFile = File(cacheDir, "category_cache.json")

    override fun saveCategoryCache(categories: List<Category>) {
        try {
            val jsonString = gson.toJson(categories) // Assuming 'gson' is already defined
            categoryCacheFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Failed to save category cache\n" + e.message)
        }
    }

    override fun getCachedCategories(): List<Category> {
        if (!categoryCacheFile.exists()) return emptyList()

        return try {
            val jsonString = categoryCacheFile.readText()
            val itemType = object : TypeToken<List<Category>>() {}.type
            gson.fromJson(jsonString, itemType) ?: emptyList()
        } catch (e: Exception) {
            println("Failed to read category cache\n" + e.message)
            emptyList()
        }
    }

    override fun getCachedBooks(): List<EBook> {
        if (!cacheFile.exists()) return emptyList()

        return try {
            val json = cacheFile.readText()
            val type = object : TypeToken<List<EBook>>() {}.type
            val baseList: List<EBook> = gson.fromJson(json, type) ?: emptyList()

            // Replay travel edits on top of the old snapshot
            applyPendingTransactions(baseList)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun logTransaction(tx: Transaction) {
        val transactions = getPendingTransactions().toMutableList()
        transactions.add(tx)
        writeTransactions(transactions)
    }

    override fun getPendingTransactions(): List<Transaction> {
        if (!transactionFile.exists()) return emptyList()

        return try {
            val json = transactionFile.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
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
                when (tx.type) {
                    TransactionType.TOGGLE_READ ->
                        bookMap[tx.bookId] = book.copy(isRead = tx.newValue.toBoolean())
                    TransactionType.TOGGLE_FAVORITE ->
                        bookMap[tx.bookId] =book.copy(isFavorite = tx.newValue.toBoolean())
                    TransactionType.UPDATE_DESCRIPTION ->
                        bookMap[tx.bookId] =book.copy(description = tx.newValue)
                    TransactionType.DELETE_BOOK ->
                        bookMap.remove(tx.bookId)
                }
            }
        }
        return bookMap.values.toList().sortedBy { it.title }
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

    override fun removeTransaction(id: String) {
        val transactions = getPendingTransactions()
            .filter { it.id != id } // Remove only the tx with the given id value
        writeTransactions(transactions)
    }

    private fun writeTransactions(txs: List<Transaction>) {
        transactionFile.writeText(gson.toJson(txs))
    }
}
