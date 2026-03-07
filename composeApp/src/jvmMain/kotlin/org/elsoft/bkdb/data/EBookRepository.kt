package org.elsoft.bkdb.data

import org.elsoft.bkdb.data.local.LocalCacheManager
import org.elsoft.bkdb.data.local.TransactionType
import org.elsoft.bkdb.data.remote.RemoteDataSource
import org.elsoft.bkdb.EBook
import org.elsoft.bkdb.data.local.Transaction

class EBookRepository(
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalCacheManager
) {
    suspend fun isOnline(): Boolean {
        return remoteSource.isAvailable()
    }

    suspend fun getBooks(): List<EBook> {
        return if (remoteSource.isAvailable()) {
            val books = remoteSource.fetchBooks()
            localSource.saveCache(books) // Update local copy
            books
        } else {
            localSource.getCachedBooks() // Use offline copy
        }
    }

    suspend fun syncIfPossible() {
        if (remoteSource.isAvailable()) {
            val pending = localSource.getPendingTransactions()

            if (pending.isNotEmpty()) {
                println("Replaying ${pending.size} transactions to MySQL...")
                pending.forEach { tx ->
                    when (tx.type) {
                        TransactionType.TOGGLE_READ ->
                            remoteSource.updateReadStatus(tx.bookId, tx.newValue.toBoolean())
                        TransactionType.TOGGLE_FAVORITE ->
                            remoteSource.updateFavoriteStatus(tx.bookId, tx.newValue.toBoolean())
                        TransactionType.UPDATE_DESCRIPTION ->
                            remoteSource.updateDescription(tx.bookId, tx.newValue)
                    }
                }
                localSource.clearTransactions()
            }

            // Now that the remote is updated, refresh the local snapshot
            val freshBooks = remoteSource.fetchBooks()
            localSource.saveCache(freshBooks)
        }
    }

    suspend fun updateReadStatus(bookId: Int, isRead: Boolean) {
        if (remoteSource.isAvailable()) {
            remoteSource.updateReadStatus(bookId, isRead)
            // Optional: Update local cache snapshot too so they stay in sync
        } else {
            localSource.logTransaction(
                Transaction(
                    bookId = bookId,
                    type = TransactionType.TOGGLE_READ,
                    newValue = isRead.toString()))
        }
    }

    suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean) {
        if (remoteSource.isAvailable()) {
            remoteSource.updateFavoriteStatus(bookId, isFavorite)
        } else {
            localSource.logTransaction(
                Transaction(
                    bookId = bookId,
                    type = TransactionType.TOGGLE_FAVORITE,
                    newValue = isFavorite.toString()))
        }
    }

    suspend fun updateDescription(bookId: Int, description: String?) {
        if (remoteSource.isAvailable()) {
            remoteSource.updateDescription(bookId, description)
        } else {
            // Note: Use an empty string or specific token if description is null
            localSource.logTransaction(
                Transaction(bookId = bookId,
                    type = TransactionType.UPDATE_DESCRIPTION,
                    newValue = description ?: ""))
        }
    }
}
