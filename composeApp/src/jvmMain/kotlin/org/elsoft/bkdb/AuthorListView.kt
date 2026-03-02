package org.elsoft.bkdb

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthorListView(
    groupedBooks: Map<String, List<EBook>>,
    onToggleRead: (EBook, Boolean) -> Unit,
    onToggleFavorite: (EBook, Boolean) -> Unit
) {

    // Track which authors are expanded
    val expandedAuthors = remember { mutableStateMapOf<String, Boolean>() }
    val state = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
        ) {
            groupedBooks.forEach { (author, authorBooks) ->
                val isExpanded = expandedAuthors[author] ?: false

                // Author Header
                item(key = author) {
                    Surface(
                        onClick = { expandedAuthors[author] = !isExpanded },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = author,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            // The Badge
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = authorBooks.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                }

                // Indented Book Items (only if expanded)
                if (isExpanded) {
                    items(authorBooks) { book ->
                        Box(modifier = Modifier.padding(start = 32.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)) {
                            BookListItem(
                                book,
                                onToggleRead = { newValue -> onToggleRead(book, newValue) },
                                onToggleFavorite = { newValue -> onToggleFavorite(book, newValue) }
                            )
                        }
                    }
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = state),
            style = defaultScrollbarStyle().copy(
                unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                hoverColor = MaterialTheme.colorScheme.primary,
                thickness = 10.dp,
                shape = RoundedCornerShape(4.dp)
            )
        )
    }
}
