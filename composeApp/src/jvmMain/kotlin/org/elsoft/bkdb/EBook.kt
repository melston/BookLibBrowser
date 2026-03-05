package org.elsoft.bkdb

// Standard Java/Kotlin for Linux process execution
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.File

data class EBook(
    val id: Int,
    val title: String,
    val author: String,
    val pubID: String,
    val filePath: String, // The absolute path on your Linux system
    val isRead: Boolean,
    val isFavorite: Boolean,
    val description: String?,
)

@Composable
fun EBookApp() {
    var isConfigured by remember { mutableStateOf(ConfigManager.isConfigured()) }

    MaterialTheme {
        if (!isConfigured) {
            SetupScreen(onConfigSaved = { isConfigured = true })
        } else {
            // Your existing Library UI (Tabs, List, etc.)
            MainScreen()
        }
    }
}


suspend fun openEBook(filePath: String): Result<Unit> {
    val customCommand = ConfigManager.get(ConfigManager.viewer_command, "")

    return try {
        val localPath = if (filePath.startsWith("dropbox:")) {
            val tpath = filePath.substringAfter(":")
            val uniqueCacheName = cacheName(tpath)
            DropboxService.download(tpath, uniqueCacheName)
        } else {
            filePath
        }

        val bookFile = File(localPath)
        if (!bookFile.exists()) {
            return Result.failure(Exception("File not found at $localPath"))
        }

        Platform.openFile(bookFile, customCommand)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun cacheName(filename: String): String {
    // Extract the filename from the dropbox path to keep the extension correct
    val modifiedFileName =
        filename
            .substringAfterLast("/")
            .replace(" ", "_")
    // Use a sanitized version of the path or a book ID to keep the cache file unique
    val uniqueCacheName = "book_${filename.hashCode()}_$modifiedFileName"

    return uniqueCacheName
}

