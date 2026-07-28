package com.pantrix.demo.rorty.compose.ui.characters

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.compose.trackClicks
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.ui.shared.ListRow
import com.pantrix.demo.rorty.compose.ui.shared.PagedListScaffold
import com.pantrix.demo.rorty.compose.ui.shared.statusColor
import org.koin.androidx.compose.koinViewModel

@Composable
fun CharactersScreen(
    onOpenDetail: (Int) -> Unit,
    viewModel: CharactersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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

    PagedListScaffold(
        state = state,
        scrollTrackingName = "characters_list",
        searchLabel = "Search characters",
        onQueryChanged = { viewModel.onIntent(CharactersContract.Intent.QueryChanged(it)) },
        onReachedEnd = { viewModel.onIntent(CharactersContract.Intent.ReachedEnd) },
        key = { it.id },
        header = {
            // The one thing Characters has that the other two lists do not.
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
        },
    ) { character ->
        ListRow(
            title = character.name,
            subtitle = character.summary,
            imageUrl = character.imageUrl,
            // `trackClicks`, not `trackedClick`: a Row owns no click handler, so the modifier form is
            // the right one — it installs the only handler there is. Both gestures go through the
            // same intent, differing only in `via`. Wiring the long press straight to `onOpenDetail`
            // would have been shorter and would have silently dropped `character_opened` on that
            // path; measured.
            modifier = Modifier.trackClicks(
                name = "character_row",
                metadata = mapOf("id" to character.id),
                onLongClick = {
                    viewModel.onIntent(
                        CharactersContract.Intent.Selected(character, via = "long_press"),
                    )
                },
            ) {
                viewModel.onIntent(CharactersContract.Intent.Selected(character, via = "tap"))
            },
        )
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
        onClick = trackedClick(
            name = "characters_status_chip",
            metadata = mapOf("label" to label),
            onClick = onClick,
        ),
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
