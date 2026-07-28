package com.pantrix.demo.rorty.compose.ui.crosslist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.compose.TrackScroll
import com.pantrix.compose.trackClicks
import com.pantrix.demo.rorty.compose.core.mvi.IdListContract
import com.pantrix.demo.rorty.compose.core.mvi.IdListViewModel
import com.pantrix.demo.rorty.compose.ui.shared.ListRow
import com.pantrix.demo.rorty.compose.ui.shared.StateMessage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CharactersByIdsScreen(
    ids: List<Int>,
    title: String,
    onOpenDetail: (Int) -> Unit,
    viewModel: CharactersByIdsViewModel = koinViewModel { parametersOf(ids) },
) = CrossListScaffold(
    viewModel = viewModel,
    title = title,
    scrollTrackingName = "characters_cross_list",
    onOpenDetail = onOpenDetail,
    id = { it.id },
) { character ->
    ListRow(title = character.name, subtitle = character.summary, imageUrl = character.imageUrl)
}

@Composable
fun EpisodesByIdsScreen(
    ids: List<Int>,
    title: String,
    onOpenDetail: (Int) -> Unit,
    viewModel: EpisodesByIdsViewModel = koinViewModel { parametersOf(ids) },
) = CrossListScaffold(
    viewModel = viewModel,
    title = title,
    scrollTrackingName = "episodes_cross_list",
    onOpenDetail = onOpenDetail,
    id = { it.id },
) { episode ->
    ListRow(
        title = episode.name,
        subtitle = episode.summary,
        leading = { Icon(Icons.Filled.Tv, contentDescription = null) },
    )
}

/**
 * A cross-list: no search, no paging, no next page — the whole id set came out of the parent item.
 *
 * The scroll is still tracked, under its own name. It is a different list from the tab it was
 * reached through, and sharing `episodes_list` between them would make "people scroll the episode
 * list" quietly mean two different lists.
 */
@Composable
private fun <T> CrossListScaffold(
    viewModel: IdListViewModel<T>,
    title: String,
    scrollTrackingName: String,
    onOpenDetail: (Int) -> Unit,
    id: (T) -> Int,
    row: @Composable (T) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.onIntent(IdListContract.Intent.Appear) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is IdListContract.Effect.OpenDetail -> onOpenDetail(effect.id)
            }
        }
    }

    TrackScroll(name = scrollTrackingName, state = listState)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                state.message?.let { item { StateMessage(it) } }

                // Said out loud rather than silently showing a short list: the API caps how many ids
                // one request may carry, so a 51-character episode would otherwise look like a
                // 20-character one and nothing would say which.
                if (state.truncated) {
                    item {
                        StateMessage(
                            "Showing the first ${state.items.size} — the API caps a batch lookup.",
                        )
                    }
                }

                items(state.items, key = id) { item ->
                    Box(
                        modifier = Modifier.trackClicks(
                            name = "cross_list_row",
                            metadata = mapOf("list" to scrollTrackingName, "id" to id(item)),
                        ) { viewModel.onIntent(IdListContract.Intent.Selected(id(item))) },
                    ) { row(item) }
                    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
