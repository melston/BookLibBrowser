package org.elsoft.bkdb

import java.sql.DriverManager

class DatabaseManager {
    private val url = "jdbc:mysql://localhost:3306/bookdb"
    private val user = "mark"
    private val password = "ThePigeonRiverFlooded595"

    fun fetchBooks(): List<EBook> {
        val bookList = mutableListOf<EBook>()
        try {
            DriverManager.getConnection(url, user, password).use { conn ->
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

    fun updateReadStatus(bookId: Int, isRead: Boolean) {
        executeUpdate("UPDATE books SET isRead = ? WHERE id = ?", isRead, bookId)
    }

    fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean) {
        executeUpdate("UPDATE books SET isFavorite = ? WHERE id = ?", isFavorite, bookId)
    }

    fun updateDescription(bookId: Int, newDescription: String?) {
        try {
            DriverManager.getConnection(url, user, password).use { conn ->
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
            DriverManager.getConnection(url, user, password).use { conn ->
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
