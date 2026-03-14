package org.elsoft.bkdb.data

import org.elsoft.bkdb.Category
import org.elsoft.bkdb.EBook
import org.elsoft.bkdb.data.local.LocalDataSource
import org.elsoft.bkdb.data.local.Transaction
import org.elsoft.bkdb.data.local.TransactionType
import org.elsoft.bkdb.data.remote.RemoteDataSource
import org.elsoft.bkdb.utils.DropboxService

class EBookRepository(
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource
) {
    suspend fun isOnline(): Boolean {
        return remoteSource.isAvailable()
    }

//    fun lastSyncTimeStamp(): Long {
//        return localSource.getLastSyncTimestamp()
//    }
//
//    fun getPendingTransactionCount(): Int {
//        return localSource.getPendingTransactionCount()
//    }

    suspend fun getBooks(): List<EBook> {
        return if (remoteSource.isAvailable()) {
            val books = remoteSource.fetchBooks()
            localSource.saveCache(books) // Update local copy
            books
        } else {
            localSource.getCachedBooks() // Use offline copy
        }
    }

    suspend fun getCategories(): List<Category> {
        return if (remoteSource.isAvailable()) {
            val categories = remoteSource.fetchCategories()
            localSource.saveCategoryCache(categories) // Update local copy
            categories
        } else {
            localSource.getCachedCategories() // Use offline copy
            emptyList()
        }
    }

    suspend fun syncIfPossible(): Result<SyncReport> {
        if (!remoteSource.isAvailable()) {
            return Result.failure(Exception("Remote source unavailable"))
        }

        val pending = localSource.getPendingTransactions()
        val failures = mutableListOf<Throwable>()
        var successCount = 0

        pending.forEach { tx ->
            val result: Result<Unit> = when (tx.type) {
                TransactionType.TOGGLE_READ ->
                    remoteSource.updateReadStatus(tx.bookId, tx.newValue.toBoolean())
                TransactionType.TOGGLE_FAVORITE ->
                    remoteSource.updateFavoriteStatus(tx.bookId, tx.newValue.toBoolean())
                TransactionType.UPDATE_DESCRIPTION ->
                    remoteSource.updateDescription(tx.bookId, tx.newValue)
                TransactionType.DELETE_BOOK ->
                    removeBookWithId(tx.bookId)
            }

            result.onSuccess {
                localSource.removeTransaction(tx.id) // Only remove if it actually worked
                successCount++
            }.onFailure { error ->
                failures.add(error)
            }
        }

        // Refresh data after processing transactions
        return try {
            val freshBooks = remoteSource.fetchBooks()
            val categories = remoteSource.fetchCategories()

            localSource.saveCache(freshBooks)
            localSource.saveCategoryCache(categories)
            localSource.updateLastSyncTimestamp()

            if (failures.isEmpty()) {
                Result.success(SyncReport(successCount, 0))
            } else {
                // Partial success: return a custom exception or a specialized Result
                Result.failure(CompositeSyncException(successCount, failures))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    suspend fun removeBookWithId(bookId: Int): Result<Unit> {
        val book = getBooks()
            .find {  it.id == bookId }
            ?: return Result.failure(Exception("Book not found!"))

        return removeBookEntirely(book)
    }

    suspend fun removeBookEntirely(book: EBook): Result<Unit> {
        val bookPath =
            if (book.filePath.startsWith("dropbox:"))
                book.filePath.substringAfter("dropbox:")
            else book.filePath
        if (remoteSource.isAvailable()) {
            return try {
                println("Repository: Removing DropBox entry for book with id: ${book.id}")
                // Remote Action: Delete from Dropbox, then (if successful)
                // delete from the database
                // 1. Physical Delete
                val dropboxResult = DropboxService.delete(bookPath)

                if (dropboxResult.isFailure) {
                    val error = dropboxResult.exceptionOrNull()
                    val errorMsg = error?.message ?: "Unknown error"

                    if (errorMsg.contains("not_found")) {
                        println("Repository: File already gone from Dropbox.")
                    } else {
                        // Log the error (like Auth Failure) but DON'T return.
                        println("Repository: Dropbox error ($errorMsg), but proceeding to DB cleanup.")
                    }
                } else {
                    println("Repository: Successfully removed Dropbox file.")
                }

                // 2. Database Delete
                println("Repository: Attempting DB delete for ID: ${book.id}")
                val delResult =  remoteSource.delete(book.id)

                return if (delResult.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(delResult.exceptionOrNull()
                        ?: Exception("Dropbox delete failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            println("Repository: Removing physical book with id: ${book.id}")
            localSource.logTransaction(
                Transaction(
                    bookId = book.id,
                    type = TransactionType.DELETE_BOOK,
                    newValue = ""
                )
            )
            return Result.success(Unit)
        }
    }
}
