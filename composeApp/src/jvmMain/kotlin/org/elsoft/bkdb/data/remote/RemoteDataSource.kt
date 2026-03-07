package org.elsoft.bkdb.data.remote

import org.elsoft.bkdb.data.local.TransactionType
import org.elsoft.bkdb.EBook

interface RemoteDataSource {
    suspend fun fetchBooks(): List<EBook>
    suspend fun isAvailable(): Boolean
    suspend fun updateReadStatus(bookId: Int, isRead: Boolean)
    suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean)
    suspend fun updateDescription(bookId: Int, description: String?)
}