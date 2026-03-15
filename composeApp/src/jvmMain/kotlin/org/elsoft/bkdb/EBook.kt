package org.elsoft.bkdb

// Standard Java/Kotlin for Linux process execution
import org.elsoft.bkdb.utils.Platform
import org.elsoft.bkdb.utils.ConfigManager
import org.elsoft.bkdb.utils.DropboxService

import java.io.File

data class Category(val id: Int, val name: String)

data class EBook(
    val id: Int,
    val title: String,
    val author: String,
    val pubID: String?,
    val filePath: String,
    val isRead: Boolean,
    val isFavorite: Boolean,
    val category: Int,
    val description: String?,
)

fun openEBook(filePath: String): Result<Unit> {
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

