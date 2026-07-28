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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pantrix.compose.navigation3.PantrixScreenNavTracking
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.ui.characters.CharactersScreen

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
            entryProvider = entryProvider {
                entry<CharactersPage> {
                    CharactersScreen(onOpenDetail = { backStack.add(CharacterDetailPage(it)) })
                }
                entry<CharacterDetailPage> { key ->
                    // Placeholder until Faz 3, but with the one control that measures trap 3:
                    // pushing an EQUAL key. `PantrixScreenNavTracking` keys its effect on the top
                    // NavKey, and these are data classes — so a re-push of `CharacterDetailPage(1)`
                    // is `==` to what is already on top and may emit no second `screen_view`.
                    Soon(
                        title = "CharacterDetailPage(id=${key.id})",
                        actionLabel = "Push the SAME key again (trap 3)",
                        onAction = { backStack.add(CharacterDetailPage(key.id)) },
                    )
                }

                entry<LocationsPage> { Soon("LocationsPage") }
                entry<LocationDetailPage> { key -> Soon("LocationDetailPage(id=${key.id})") }

                entry<EpisodesPage> { Soon("EpisodesPage") }
                entry<EpisodeDetailPage> { key -> Soon("EpisodeDetailPage(id=${key.id})") }

                entry<CharactersByIdsPage> { key -> Soon("CharactersByIdsPage(${key.title})") }
                entry<EpisodesByIdsPage> { key -> Soon("EpisodesByIdsPage(${key.title})") }

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
