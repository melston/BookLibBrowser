package org.elsoft.bkdb

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    data class Editing(val book: EBook) : LibraryUiState()
    data class ConfirmDelete(val book: EBook) : LibraryUiState()
    data class DuplicateResults(
        val groups: List<List<EBook>>,
        val deletedIds: Set<Int> = emptySet()
    ) : LibraryUiState()
}
