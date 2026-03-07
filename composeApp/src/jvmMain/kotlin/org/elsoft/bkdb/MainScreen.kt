package org.elsoft.bkdb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class LibraryTab(val title: String) {
    data object ByTitle : LibraryTab("By Title")
    data object ByAuthor : LibraryTab("By Author")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val tabs = listOf(LibraryTab.ByTitle, LibraryTab.ByAuthor)
    val vm = viewModel<EBookViewModel>()
    val stats by vm.libraryStats.collectAsState()
    val online by vm.isOnline.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    // Initial tab
    var selectedTab by remember { mutableStateOf<LibraryTab>(LibraryTab.ByTitle) }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("EBook Library Browser") },
                        actions = {
                            val filterTooltip = when(vm.readFilter) {
                                ReadFilter.ALL -> "Currently showing All (Click for Unread)"
                                ReadFilter.UNREAD -> "Currently showing Unread (Click for Read)"
                                ReadFilter.READ -> "Currently showing Read (Click for All)"
                            }
                            WithTooltip(filterTooltip) {
                                // A simple icon button to cycle through filters
                                IconButton(onClick = {
                                    vm.readFilter = when(vm.readFilter) {
                                        ReadFilter.ALL -> ReadFilter.UNREAD
                                        ReadFilter.UNREAD -> ReadFilter.READ
                                        ReadFilter.READ -> ReadFilter.ALL
                                    }
                                }) {
                                    Icon(
                                        imageVector = when(vm.readFilter) {
                                            ReadFilter.ALL -> Icons.AutoMirrored.Filled.List
                                            ReadFilter.UNREAD -> Icons.Default.RadioButtonUnchecked
                                            ReadFilter.READ -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = "Filter by read status",
                                        tint = if (vm.readFilter == ReadFilter.ALL) LocalContentColor.current
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
                        value = vm.searchQuery,
                        onValueChange = { vm.searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Search by title or author...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (vm.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                        if (vm.isSyncing) {
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
                when (selectedTab) {
                    is LibraryTab.ByTitle -> TitleListView()

                    is LibraryTab.ByAuthor -> AuthorListView()
                }

                if (vm.editingBook != null) {
                    DescriptionEditDialog(
                        book = vm.editingBook!!,
                        onDismiss = { vm.stopEditing() },
                        onConfirm = { newDesc ->
                            vm.updateDescription(vm.editingBook!!, newDesc)
                            vm.stopEditing()
                        }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
