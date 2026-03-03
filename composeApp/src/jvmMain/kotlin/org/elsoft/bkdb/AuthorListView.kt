package org.elsoft.bkdb

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuthorListView(
    groupedBooks: Map<String, List<EBook>>,
    onToggleRead: (EBook, Boolean) -> Unit,
    onToggleFavorite: (EBook, Boolean) -> Unit,
    onEditDescription: (EBook) -> Unit
) {

    // Track which authors are expanded
    val expandedAuthors = remember { mutableStateMapOf<String, Boolean>() }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val alphabet = ('A'..'Z').toList()
    // Calculate this once whenever groupedBooks changes
    val activeLetters = remember(groupedBooks) {
        groupedBooks.keys
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .toSet()
    }

    val showButton by remember {
        derivedStateOf {
            state.firstVisibleItemIndex > 0
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
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
                                    onToggleFavorite = { newValue -> onToggleFavorite(book, newValue) },
                                    onEditDescription = onEditDescription
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

            // The Floating Action Button
            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { state.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
            )
        }

        VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { letter ->
                val isActive = activeLetters.contains(letter)
                val letterColor = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // Dimmed out
                }
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clickable {
                            // 3. Logic to find the index of the first author starting with this letter
                            val targetIndex = findIndexForLetter(groupedBooks, letter)
                            if (targetIndex != -1) {
                                scope.launch {
                                    state.animateScrollToItem(targetIndex)
                                }
                            }
                        }
                        .padding(vertical = 1.dp),
                    color = letterColor,
                )
            }
        }
    }
}

fun findIndexForLetter(groupedBooks: Map<String, List<EBook>>, letter: Char): Int {
    var currentIndex = 0
    for ((author, _) in groupedBooks) {
        if (author.startsWith(letter, ignoreCase = true)) {
            return currentIndex
        }
        // Increment by 1 for the header + the number of books if expanded
        // NOTE: This gets tricky if authors are collapsed.
        // For a simple jump table, it's easiest to jump to the AUTHOR HEADER.
        currentIndex += 1 // The stickyHeader
        // If you want to jump accurately while expanded, you'd need to check expandedAuthors state here
    }
    return -1
}