package org.elsoft.bkdb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BookListItem(book: EBook) {
    // Define background color based on status
    val backgroundColor = when {
        book.isFavorite && book.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        book.isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val vm = viewModel<EBookViewModel>()
    var isExpanded by remember { mutableStateOf(false) }

    // Animation for the expansion arrow
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp)
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // --- THE COMPACT HEADER (Always Visible) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Expansion Indicator
                Icon(
                    imageVector = Icons.Default.PlayArrow, // Triangle shape
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotationState),
                    tint = MaterialTheme.colorScheme.primary
                )

                // 2. Title & Author
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Status & Quick Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WithTooltip("Toggle is Read") {
                        // Read Toggle
                        IconButton(
                            onClick = { vm.setRead(book, !book.isRead) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (book.isRead) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Read",
                                tint = if (book.isRead) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    WithTooltip("Toggle Favorite") {
                        // Favorite Toggle
                        IconButton(
                            onClick = { vm.setFavorite(book, !book.isFavorite) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (book.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Open Book (Launches Okular)
                WithTooltip("Open") {
                    IconButton(
                        onClick = { vm.openBook(book) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open File",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- THE DETAILS SECTION (Hidden until clicked) ---
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 24.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = 0.5.dp)

                Text(
                    text = book.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = { vm.startEditing(book) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Description")
                }
            }
        }
    }
}
