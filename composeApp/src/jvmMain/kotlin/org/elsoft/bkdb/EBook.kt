package org.elsoft.bkdb

// Standard Java/Kotlin for Linux process execution
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val db = remember { DatabaseManager() }
    val scope = rememberCoroutineScope()
    val tabs = listOf(LibraryTab.ByTitle, LibraryTab.ByAuthor)

    // Initial tab
    var selectedTab by remember { mutableStateOf<LibraryTab>(LibraryTab.ByTitle) }

    // As this changes, the filtered and grouped state values change as well.
    var searchQuery by remember { mutableStateOf("") }

    // This state value never changes.
    val allBooks = remember { mutableStateListOf<EBook>() }
    // 1. Filter the raw data first
    val filteredBooks = remember(allBooks.toList(), searchQuery) {
        if (searchQuery.isBlank()) allBooks.toList()
        else allBooks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.author.contains(searchQuery, ignoreCase = true)
        }
    }
    // 2. Derive your Tab data from the FILTERED list
    val groupedByAuthor = remember(filteredBooks) {
        filteredBooks.groupBy { it.author }
            .toSortedMap()
            .mapValues { it.value.sortedBy { it.title } }
    }

    LaunchedEffect(Unit) {
        // We move the work to an IO-optimized thread
        withContext(Dispatchers.IO) {
            val data = db.fetchBooks()
            // Switch back to the Main thread to update the UI
            withContext(Dispatchers.Main) {
                allBooks.clear()
                allBooks.addAll(data)
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(title = { Text("My Linux Library") })
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Search by title or author...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        }
                    )
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
                    is LibraryTab.ByTitle -> TitleListView(
                        allBooks,
                        onToggleRead = { book: EBook, newValue: Boolean ->
                            val index = allBooks.indexOfFirst { it.id == book.id }
                            if (index != -1) {
                                allBooks[index] = book.copy(isRead = newValue)
                            }

                            scope.launch(Dispatchers.IO) {
                                db.updateReadStatus(book.id, newValue)
                            }
                        },
                        onToggleFavorite = { book: EBook, newValue: Boolean ->
                            val index = allBooks.indexOfFirst { it.id == book.id }
                            if (index != -1) {
                                allBooks[index] = book.copy(isFavorite = newValue)
                            }

                            scope.launch(Dispatchers.IO) {
                                db.updateFavoriteStatus(book.id, newValue)
                            }
                        }
                    )
                    is LibraryTab.ByAuthor -> AuthorListView(
                        groupedByAuthor,
                        onToggleRead = { book: EBook, newValue: Boolean ->
                            val index = allBooks.indexOfFirst { it.id == book.id }
                            if (index != -1) {
                                allBooks[index] = book.copy(isRead = newValue)
                            }

                            scope.launch(Dispatchers.IO) {
                                db.updateReadStatus(book.id, newValue)
                            }
                        },
                        onToggleFavorite = { book: EBook, newValue: Boolean ->
                            val index = allBooks.indexOfFirst { it.id == book.id }
                            if (index != -1) {
                                allBooks[index] = book.copy(isFavorite = newValue)
                            }

                            scope.launch(Dispatchers.IO) {
                                db.updateFavoriteStatus(book.id, newValue)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookListItem(
        book: EBook,
        onToggleRead: (Boolean) -> Unit,
        onToggleFavorite: (Boolean) -> Unit
) {
    // Define background color based on status
    val backgroundColor = when {
        book.isFavorite && book.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        book.isFavorite -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        book.isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Surface(color = backgroundColor) {
            ListItem(
                // LEADING: The "Read" Status
                leadingContent = {
                    Checkbox(
                        checked = book.isRead,
                        onCheckedChange = { onToggleRead(it) }
                    )
                },

                // HEADLINE: Title
                headlineContent = { Text(book.title) },

                // SUPPORTING: Author and persistent description
                supportingContent = {
                    Column {
                        Text(book.author, style = MaterialTheme.typography.bodyMedium)
                        if (!book.description.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = book.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },

                // TRAILING: Favorite toggle and Open action
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Favorite Toggle (using an IconToggleButton for a cleaner look)
                        IconToggleButton(
                            checked = book.isFavorite,
                            onCheckedChange = { onToggleFavorite(it) }
                        ) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (book.isFavorite) Color.Green else LocalContentColor.current
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))

                        Text(
                            text = "OPEN",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { openInOkular(book.filePath) }
                                .padding(8.dp)
                        )
                    }
                }
            )
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