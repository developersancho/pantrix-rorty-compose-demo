package com.pantrix.demo.rorty.compose.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pantrix.compose.TrackScroll
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.core.mvi.DetailContract
import com.pantrix.demo.rorty.compose.core.mvi.DetailViewModel
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.ui.shared.StateMessage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CharacterDetailScreen(
    id: Int,
    onOpenLinked: (List<Int>, String) -> Unit,
    viewModel: CharacterDetailViewModel = koinViewModel { parametersOf(id) },
) = DetailScaffold(viewModel, onOpenLinked, "character_detail") { character: Character ->
    AsyncImage(
        model = character.imageUrl,
        contentDescription = null,
        modifier = Modifier.size(160.dp).clip(CircleShape),
    )
    Text(character.name, style = MaterialTheme.typography.headlineMedium)
    Field("Status", character.status.display)
    Field("Species", character.species)
    Field("Type", character.type)
    Field("Gender", character.gender)
    Field("Origin", character.originName)
    Field("Last known location", character.locationName)
    LinkedButton("Episodes", character.episodeIds.size, viewModel)
}

@Composable
fun LocationDetailScreen(
    id: Int,
    onOpenLinked: (List<Int>, String) -> Unit,
    viewModel: LocationDetailViewModel = koinViewModel { parametersOf(id) },
) = DetailScaffold(viewModel, onOpenLinked, "location_detail") { location: Location ->
    Text(location.name, style = MaterialTheme.typography.headlineMedium)
    Field("Type", location.type)
    Field("Dimension", location.dimension)
    LinkedButton("Residents", location.residentIds.size, viewModel)
}

@Composable
fun EpisodeDetailScreen(
    id: Int,
    onOpenLinked: (List<Int>, String) -> Unit,
    viewModel: EpisodeDetailViewModel = koinViewModel { parametersOf(id) },
) = DetailScaffold(viewModel, onOpenLinked, "episode_detail") { episode: Episode ->
    Text(episode.name, style = MaterialTheme.typography.headlineMedium)
    Field("Code", episode.code)
    Field("Air date", episode.airDate)
    LinkedButton("Characters", episode.characterIds.size, viewModel)
}

/**
 * Loading, failure and content for a detail screen — the parts that are identical whichever of the
 * three it is, so a retry that works on one cannot be missing on another.
 *
 * No `TrackScreen` here either: these are Navigation 3 destinations, already reported by
 * `PantrixScreenNavTracking`.
 */
@Composable
private fun <T> DetailScaffold(
    viewModel: DetailViewModel<T>,
    onOpenLinked: (List<Int>, String) -> Unit,
    scrollTrackingName: String,
    content: @Composable (T) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // The `ScrollState` overload, as opposed to the `LazyListState` one the lists use. It reports
    // `scrollOffset` in pixels rather than `firstVisibleItem`, which is the only sensible answer for
    // a column that has no items to count — and the reason the SDK ships two overloads instead of
    // one. Same rule either way: one event per settled gesture, not per frame.
    TrackScroll(name = scrollTrackingName, state = scrollState)

    LaunchedEffect(Unit) { viewModel.onIntent(DetailContract.Intent.Appear) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DetailContract.Effect.OpenLinked -> onOpenLinked(effect.ids, effect.title)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        state.item?.let { item ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content(item)
            }
        }

        state.message?.let { message ->
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StateMessage(message)
                FilledTonalButton(
                    onClick = trackedClick("detail_retry") {
                        viewModel.onIntent(DetailContract.Intent.Retry)
                    },
                ) { Text("Retry") }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** A labelled value. Blank fields show `—` rather than vanishing, so the layout stays comparable. */
@Composable
private fun Field(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

/**
 * The link out to the cross-list.
 *
 * `trackedClick` because `FilledTonalButton` owns its own `onClick` — `Modifier.trackClick` here
 * would install a second clickable over a button that already has one. The count travels as
 * metadata so "does anyone open a 51-episode list" is answerable without a second event.
 */
@Composable
private fun <T> LinkedButton(label: String, count: Int, viewModel: DetailViewModel<T>) {
    FilledTonalButton(
        enabled = count > 0,
        onClick = trackedClick(
            name = "detail_linked_button",
            metadata = mapOf("label" to label, "count" to count),
        ) { viewModel.onIntent(DetailContract.Intent.OpenLinked) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(if (count > 0) "$label ($count)" else "No $label")
    }
}
