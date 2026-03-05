package org.elsoft.bkdb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Initial tab
    var selectedTab by remember { mutableStateOf<LibraryTab>(LibraryTab.ByTitle) }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("My Linux Library") },
                        actions = {
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
                        Text(
                            text = stats,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Optional: A "Refresh" button if you've added new files to Dropbox/DB
                        IconButton(onClick = { vm.refreshBooks() }) {
                            Icon(Icons.Default.Refresh,
                                contentDescription = "Refresh Library",
                                modifier = Modifier.size(18.dp))
                        }
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
}
