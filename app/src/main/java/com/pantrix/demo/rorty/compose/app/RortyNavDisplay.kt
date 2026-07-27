package com.pantrix.demo.rorty.compose.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pantrix.compose.trackedClick
import com.pantrix.compose.navigation3.PantrixScreenNavTracking

/**
 * The whole navigation surface: one owned back stack, one `NavDisplay`, one line of Pantrix.
 *
 * Navigation 3 has no `NavController` and no graph — the back stack is a list this app holds, which
 * is why tracking it takes a single observing composable rather than a listener registration.
 */
@Composable
fun RortyNavDisplay() {
    val backStack = rememberNavBackStack(CharactersPage)

    // The only Pantrix wiring for screens. It emits a `screen_view` for the top of the stack AND
    // updates the SDK's current screen, so every later click / HTTP call / crash is attributed to
    // that destination.
    //
    // Do NOT also put `TrackScreen(...)` inside an entry — that would report the same screen twice.
    PantrixScreenNavTracking(backStack)

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            entryProvider = entryProvider {
                entry<CharactersPage> {
                    Placeholder(
                        title = "CharactersPage",
                        buttonLabel = "Open character 1",
                        element = "faz0_open_detail",
                    ) { backStack.add(CharacterDetailPage(id = 1)) }
                }
                entry<CharacterDetailPage> { key ->
                    Placeholder(
                        title = "CharacterDetailPage(id=${key.id})",
                        buttonLabel = "Open the Lab",
                        element = "faz0_open_lab",
                    ) { backStack.add(LabPage) }
                }
                entry<LabPage> {
                    Placeholder(title = "LabPage", buttonLabel = null, element = null, onClick = {})
                }
            },
        )
    }
}

@Composable
private fun Placeholder(
    title: String,
    buttonLabel: String?,
    element: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (buttonLabel != null && element != null) {
            // `trackedClick`, NOT `Modifier.trackClick`: a Button already owns a click handler, and
            // the modifier installs a `clickable` on top of it — two handlers for one tap. The
            // modifier form is for views that have none of their own (a Row, a Text).
            Button(onClick = trackedClick(element, onClick = onClick)) { Text(buttonLabel) }
        }
    }
}
