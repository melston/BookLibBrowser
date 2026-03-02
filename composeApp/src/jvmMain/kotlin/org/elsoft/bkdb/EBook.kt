package org.elsoft.bkdb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Standard Java/Kotlin for Linux process execution
import java.lang.ProcessBuilder

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EBookApp() {
    val books = remember { mutableStateListOf<EBook>() }
    val db = remember { DatabaseManager() }
    val tabs = listOf(LibraryTab.ByTitle, LibraryTab.ByAuthor)

    // Initial tab
    var selectedTab by remember { mutableStateOf<LibraryTab>(LibraryTab.ByTitle) }

    val groupedByAuthor = remember(books.toList()) {
        books.groupBy { it.author }
            .toSortedMap()
            .mapValues { it.value.sortedBy { it.title } }
    }

    LaunchedEffect(Unit) {
        // We move the work to an IO-optimized thread
        withContext(Dispatchers.IO) {
            val data = db.fetchBooks()
            // Switch back to the Main thread to update the UI
            withContext(Dispatchers.Main) {
                books.clear()
                books.addAll(data)
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(title = { Text("My Linux Library") })
                    TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    is LibraryTab.ByTitle -> TitleListView(books)
                    is LibraryTab.ByAuthor -> AuthorListView(groupedByAuthor)
                }
            }
        }
    }
}

@Composable
fun BookListItem(book: EBook) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openInOkular(book.filePath) }
    ) {
        ListItem(
            headlineContent = { Text(book.title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(book.author, style = MaterialTheme.typography.bodyMedium) },
            leadingContent = {
                Icon(Icons.Default.Book, contentDescription = null)
            },
            trailingContent = {
                Text("Open", color = MaterialTheme.colorScheme.primary)
            }
        )
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