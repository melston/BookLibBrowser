package org.elsoft.bkdb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BookListItem(book: EBook) {
    // Define background color based on status
    val backgroundColor = when {
        book.isFavorite && book.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        book.isFavorite -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        book.isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val vm = viewModel<EBookViewModel>()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Surface(color = backgroundColor) {
            ListItem(
                // LEADING: The "Read" Status
                leadingContent = {
                    WithTooltip("Toggle is Read") {
                        Checkbox(
                            checked = book.isRead,
                            onCheckedChange = { vm.setRead(book, it) }
                        )
                    }
                },

                // HEADLINE: Title
                headlineContent = { Text(book.title) },

                // SUPPORTING: Author and persistent description
                supportingContent = {
                    Column {
                        Text(book.author, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (book.description.isNullOrBlank()) ""
                                       else book.description,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = if (book.description.isNullOrBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ActionIconButton(onClick = { vm.startEditing(book) },
                                icon = Icons.Default.Edit,
                                tooltipText = "Edit Description",
                            )
                        }
                    }
                },

                // TRAILING: Favorite toggle and Open action
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WithTooltip("Toggle is Favorite") {
                            // Favorite Toggle (using an IconToggleButton for a cleaner look)
                            IconToggleButton(
                                checked = book.isFavorite,
                                onCheckedChange = { vm.setFavorite(book, it, ) }
                            ) {
                                Icon(
                                    imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (book.isFavorite) Color.Blue else LocalContentColor.current
                                )
                            }
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))

                        WithTooltip("Open EBook") {
                            Text(
                                text = "OPEN",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { vm.openBook(book)}
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}