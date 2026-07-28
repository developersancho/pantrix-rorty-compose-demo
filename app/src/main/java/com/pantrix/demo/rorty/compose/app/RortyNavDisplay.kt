package com.pantrix.demo.rorty.compose.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pantrix.compose.navigation3.PantrixScreenNavTracking
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.ui.characters.CharactersScreen
import com.pantrix.demo.rorty.compose.ui.crosslist.CharactersByIdsScreen
import com.pantrix.demo.rorty.compose.ui.crosslist.EpisodesByIdsScreen
import com.pantrix.demo.rorty.compose.ui.detail.CharacterDetailScreen
import com.pantrix.demo.rorty.compose.ui.detail.EpisodeDetailScreen
import com.pantrix.demo.rorty.compose.ui.detail.LocationDetailScreen
import com.pantrix.demo.rorty.compose.ui.episodes.EpisodesScreen
import com.pantrix.demo.rorty.compose.ui.locations.LocationsScreen

/**
 * The whole navigation surface: five owned back stacks, one `NavDisplay`, one line of Pantrix.
 *
 * Navigation 3 has no `NavController` and no graph — a back stack is a list this app holds. That is
 * why tracking it takes a single observing composable rather than a listener registration, and why
 * the tab structure below is ordinary Kotlin instead of a nested-graph DSL.
 */
@Composable
fun RortyNavDisplay() {
    var tab by rememberSaveable { mutableStateOf(RootTab.CHARACTERS) }

    // One stack per tab, each created unconditionally so switching tabs preserves where you were.
    // They cannot be built in a loop: `rememberNavBackStack` is @Composable and `associateWith`
    // takes a plain lambda, so the map is assembled from five explicit calls.
    val stacks: Map<RootTab, NavBackStack<NavKey>> = mapOf(
        RootTab.CHARACTERS to rememberNavBackStack(CharactersPage),
        RootTab.LOCATIONS to rememberNavBackStack(LocationsPage),
        RootTab.EPISODES to rememberNavBackStack(EpisodesPage),
        RootTab.PROFILE to rememberNavBackStack(ProfilePage),
        RootTab.LAB to rememberNavBackStack(LabPage),
    )
    val backStack = stacks.getValue(tab)

    // The only Pantrix wiring for screens. It emits a `screen_view` for the top of the VISIBLE stack
    // and updates the SDK's current screen, so every later click / HTTP call / crash is attributed to
    // that destination. Switching tabs changes the top, so the switch is reported too.
    //
    // Do NOT also put `TrackScreen(...)` inside an entry — that reports the same screen twice.
    //
    // One measured edge: the effect is keyed on the top NavKey, and NavKeys are data classes, so
    // pushing a key EQUAL to the one already on top emits nothing (and its matching pop emits nothing
    // either — the pair is silent in both directions). Measured on-device: `CharacterDetailPage(1)`
    // on top of `CharacterDetailPage(1)` produced a `ui_click` and no `screen_view`.
    //
    // It is self-consistent rather than wrong — the screen name never changed, so nothing is
    // mis-attributed; a screen visited twice simply counts once. It only becomes a problem if a
    // destination can legitimately re-open itself, which is why nothing in this app does.
    PantrixScreenNavTracking(backStack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                RootTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        // A plain lambda: NavigationBarItem owns its click handler, so a
                        // `Modifier.trackClick` on top would install a second one. The tab switch is
                        // already visible in the data as a `screen_view` for the new tab's top.
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            // Measured, and NOT the default: without the view-model decorator `koinViewModel()`
            // resolves against the Activity, so one `CharacterDetailViewModel` served every detail
            // entry. Opening Rick (id 1) then Morty (id 2) produced two `character_opened` events but
            // only ONE request — `/api/character/1` — and the second screen rendered Rick. Nothing
            // failed; the screen was simply wrong.
            //
            // Naming this list replaces NavDisplay's defaults, so the saveable-state decorator has to
            // be restated here to keep per-entry `rememberSaveable` working.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<CharactersPage> {
                    CharactersScreen(onOpenDetail = { backStack.add(CharacterDetailPage(it)) })
                }
                entry<CharacterDetailPage> { key ->
                    CharacterDetailScreen(
                        id = key.id,
                        onOpenLinked = { ids, title -> backStack.add(EpisodesByIdsPage(ids, title)) },
                    )
                }

                entry<LocationsPage> {
                    LocationsScreen(onOpenDetail = { backStack.add(LocationDetailPage(it)) })
                }
                entry<LocationDetailPage> { key ->
                    LocationDetailScreen(
                        id = key.id,
                        onOpenLinked = { ids, title -> backStack.add(CharactersByIdsPage(ids, title)) },
                    )
                }

                entry<EpisodesPage> {
                    EpisodesScreen(onOpenDetail = { backStack.add(EpisodeDetailPage(it)) })
                }
                entry<EpisodeDetailPage> { key ->
                    EpisodeDetailScreen(
                        id = key.id,
                        onOpenLinked = { ids, title -> backStack.add(CharactersByIdsPage(ids, title)) },
                    )
                }

                entry<CharactersByIdsPage> { key ->
                    CharactersByIdsScreen(
                        ids = key.ids,
                        title = key.title,
                        onOpenDetail = { backStack.add(CharacterDetailPage(it)) },
                    )
                }
                entry<EpisodesByIdsPage> { key ->
                    EpisodesByIdsScreen(
                        ids = key.ids,
                        title = key.title,
                        onOpenDetail = { backStack.add(EpisodeDetailPage(it)) },
                    )
                }

                entry<ProfilePage> { Soon("ProfilePage") }
                entry<LabPage> {
                    Soon("LabPage", "Open Crash Lab") { backStack.add(CrashLabPage) }
                }
                entry<CrashLabPage> { Soon("CrashLabPage") }
            },
        )
    }
}

/** Placeholder for the destinations Faz 3–5 fill in. Still a real, tracked screen. */
@Composable
private fun Soon(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (actionLabel != null && onAction != null) {
            // `trackedClick`, not `Modifier.trackClick`: Button already owns a click handler, and the
            // modifier form would install a second one on top of it.
            Button(onClick = trackedClick(actionLabel.lowercase().replace(' ', '_'), onClick = onAction)) {
                Text(actionLabel)
            }
        }
    }
}
