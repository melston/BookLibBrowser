package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import org.elsoft.bkdb.LibraryUiState
import org.elsoft.bkdb.LocalViewModel
import org.elsoft.bkdb.ReadFilter

sealed class LibraryTab(val title: String) {
    data object ByTitle : LibraryTab("By Title")
    data object ByAuthor : LibraryTab("By Author")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val vm = LocalViewModel.current

    val tabs = listOf(LibraryTab.ByTitle, LibraryTab.ByAuthor)
    val stats by vm.libraryStats.collectAsState()
    val online by vm.isOnline.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val syncing by vm.isSyncing.collectAsState() // Correctly observing the state

    // Initial tab
    var selectedTab by remember { mutableStateOf<LibraryTab>(LibraryTab.ByTitle) }

    // This "LaunchedEffect" listens to the ViewModel's events from the uiEvents channel
    LaunchedEffect(Unit) {
        vm.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("EBook Library Browser") },
                    actions = {
                        val filterTooltip = when(vm.readFilter.value) {
                            ReadFilter.ALL -> "Currently showing All (Click for Unread)"
                            ReadFilter.UNREAD -> "Currently showing Unread (Click for Read)"
                            ReadFilter.READ -> "Currently showing Read (Click for All)"
                        }
                        WithTooltip(filterTooltip) {
                            // A simple icon button to cycle through filters
                            IconButton(onClick = {
                                vm.setReadFilter(
                                    when (vm.readFilter.value) {
                                        ReadFilter.ALL -> ReadFilter.UNREAD
                                        ReadFilter.UNREAD -> ReadFilter.READ
                                        ReadFilter.READ -> ReadFilter.ALL
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = when (vm.readFilter.value) {
                                        ReadFilter.ALL -> Icons.AutoMirrored.Filled.List
                                        ReadFilter.UNREAD -> Icons.Default.RadioButtonUnchecked
                                        ReadFilter.READ -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = "Filter by read status",
                                    tint = if (vm.readFilter.value == ReadFilter.ALL) LocalContentColor.current
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Info Button with Tooltip
                        ActionIconButton(
                            onClick = { showAboutDialog = true },
                            icon = Icons.Default.Info,
                            tooltipText = "About Ebook Library"
                        )
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { vm.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search by title or author...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.setSearchQuery( "" ) }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Dropdown
                CategoryDropdown()

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
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(48.dp) // Slimmer than a standard bottom bar
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (syncing) {
                        // Show spinning indicator while syncing
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), // Keep it small for the status bar
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Syncing with MySQL...",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = stats,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = vm.lastSyncTime.value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = vm.pendingTransactions.value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val onlineTooltip = when(online) {
                        true -> "Online"
                        false -> "Offline"
                    }
                    WithTooltip(onlineTooltip) {
                        Icon(
                            imageVector =
                                if (online) Icons.Default.CloudDone
                                else Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (online) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // A "Refresh" button if you've added new files to Dropbox/DB
                    ActionIconButton(
                        onClick = { vm.refreshBooks() },
                        icon = Icons.Default.Refresh,
                        tooltipText = "Refresh Books"
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val uiState by vm.uiState.collectAsState()
            val filteredBooks by vm.filteredBooks.collectAsState()
            val allBooks by vm.allBooks.collectAsState()

            // 1. Content Layer
            if (allBooks.isNotEmpty() && filteredBooks.isEmpty()) {
                EmptyLibraryState(onReset = { vm.resetAllFilters() })
            } else {
                when (selectedTab) {
                    is LibraryTab.ByTitle -> TitleListView()
                    is LibraryTab.ByAuthor -> AuthorListView()
                }
            }

            // 2. Dialog Layer (The "Switchboard")
            when (val state = uiState) {
                is LibraryUiState.Editing -> {
                    BookEditDialog(
                        book = state.book,
                        onDismiss = { vm.resetUiState() },
                        onSave = { title, author, desc ->
                            vm.updateBookMetadata(state.book, title, author, desc)
                            vm.resetUiState()
                        }
                    )
                }
                is LibraryUiState.ConfirmDelete -> {
                    AlertDialog(
                        onDismissRequest = { vm.resetUiState() },
                        title = { Text("Delete Book?") },
                        text = {
                            Column {
                                Text("This will remove the entry from your library, including DropBox.")
                                Text(
                                    text = "File: ${state.book.filePath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { vm.performDeletion() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { vm.resetUiState() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                is LibraryUiState.DuplicateResults -> {
                    Dialog(
                        onCloseRequest = { vm.resetUiState() },
                        title = "Potential Duplicates Found",
                        state = rememberDialogState(width = 800.dp, height = 600.dp)
                    ) {
                        if (state.groups.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No duplicates found!")
                            }
                        } else {
                            LazyColumn(Modifier.padding(16.dp)) {
                                items(state.groups) { group ->
                                    DuplicateGroupItem(group, onDelete = { vm.startDeleteConfirmation(it) })
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
                LibraryUiState.Idle -> { /* Nothing to show */ }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
