package org.elsoft.bkdb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TitleListView(
        books: List<EBook>,
        onToggleRead: (EBook, Boolean) -> Unit,
        onToggleFavorite: (EBook, Boolean) -> Unit
) {
    // 1. Create the shared scroll state
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val showButton by remember {
        derivedStateOf {
            state.firstVisibleItemIndex > 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 2. Pass the state to the LazyColumn
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp) // Leave room for the bar
        ) {
            items(books, key = { it.id }) { book ->
                BookListItem(
                    book,
                    onToggleRead = { newValue -> onToggleRead(book, newValue) },
                    onToggleFavorite = { newValue -> onToggleFavorite(book, newValue) }
                )
            }
        }

        // 3. Add the VerticalScrollbar
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
        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 48.dp) // Offset it from the jump table/scrollbar
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        state.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Jump to top")
            }
        }

    }
}