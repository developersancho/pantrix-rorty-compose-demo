package com.pantrix.demo.rorty.compose.ui.characters

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.compose.TrackScroll
import com.pantrix.compose.trackClicks
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.ui.shared.ListRow
import com.pantrix.demo.rorty.compose.ui.shared.LoadingMoreFooter
import com.pantrix.demo.rorty.compose.ui.shared.StateMessage
import com.pantrix.demo.rorty.compose.ui.shared.statusColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import org.koin.androidx.compose.koinViewModel

@Composable
fun CharactersScreen(
    onOpenDetail: (Int) -> Unit,
    viewModel: CharactersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.onIntent(CharactersContract.Intent.Appear) }

    // Effects are one-shot. Collecting them here rather than reading them out of state is what keeps
    // a recomposition from navigating a second time.
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CharactersContract.Effect.OpenDetail -> onOpenDetail(effect.id)
            }
        }
    }

    // Prefetch trigger: five rows from the end, matching the sibling demos so all four apps issue a
    // comparable number of page requests. `derivedStateOf` so this recomputes on scroll without
    // recomposing the whole screen.
    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= listState.layoutInfo.totalItemsCount - 5
        }
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd) viewModel.onIntent(CharactersContract.Intent.ReachedEnd)
    }

    // One `ui_scroll` per settled gesture, not per frame — the SDK watches `isScrollInProgress` and
    // reports the resting `firstVisibleItem`. NOT `TrackScreen`: this screen is a Nav3 destination,
    // and `PantrixScreenNavTracking` already reported it. Adding both would double-count.
    TrackScroll(name = "characters_list", state = listState)

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.onIntent(CharactersContract.Intent.QueryChanged(it)) },
            label = { Text("Search characters") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip("All", state.status == null) {
                viewModel.onIntent(CharactersContract.Intent.StatusSelected(null))
            }
            CharacterStatus.entries.forEach { status ->
                StatusChip(status.display, state.status == status) {
                    viewModel.onIntent(CharactersContract.Intent.StatusSelected(status))
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                state.message?.let { item { StateMessage(it) } }

                items(state.items, key = { it.id }) { character ->
                    ListRow(
                        title = character.name,
                        subtitle = character.summary,
                        imageUrl = character.imageUrl,
                        leading = null,
                        // `trackClicks`, not `trackedClick`: a Row owns no click handler, so the
                        // modifier form is the right one — it installs the only handler there is.
                        // Both gestures go through the same intent, differing only in `via`. Wiring
                        // the long press straight to `onOpenDetail` would have been shorter and
                        // would have silently dropped `character_opened` on that path; measured.
                        modifier = Modifier.trackClicks(
                            name = "character_row",
                            metadata = mapOf("id" to character.id),
                            onLongClick = {
                                viewModel.onIntent(
                                    CharactersContract.Intent.Selected(character, via = "long_press"),
                                )
                            },
                        ) {
                            viewModel.onIntent(
                                CharactersContract.Intent.Selected(character, via = "tap"),
                            )
                        },
                    )
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

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        // `trackedClick`, not `Modifier.trackClick` — this is the other half of the pair the rows
        // show. `FilterChip` already owns a click handler, so the modifier form would install a
        // SECOND clickable on top of it; `trackedClick` wraps the one that is already there.
        //
        // Both layers fire, and neither is redundant: `ui_click` says a chip was pressed, and the
        // view model's `characters_filter_changed` says which filter is now active — a fact the
        // reducer knows and the view does not.
        onClick = trackedClick(name = "characters_status_chip", metadata = mapOf("label" to label), onClick = onClick),
        label = { Text(label) },
        leadingIcon = if (label != "All") {
            {
                val status = CharacterStatus.entries.first { it.display == label }
                Surface(
                    color = statusColor(status),
                    modifier = Modifier.size(10.dp).clip(CircleShape),
                ) {}
            }
        } else {
            null
        },
    )
}
