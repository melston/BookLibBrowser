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
import kotlinx.coroutines.withContext
import org.elsoft.bkdb.data.EBookRepository
import org.elsoft.bkdb.data.local.LocalCacheManager
import org.elsoft.bkdb.data.local.Transaction
import org.elsoft.bkdb.data.local.TransactionType
import org.elsoft.bkdb.data.remote.DatabaseManager
import org.elsoft.bkdb.utils.Platform
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ReadFilter { ALL, UNREAD, READ }

class EBookViewModel : ViewModel() {
    // Use a Channel for one-time events
    private val _uiEvents = Channel<String>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    val repository = EBookRepository(
        DatabaseManager(),
        LocalCacheManager(Platform.getCacheDir()))

    ////////////////////////////////////////////////////
    // Categories and filters
    ////////////////////////////////////////////////////

    // UI State: What is the user currently looking for?
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Add a setter for the UI to call
    fun setSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

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

    ////////////////////////////////////////////////////
    // Books for the listing
    ////////////////////////////////////////////////////

    // The backing property (private)
    private val _allBooks = MutableStateFlow<List<EBook>>(emptyList())
    // The public observable state
    val allBooks: StateFlow<List<EBook>> = _allBooks.asStateFlow()

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

    ////////////////////////////////////////////////////
    // UI State Stuff
    ////////////////////////////////////////////////////
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun startEditing(book: EBook) {
        _uiState.value = LibraryUiState.Editing(book)
    }

    fun startDeleteConfirmation(book: EBook) {
        _uiState.value = LibraryUiState.ConfirmDelete(book)
    }

    fun resetUiState() {
        _uiState.value = LibraryUiState.Idle
    }

    ////////////////////////////////////////////////////
    // Stats Stuff
    ////////////////////////////////////////////////////
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private fun formatLastSyncTimestamp(timestamp: Long): String {
        val formattedDate = dateFormatter.format(Instant.ofEpochMilli(timestamp))
        return "Last Sync: $formattedDate"
    }
    private fun formatPendingTransactions(numTransactions: Int): String {
        return "$numTransactions Pending Transactions"
    }

    val _pendingTransactions = MutableStateFlow(
        formatPendingTransactions(repository.getPendingTransactionCount())
    )
    val pendingTransactions: StateFlow<String> = _pendingTransactions.asStateFlow()

    val _lastSyncTime = MutableStateFlow(
        formatLastSyncTimestamp(repository.lastSyncTimeStamp())
    )
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    ////////////////////////////////////////////////////
    // Syncing stuff
    ////////////////////////////////////////////////////
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private fun runWithSyncSignaling(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSyncing.value = true
                block()
            } catch (e: Exception) {
                _uiEvents.send("Sync error: ${e.message}")
            } finally {
                // This is guaranteed to run even if the block crashes
                _isSyncing.value = false
            }
        }
    }

    val isOnline: StateFlow<Boolean> = isSyncing
        .map { repository.isOnline() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

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

    fun updateSyncUI() {
        val timestamp = repository.lastSyncTimeStamp()
        val pendingTransactions = repository.getPendingTransactionCount()
        _lastSyncTime.value = formatLastSyncTimestamp(timestamp)
        _pendingTransactions.value = formatPendingTransactions(pendingTransactions)
    }

    fun refreshBooks() = runWithSyncSignaling {
        // Update the DB with any pending changes.
        repository.syncIfPossible()

        // Now fetch the latest set of book data
        val books = repository.getBooks()
        val categories = repository.getCategories()

        // Update the UI state
        _allBooks.value = books.toList()
        _allCategories.value = categories

        // 3. Update UI (Switch to Main thread for StateFlow updates)
        withContext(Dispatchers.Main) {
            _allBooks.value = books
            _allCategories.value = categories
            updateSyncUI() // Update that timestamp we fixed earlier!
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

    fun performDeletion() {
        val state = _uiState.value
        if (state is LibraryUiState.ConfirmDelete) {
            val book = state.book
            viewModelScope.launch {
                repository.removeBookWithId(book.id)
                    .onSuccess {
                        _uiEvents.send("Deleted duplicate: ${book.filePath}")
                        refreshBooks()
                    }
                    .onFailure { t ->
                        _uiEvents.send("Failed to delete ${book.filePath}: ${t.message}")
                    }
                resetUiState() // Close the dialog
            }
        }
    }

    fun updateBookMetadata(book: EBook, newTitle: String, newAuthor: String, newDesc: String?) {
        viewModelScope.launch {
            // 1. Check Title
            if (newTitle != book.title) {
                logAndExecute(book.id, TransactionType.UPDATE_TITLE, newTitle)
            }

            // 2. Check Author
            if (newAuthor != book.author) {
                logAndExecute(book.id, TransactionType.UPDATE_AUTHOR, newAuthor)
            }

            // 3. Check Description (handling the null safety)
            val currentDesc = book.description ?: ""
            val incomingDesc = newDesc ?: ""
            if (incomingDesc != currentDesc) {
                logAndExecute(book.id, TransactionType.UPDATE_DESCRIPTION, incomingDesc)
            }

            // Refresh to show updates immediately if online
            refreshBooks()
        }
    }

    // Helper to keep the code DRY
    private suspend fun logAndExecute(bookId: Int, type: TransactionType, value: String) {
        repository.handleTransaction(
            Transaction(bookId = bookId, type = type, newValue = value)
        )
    }
}
