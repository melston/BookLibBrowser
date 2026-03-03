package org.elsoft.bkdb

// Standard Java/Kotlin for Linux process execution
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    // The book currently being edited (null means dialog is closed)
    var editingBook by remember { mutableStateOf<EBook?>(null) }
    val onEditDescription: (EBook) -> Unit = { book ->
        editingBook = book
    }

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

    // Define the shared "Read" logic
    val toggleRead: (EBook, Boolean) -> Unit = { book, newValue ->
        val index = allBooks.indexOfFirst { it.id == book.id }
        if (index != -1) {
            allBooks[index] = allBooks[index].copy(isRead = newValue)
            scope.launch(Dispatchers.IO) {
                db.updateReadStatus(book.id, newValue)
            }
        }
    }

    // Define the shared "Favorite" logic
    val toggleFavorite: (EBook, Boolean) -> Unit = { book, newValue ->
        val index = allBooks.indexOfFirst { it.id == book.id }
        if (index != -1) {
            allBooks[index] = allBooks[index].copy(isFavorite = newValue)
            scope.launch(Dispatchers.IO) {
                db.updateFavoriteStatus(book.id, newValue)
            }
        }
    }

    val updateDescription: (EBook, String?) -> Unit = { book, newDesc ->
        val index = allBooks.indexOfFirst { it.id == book.id }
        if (index != -1) {
            // Optimistic UI update
            allBooks[index] = allBooks[index].copy(description = newDesc)
            scope.launch(Dispatchers.IO) {
                db.updateDescription(book.id, newDesc)
            }
        }
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
                        onToggleRead = toggleRead,
                        onToggleFavorite = toggleFavorite,
                        onEditDescription = onEditDescription
                    )
                    is LibraryTab.ByAuthor -> AuthorListView(
                        groupedByAuthor,
                        onToggleRead = toggleRead,
                        onToggleFavorite = toggleFavorite,
                        onEditDescription = onEditDescription
                    )
                }

                if (editingBook != null) {
                    DescriptionEditDialog(
                        book = editingBook!!,
                        onDismiss = { editingBook = null },
                        onConfirm = { newDesc ->
                            updateDescription(editingBook!!, newDesc)
                            editingBook = null
                        }
                    )
                }
            }
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