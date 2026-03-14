package org.elsoft.bkdb.data.remote

import org.elsoft.bkdb.Category
import org.elsoft.bkdb.EBook

interface RemoteDataSource {
    suspend fun fetchCategories(): List<Category>
    suspend fun fetchBooks(): List<EBook>
    suspend fun isAvailable(): Boolean
    suspend fun updateReadStatus(bookId: Int, isRead: Boolean): Result<Unit>
    suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean): Result<Unit>
    suspend fun updateDescription(bookId: Int, description: String?): Result<Unit>
    suspend fun delete(bookId: Int): Result<Int>
}
