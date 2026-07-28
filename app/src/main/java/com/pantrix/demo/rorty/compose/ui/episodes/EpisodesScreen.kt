package com.pantrix.demo.rorty.compose.ui.episodes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.compose.trackClicks
import com.pantrix.demo.rorty.compose.ui.shared.ListRow
import com.pantrix.demo.rorty.compose.ui.shared.PagedListScaffold
import org.koin.androidx.compose.koinViewModel

@Composable
fun EpisodesScreen(
    onOpenDetail: (Int) -> Unit,
    viewModel: EpisodesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(EpisodesContract.Intent.Appear) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EpisodesContract.Effect.OpenDetail -> onOpenDetail(effect.id)
            }
        }
    }

    PagedListScaffold(
        state = state,
        scrollTrackingName = "episodes_list",
        searchLabel = "Search episodes",
        onQueryChanged = { viewModel.onIntent(EpisodesContract.Intent.QueryChanged(it)) },
        onReachedEnd = { viewModel.onIntent(EpisodesContract.Intent.ReachedEnd) },
        key = { it.id },
    ) { episode ->
        ListRow(
            title = episode.name,
            subtitle = episode.summary,
            leading = { Icon(Icons.Filled.Tv, contentDescription = null) },
            modifier = Modifier.trackClicks(
                name = "episode_row",
                metadata = mapOf("id" to episode.id, "code" to episode.code),
                onLongClick = {
                    viewModel.onIntent(EpisodesContract.Intent.Selected(episode, via = "long_press"))
                },
            ) {
                viewModel.onIntent(EpisodesContract.Intent.Selected(episode, via = "tap"))
            },
        )
    }
}
