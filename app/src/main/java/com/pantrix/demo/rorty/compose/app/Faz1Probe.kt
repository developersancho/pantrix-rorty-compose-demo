package com.pantrix.demo.rorty.compose.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.domain.repository.RickMortyRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * TEMPORARY — deleted in Faz 2 when the real screens land.
 *
 * Faz 1 has no UI of its own, so this is how the data layer gets exercised for real: it drives the
 * three things that are easy to get wrong and impossible to notice from a compile —
 *
 *  1. a normal page (paging cursor → `hasMore`),
 *  2. a search with no matches (the API's 404, which must read as "empty", not "broken"),
 *  3. a one-id batch lookup (the API answers an object, not an array).
 *
 * It also proves the Ktor client reaches Pantrix: every line below produces an HTTP event with
 * `client = "ktor"`.
 */
@Composable
fun Faz1Probe() {
    val repository = koinInject<RickMortyRepository>()
    val scope = rememberCoroutineScope()
    var log by remember { mutableStateOf(listOf<String>()) }

    fun run(label: String, block: suspend () -> String) {
        scope.launch {
            val line = runCatching { block() }.fold(
                onSuccess = { "$label → $it" },
                onFailure = { "$label FAILED → ${it::class.simpleName}: ${it.message}" },
            )
            log = (listOf(line) + log).take(6)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Faz 1 probe", style = MaterialTheme.typography.headlineSmall)

        Button(
            onClick = trackedClick("faz1_page") {
                run("page 1") { repository.characters(1, null, null).let { "${it.items.size} items, hasMore=${it.hasMore}" } }
            }
        ) { Text("Characters page 1") }

        Button(
            onClick = trackedClick("faz1_search_404") {
                run("no-match search") { repository.characters(1, "zzzzzznope", null).let { "${it.items.size} items, hasMore=${it.hasMore}" } }
            }
        ) { Text("Search with no matches (404)") }

        Button(
            onClick = trackedClick("faz1_single_id") {
                run("one-id batch") { repository.charactersByIds(listOf(1)).let { "${it.size} → ${it.firstOrNull()?.name}" } }
            }
        ) { Text("Batch lookup with ONE id") }

        Button(
            onClick = trackedClick("faz1_multi_id") {
                run("multi-id batch") { repository.charactersByIds(listOf(1, 2, 3)).let { "${it.size} → ${it.joinToString { c -> c.name }}" } }
            }
        ) { Text("Batch lookup with THREE ids") }

        log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
