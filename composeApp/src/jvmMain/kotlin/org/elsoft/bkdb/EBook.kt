package org.elsoft.bkdb

// Standard Java/Kotlin for Linux process execution
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.elsoft.bkdb.utils.ConfigManager
import org.elsoft.bkdb.utils.DropboxService
import org.elsoft.bkdb.utils.Platform
import java.io.File

data class EBook(
    val id: Int,
    val title: String,
    val author: String,
    val pubID: String?,
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

fun cacheName(path: String): String {
    val fullFileName = path.substringAfterLast("/")
    val baseName = fullFileName.substringBeforeLast(".", "")
    val extension = fullFileName.substringAfterLast(".", "")

    // 1. Shorten only the base name (remove a-z and _)
    val shortenedBase = baseName
        .replace(Regex("[a-z_]"), "")
        .replace(" ", "")

    // 2. Build the unique name: book + hash + shortenedBase + .ext
    // This ensures "Java_Pro_1.epub" and "Java_Pro_2.epub" stay unique
    val hash = path.hashCode().toString().takeLast(6)

    return "BK_${hash}_$shortenedBase.$extension"
}

