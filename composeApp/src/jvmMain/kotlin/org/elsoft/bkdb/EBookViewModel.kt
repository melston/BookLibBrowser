package org.elsoft.bkdb

import androidx.compose.material3.SnackbarHostState
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

enum class ReadFilter { ALL, UNREAD, READ }

class EBookViewModel : ViewModel() {
    val snackbarHostState = SnackbarHostState()
    val db = DatabaseManager()
    // UI State: What is the user currently looking for?
    var searchQuery by mutableStateOf("")

    // The backing property (private)
    private val _allBooks = MutableStateFlow<List<EBook>>(emptyList())
    // The public observable state
    val allBooks: StateFlow<List<EBook>> = _allBooks.asStateFlow()

    // The current filter selection
    var readFilter by mutableStateOf(ReadFilter.ALL)

    // The logic that drives the UI
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

    // 1. The Filtered List (replaces filteredBooks)
    val filteredBooks: StateFlow<List<EBook>> = snapshotFlow { searchQuery }
        .combine(displayedBooks) { query, books ->
            if (query.isBlank()) books
            else books.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. The Grouped Data (replaces groupedByAuthor)
    // We derive this directly from the filteredBooks flow
    val booksByAuthor: StateFlow<Map<String, List<EBook>>> = filteredBooks
        .map { books ->
            books.groupBy { it.author }
                .toSortedMap()
                .mapValues { it.value.sortedBy { it.title } }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        refreshBooks()
    }

    fun refreshBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Assuming DatabaseManager returns the list from MySQL
                val books = db.fetchBooks()
                _allBooks.value = books
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Database error: ${e.message}")
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
            db.updateReadStatus(book.id, isRead)

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
            db.updateFavoriteStatus(book.id, isFavorite)

            // 2. Update the State
            _allBooks.update { currentList ->
                currentList.map {
                    if (it.id == book.id) it.copy(isFavorite = isFavorite) else it
                }
            }
        }
    }

    // State for the editing dialog
    var editingBook by mutableStateOf<EBook?>(null)
        private set // Only the ViewModel can change this directly

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
                db.updateDescription(book.id, newDesc)

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

    // Derive stats from the existing flows
    val libraryStats: StateFlow<String> = displayedBooks.combine(_allBooks) { filtered, all ->
        if (filtered.size == all.size) {
            "Total: ${all.size} books"
        } else {
            "Showing ${filtered.size} of ${all.size} books"
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "Loading...")
}
