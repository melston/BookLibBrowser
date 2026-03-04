package org.elsoft.bkdb

// Standard Java/Kotlin for Linux process execution
import androidx.compose.material3.*
import androidx.compose.runtime.*

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

sealed class LibraryTab(val title: String) {
    data object ByTitle : LibraryTab("By Title")
    data object ByAuthor : LibraryTab("By Author")
}

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

fun openInOkular(filePath: String) {
    try {
        // This launches the Linux process 'okular' with the file path as an argument
        ProcessBuilder("okular", filePath).start()
    } catch (e: Exception) {
        println("Could not launch Okular. Is it installed? Error: ${e.message}")
    }
}