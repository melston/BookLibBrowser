package org.elsoft.bkdb.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.elsoft.bkdb.Category
import org.elsoft.bkdb.EBook
import org.elsoft.bkdb.utils.ConfigManager
import java.sql.DriverManager

private object Consts {
    const val EBOOK_TBL = "books"
    const val CATEGORY_TBL = "categories"
}

class DatabaseManager : RemoteDataSource {
    private val url = ConfigManager.get(ConfigManager.db_url)
    private val user = ConfigManager.get(ConfigManager.db_user)
    private val pass = ConfigManager.get(ConfigManager.db_password)

    companion object {
        fun testConnection(url: String, user: String, pass: String): Boolean {
            return try {
                DriverManager.getConnection(url, user, pass).use { it.isValid(2) }
                true
            } catch (_: Exception) {
                false
            }
        }

    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        testConnection(url, user, pass)
    }

    override suspend fun fetchCategories(): List<Category> {
        val catList = mutableListOf<Category>()
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                val query = "SELECT * FROM ${Consts.CATEGORY_TBL} ORDER BY name ASC"
                val statement = conn.createStatement()
                val resultSet = statement.executeQuery(query)

                while (resultSet.next()) {
                    catList.add(
                        Category(
                            id = resultSet.getInt("id"),
                            name = resultSet.getString("name"),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return catList
    }

    override suspend fun fetchBooks(): List<EBook> {
        val bookList = mutableListOf<EBook>()
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                val query = "SELECT * FROM ${Consts.EBOOK_TBL} ORDER BY title ASC"
                val statement = conn.createStatement()
                val resultSet = statement.executeQuery(query)

                while (resultSet.next()) {
                    val rawDescription = resultSet.getString("description")
                    // Check if the DB value was actually NULL
                    val finalDescription = if (resultSet.wasNull()) {
                        null // Or use a default like "No description available"
                    } else {
                        rawDescription
                    }
                    // Change the variable type to String? (with the question mark)
                    val pubID: String? = resultSet.getString("publisher_id")
                    bookList.add(
                        EBook(
                            id = resultSet.getInt("id"),
                            title = resultSet.getString("title"),
                            author = resultSet.getString("author"),
                            pubID = pubID,
                            filePath = resultSet.getString("file_path"),
                            isRead = resultSet.getBoolean("is_read"),
                            isFavorite = resultSet.getBoolean("is_favorite"),
                            category = resultSet.getInt("category_id"),
                            description = finalDescription,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bookList
    }

    override suspend fun updateReadStatus(bookId: Int, isRead: Boolean): Result<Unit> {
        return executeUpdate("UPDATE ${Consts.EBOOK_TBL} SET is_read = ? WHERE id = ?", isRead, bookId)
    }

    override suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean): Result<Unit> {
        return executeUpdate("UPDATE ${Consts.EBOOK_TBL} SET is_favorite = ? WHERE id = ?", isFavorite, bookId)
    }

    override suspend fun updateDescription(bookId: Int, description: String?): Result<Unit> {
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                val sql = "UPDATE ${Consts.EBOOK_TBL} SET description = ? WHERE id = ?"
                conn.prepareStatement(sql).use { pstmt ->
                    // MySQL handles nulls correctly if passed via setString
                    pstmt.setString(1, description)
                    pstmt.setInt(2, bookId)
                    pstmt.executeUpdate()
                }

                return Result.success(Unit)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }


    override suspend fun delete(bookId: Int): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            println("DatabaseManager: deleting book with id: $bookId")
            DriverManager.getConnection(url, user, pass).use { conn ->
                val sql = "DELETE FROM ${Consts.EBOOK_TBL} WHERE id = ?"
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, bookId)
                    pstmt.executeUpdate()
                }
            }
        }
    }

    private fun executeUpdate(sql: String, value: Boolean, id: Int): Result<Unit> {
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setBoolean(1, value)
                    pstmt.setInt(2, id)
                    pstmt.executeUpdate()
                }

                return Result.success(Unit)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}