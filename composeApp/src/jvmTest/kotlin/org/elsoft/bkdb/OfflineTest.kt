package org.elsoft.bkdb

import kotlinx.coroutines.runBlocking
import org.elsoft.bkdb.data.local.Transaction
import kotlin.test.Test


class OfflineTest {
    @Test
    fun `test offline fallback when remote is disconnected`() = runBlocking {
        val mockRemote = object : `RemoteDataSource.kt` {
            override suspend fun isAvailable() = false // Simulate hotel WiFi failure
            override suspend fun updateReadStatus(bookId: Int, isRead: Boolean) {
                TODO("Not yet implemented")
            }
            override suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean) {
                TODO("Not yet implemented")
            }
            override suspend fun updateDescription(bookId: Int, description: String?) {
                TODO("Not yet implemented")
            }
            override suspend fun fetchBooks() = throw Exception("No Network")
        }
        val localCache = object: LocalDataSource {
            override fun getCachedBooks(): List<EBook> {
                return listOf(
                    EBook(1, "abc", "someAuth",
                        "", "", false, false, "")
                )
            }
            override fun saveCache(books: List<EBook>) {
                TODO("Not yet implemented")
            }
            override fun logTransaction(tx: Transaction) {
                TODO("Not yet implemented")
            }
            override fun getPendingTransactions(): List<Transaction> {
                TODO("Not yet implemented")
            }
            override fun clearTransactions() {
                TODO("Not yet implemented")
            }
        }

        val repo = `EBookRepository.kt`(
            mockRemote,
            localCache)
        val books = repo.getBooks()

        // Assert that we got the books from the JSON cache instead of a crash
        assert(books.isNotEmpty())
    }
}