package org.elsoft.bkdb

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
        ScrollToTopButton(
            visible = showButton,
            onClick = { scope.launch { state.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
        )
    }
}