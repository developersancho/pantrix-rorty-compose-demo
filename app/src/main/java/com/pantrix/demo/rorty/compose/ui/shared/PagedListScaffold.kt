package com.pantrix.demo.rorty.compose.ui.shared

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pantrix.compose.TrackInteractions
import com.pantrix.compose.TrackScroll
import com.pantrix.demo.rorty.compose.core.mvi.PagedListState

/**
 * The list shell the three tabs share: search field, optional header, rows, paging and the one
 * `TrackScroll` line.
 *
 * Kept together because it is where the SDK usage lives, and three copies of it would be three
 * chances to instrument one list differently from the next. What each screen still owns is its
 * **rows** — that is where `trackClicks` carries per-item metadata, which is the part that genuinely
 * differs.
 *
 * NOT `TrackScreen`: every caller is a Navigation 3 destination, and `PantrixScreenNavTracking`
 * already reported it next to the `NavDisplay`. Adding one here would double-count every screen.
 */
@Composable
fun <T> PagedListScaffold(
    state: PagedListState<T>,
    scrollTrackingName: String,
    searchLabel: String,
    onQueryChanged: (String) -> Unit,
    onReachedEnd: () -> Unit,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    row: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()

    // Shared with the search field below, so the SDK observes the SAME source the field already uses
    // rather than installing anything of its own. That is the whole point of the API: it watches, it
    // does not intercept, so a field keeps every handler it had.
    val searchInteractions = remember { MutableInteractionSource() }

    // Prefetch trigger: five rows from the end, matching the sibling demos so all four apps issue a
    // comparable number of page requests. `derivedStateOf` so this recomputes on scroll without
    // recomposing the whole screen.
    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            last >= listState.layoutInfo.totalItemsCount - PREFETCH_ROWS
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) onReachedEnd() }

    // One `ui_scroll` per settled gesture, not per frame — the SDK watches `isScrollInProgress` and
    // reports the resting `firstVisibleItem`.
    TrackScroll(name = scrollTrackingName, state = listState)

    // Hover, focus and drag — the three interactions a click modifier cannot see. Here it answers
    // "how many people open the search box and then type nothing", which no `ui_click` can: a text
    // field gains focus without ever being clicked (keyboard, D-pad, autofill). `ui_hover` needs a
    // pointer, so it stays quiet on a touch-only phone and earns its place on desktop and TV.
    TrackInteractions(name = "${scrollTrackingName}_search", interactionSource = searchInteractions)

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            label = { Text(searchLabel) },
            singleLine = true,
            interactionSource = searchInteractions,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        header?.invoke()

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                state.message?.let { item { StateMessage(it) } }

                items(state.items, key = key) { item ->
                    row(item)
                    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                }

                if (state.isLoadingMore) item { LoadingMoreFooter() }
            }

            if (state.isLoading && state.items.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private const val PREFETCH_ROWS = 5
