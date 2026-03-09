package org.elsoft.bkdb

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.elsoft.bkdb.data.EBookRepository
import org.elsoft.bkdb.data.local.LocalCacheManager
import org.elsoft.bkdb.data.remote.DatabaseManager
import org.elsoft.bkdb.utils.Platform

enum class ReadFilter { ALL, UNREAD, READ }

class EBookViewModel : ViewModel() {
    val snackbarHostState = SnackbarHostState()
    val repository = EBookRepository(
        DatabaseManager(),
        LocalCacheManager(Platform.getCacheDir()))
    // UI State: What is the user currently looking for?
    var searchQuery by mutableStateOf("")

    // The backing property (private)
    private val _allBooks = MutableStateFlow<List<EBook>>(emptyList())
    // The public observable state
    val allBooks: StateFlow<List<EBook>> = _allBooks.asStateFlow()

    // The current filter selection
    var readFilter by mutableStateOf(ReadFilter.ALL)

    // State for the editing dialog
    var editingBook by mutableStateOf<EBook?>(null)
        private set // Only the ViewModel can change this directly

    // To allow the UI to display syncing state
    var isSyncing by mutableStateOf(false)

    val syncStatus: String by derivedStateOf {
        val timestamp = repository.lastSyncTimeStamp()
        val pendingCount = repository.getPendingTransactionCount()

        val dateStr = if (timestamp == 0L) "Never" else {
            java.text
                .SimpleDateFormat("MMM dd, yyyy HH:mm")
                .format(java.util.Date(timestamp))
        }

        val pendingStr = if (pendingCount > 0) "\nPending Changes: $pendingCount" else ""

        "Last Home Sync: $dateStr$pendingStr"
    }

    val isOnline: StateFlow<Boolean> = snapshotFlow { isSyncing }
        .map { repository.isOnline() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    /**
     *
     */
    val displayedBooks: StateFlow<List<EBook>> = snapshotFlow { searchQuery }
        .combine(snapshotFlow { readFilter }) { query, filter -> query to filter }
        .combine(_allBooks) { (query, filter), books ->
            books.filter { book ->
                // 1. Apply Text Search
                val matchesQuery = query.isBlank() ||
                        book.title.contains(query, ignoreCase = true) ||
                        book.author.contains(query, ignoreCase = true)

                // 2. Apply Read/Unread Filter
                val matchesFilter = when (filter) {
                    ReadFilter.ALL -> true
                    ReadFilter.UNREAD -> !book.isRead
                    ReadFilter.READ -> book.isRead
                }

                matchesQuery && matchesFilter
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * This is a <code>List&lt;EBook&gt;</code> containing all of the books in the
     * repository after applying any selected filters.
     */
    val filteredBooks: StateFlow<List<EBook>> = snapshotFlow { searchQuery }
        .combine(displayedBooks) { query, books ->
            if (query.isBlank()) books
            else books.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * The books in the repository grouped by author after applying any
     * selected filters.
     *
     * This is a map from <code>(authorName: String)</code> to
     * a <code>List&lt;EBook&gt;</code>.
     *
     */
    val booksByAuthor: StateFlow<Map<String, List<EBook>>> = filteredBooks
        .map { books ->
            books.groupBy { it.author }
                .toSortedMap()
                .mapValues { it.value.sortedBy { it.title } }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Derive stats from the existing flows
    val libraryStats: StateFlow<String> = displayedBooks.combine(_allBooks) { filtered, all ->
        if (filtered.size == all.size) {
            "Total: ${all.size} books"
        } else {
            "Showing ${filtered.size} of ${all.size} books"
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "Loading...")

    init {
        refreshBooks()
    }

    fun refreshBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isSyncing = true

                // Update the DB with any pending changes.
                repository.syncIfPossible()

                // Now fetch the latest set of book data
                val books = repository.getBooks()

                // Update the UI state
                _allBooks.value = books
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Database error: ${e.message}")
            } finally {
                // 5. This ALWAYS runs, even if a crash happens above
                isSyncing = false
            }
        }
    }

    fun openBook(book: EBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = openEBook(book.filePath)

            result.onFailure { error ->
                snackbarHostState.showSnackbar("Error: ${error.message}")
            }
        }
    }

    fun setRead(book: EBook, isRead: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update the Database
            repository.updateReadStatus(book.id, isRead)

            // 2. Update the State (Immutable way)
            _allBooks.update { currentList ->
                currentList.map {
                    if (it.id == book.id) it.copy(isRead = isRead) else it
                }
            }
        }
    }

    fun setFavorite(book: EBook, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update the Database
            repository.updateFavoriteStatus(book.id, isFavorite)

            // 2. Update the State
            _allBooks.update { currentList ->
                currentList.map {
                    if (it.id == book.id) it.copy(isFavorite = isFavorite) else it
                }
            }
        }
    }

    fun startEditing(book: EBook) {
        editingBook = book
    }

    fun stopEditing() {
        editingBook = null
    }

    fun updateDescription(book: EBook, newDesc: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Database Update
                repository.updateDescription(book.id, newDesc)

                // 2. UI State Update
                _allBooks.update { currentList ->
                    currentList.map {
                        if (it.id == book.id) it.copy(description = newDesc) else it
                    }
                }
                // 3. Close the dialog
                stopEditing()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error saving description: ${e.message}")
            }
        }
    }
}
