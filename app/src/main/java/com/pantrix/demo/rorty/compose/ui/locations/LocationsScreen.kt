package com.pantrix.demo.rorty.compose.ui.locations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
fun LocationsScreen(
    onOpenDetail: (Int) -> Unit,
    viewModel: LocationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(LocationsContract.Intent.Appear) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LocationsContract.Effect.OpenDetail -> onOpenDetail(effect.id)
            }
        }
    }

    PagedListScaffold(
        state = state,
        scrollTrackingName = "locations_list",
        searchLabel = "Search locations",
        onQueryChanged = { viewModel.onIntent(LocationsContract.Intent.QueryChanged(it)) },
        onReachedEnd = { viewModel.onIntent(LocationsContract.Intent.ReachedEnd) },
        key = { it.id },
    ) { location ->
        ListRow(
            title = location.name,
            subtitle = location.summary,
            // Locations have no image; the leading slot takes an icon instead so the three lists
            // still line up at the same row height.
            leading = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
            modifier = Modifier.trackClicks(
                name = "location_row",
                metadata = mapOf("id" to location.id, "residents" to location.residentIds.size),
                onLongClick = {
                    viewModel.onIntent(LocationsContract.Intent.Selected(location, via = "long_press"))
                },
            ) {
                viewModel.onIntent(LocationsContract.Intent.Selected(location, via = "tap"))
            },
        )
    }
}
