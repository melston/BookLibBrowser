package org.elsoft.bkdb

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    data class Editing(val book: EBook) : LibraryUiState()
    data class ConfirmDelete(val book: EBook) : LibraryUiState()
    object Syncing : LibraryUiState()
}
