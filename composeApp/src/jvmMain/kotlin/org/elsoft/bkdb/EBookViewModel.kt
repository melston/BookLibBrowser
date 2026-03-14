package org.elsoft.bkdb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.elsoft.bkdb.data.EBookRepository
import org.elsoft.bkdb.data.local.LocalCacheManager
import org.elsoft.bkdb.data.remote.DatabaseManager
import org.elsoft.bkdb.utils.Platform

enum class ReadFilter { ALL, UNREAD, READ }

class EBookViewModel : ViewModel() {
    // Use a Channel for one-time events
    private val _uiEvents = Channel<String>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    val repository = EBookRepository(
        DatabaseManager(),
        LocalCacheManager(Platform.getCacheDir()))

    // UI State: What is the user currently looking for?
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Add a setter for the UI to call
    fun setSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    // The backing property (private)
    private val _allBooks = MutableStateFlow<List<EBook>>(emptyList())
    // The public observable state
    val allBooks: StateFlow<List<EBook>> = _allBooks.asStateFlow()

    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    // The current filter selection
    private val _readFilter = MutableStateFlow(ReadFilter.ALL)
    val readFilter: StateFlow<ReadFilter> = _readFilter.asStateFlow()

    // Add a setter for your UI components (like a RadioGroup or Switch)
    fun setReadFilter(filter: ReadFilter) {
        _readFilter.value = filter
    }

    // The current category filter
    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter.asStateFlow()

    fun setCategoryFilter(filter: Category?) {
        _categoryFilter.value = filter
    }

    // State for the editing dialog
    private val _editingBook = MutableStateFlow<EBook?>(null)
    val editingBook: StateFlow<EBook?> = _editingBook.asStateFlow()
    fun setEditingBook(book: EBook?) {
        _editingBook.value = book
    }

    // To allow the UI to display syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    fun setIsSyncing(boolean: Boolean) {
        _isSyncing.value = boolean
    }

    // This may need to change.  If refreshBooks causes the sync state to
    // differ then the timestamp and count need to be Flows from the
    // Repository to trigger a change to the syncStatus.
//    val syncStatus: StateFlow<String> = flow {
//        // 1. Fetch the data once
//        val timestamp = repository.lastSyncTimeStamp()
//        val count = repository.getPendingTransactionCount()
//
//        // 2. Do the formatting logic
//        val dateStr = if (timestamp == 0L) "Never" else {
//            java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
//                .format(java.util.Date(timestamp))
//        }
//        val pendingStr = if (count > 0) "\nPending Changes: $count" else ""
//
//        // 3. Emit the final string
//        emit("Last Home Sync: $dateStr$pendingStr")
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.Lazily, // Only runs when the UI actually looks at it
//        initialValue = "Checking sync..."
//    )

    val isOnline: StateFlow<Boolean> = snapshotFlow { isSyncing }
        .map { repository.isOnline() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    var bookToDelete by mutableStateOf<EBook?>(null)
        private set

    fun confirmDeletion(book: EBook) {
        bookToDelete = book
    }

    fun cancelDeletion() {
        bookToDelete = null
    }

    /**
     * This is a <code>List&lt;EBook&gt;</code> containing all of the books in the
     * repository after applying any selected filters.
     */
    val filteredBooks: StateFlow<List<EBook>> = combine(
        allBooks,           // The master list of books
        _searchQuery,      // The existing title search string
        _categoryFilter,   // The new category filter
        _readFilter        // Whether read or not.
    ) { books: List<EBook>, query: String, category: Category?, readFilter ->
        books.filter { book ->
            // 1. Text Search (Title or Author)
            val matchesQuery = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true)

            // 2. Category Filter
            val matchesCategory = category == null || book.category == category.id

            // 3. Read/Unread Filter
            val matchesRead = when (readFilter) {
                ReadFilter.ALL -> true
                ReadFilter.UNREAD -> !book.isRead
                ReadFilter.READ -> book.isRead
            }

            matchesQuery && matchesCategory && matchesRead
        }
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList())

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
    val libraryStats: StateFlow<String> = filteredBooks.map { books ->
        "Showing ${books.size} of ${allBooks.value.size} books"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        refreshBooks()
    }

    fun resetAllFilters() {
        setSearchQuery("")
        setCategoryFilter(null)
        setReadFilter(ReadFilter.ALL)
    }

    fun refreshBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                setIsSyncing(true)

                // Update the DB with any pending changes.
                repository.syncIfPossible()

                // Now fetch the latest set of book data
                val books = repository.getBooks()
                val categories = repository.getCategories()

                // Update the UI state
                _allBooks.value = books.toList()
                _allCategories.value = categories
            } catch (e: Exception) {
                _uiEvents.send("Database error: ${e.message}")
            } finally {
                // 5. This ALWAYS runs, even if a crash happens above
                setIsSyncing(false)
            }
        }
    }

    fun openBook(book: EBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = openEBook(book.filePath)

            result.onFailure { error ->
                _uiEvents.send("Error opening book: ${error.message}")
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
        setEditingBook(book)
    }

    fun stopEditing() {
        setEditingBook(null)
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
                _uiEvents.send("Error saving description: ${e.message}")
            }
        }
    }

    fun performDeletion() {
        bookToDelete?.let { book ->
            viewModelScope.launch {
                repository.removeBookWithId(book.id)
                    .onSuccess {
                        _uiEvents.send("Deleted duplicate: ${book.filePath}")
                        // Refresh the list to remove the duplicate from view
                        refreshBooks()
                    }
                    .onFailure { t ->
                        _uiEvents.send("Failed to delete ${book.filePath} from Library.\n" +
                        t.message)
                    }
                bookToDelete = null
            }
        }
    }
}
