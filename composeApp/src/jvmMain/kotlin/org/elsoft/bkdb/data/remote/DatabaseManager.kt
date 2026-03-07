package org.elsoft.bkdb.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.elsoft.bkdb.data.local.TransactionType
import org.elsoft.bkdb.EBook
import org.elsoft.bkdb.utils.ConfigManager
import java.sql.DriverManager

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

    override suspend fun fetchBooks(): List<EBook> {
        val bookList = mutableListOf<EBook>()
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                val query = "SELECT * FROM books ORDER BY title ASC"
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
                    bookList.add(
                        EBook(
                            id = resultSet.getInt("id"),
                            title = resultSet.getString("title"),
                            author = resultSet.getString("author"),
                            pubID = resultSet.getString("publisher_id"),
                            filePath = resultSet.getString("file_path"),
                            isRead = resultSet.getBoolean("is_read"),
                            isFavorite = resultSet.getBoolean("is_favorite"),
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

    override suspend fun updateReadStatus(bookId: Int, isRead: Boolean) {
        executeUpdate("UPDATE books SET is_read = ? WHERE id = ?", isRead, bookId)
    }

    override suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean) {
        executeUpdate("UPDATE books SET is_favorite = ? WHERE id = ?", isFavorite, bookId)
    }

    override suspend fun updateDescription(bookId: Int, newDescription: String?) {
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                val sql = "UPDATE books SET description = ? WHERE id = ?"
                conn.prepareStatement(sql).use { pstmt ->
                    // MySQL handles nulls correctly if passed via setString
                    pstmt.setString(1, newDescription)
                    pstmt.setInt(2, bookId)
                    pstmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executeUpdate(sql: String, value: Boolean, id: Int) {
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setBoolean(1, value)
                    pstmt.setInt(2, id)
                    pstmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}