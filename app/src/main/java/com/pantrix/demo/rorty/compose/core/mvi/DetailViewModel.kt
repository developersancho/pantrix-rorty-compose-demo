package com.pantrix.demo.rorty.compose.core.mvi

import androidx.lifecycle.viewModelScope
import com.pantrix.api.Pantrix
import kotlinx.coroutines.launch

/**
 * One item, loaded once, plus the link out to the things it references — the shape all three detail
 * screens have.
 *
 * The link is the interesting half. A character knows its episodes, an episode knows its characters
 * and a location knows its residents; each is a list of ids the API only hands over as part of the
 * parent. Which is why the cross-list pages take ids rather than a query.
 */
object DetailContract {

    data class State<T>(
        val item: T? = null,
        val isLoading: Boolean = false,
        val message: String? = null,
    ) : MviState

    sealed interface Intent : MviIntent {
        data object Appear : Intent
        data object Retry : Intent

        /** The "N episodes" / "N residents" row was tapped. */
        data object OpenLinked : Intent
    }

    sealed interface Effect : MviEffect {
        /** Which ids to show and what to call the screen. The NavKey type is the caller's choice. */
        data class OpenLinked(val ids: List<Int>, val title: String) : Effect
    }
}

abstract class DetailViewModel<T> :
    MviViewModel<DetailContract.State<T>, DetailContract.Intent, DetailContract.Effect>(
        initialState = DetailContract.State(),
    ) {

    private var started = false

    /** Reported with a handled failure, so a broken detail screen is identifiable in the data. */
    protected abstract val screenName: String

    /** The event this screen sends when its linked list is opened, e.g. `character_episodes_opened`. */
    protected abstract val linkedEventName: String

    protected abstract suspend fun load(): T

    /** The ids this item references, and the title the linked screen should carry. */
    protected abstract fun linked(item: T): Pair<List<Int>, String>

    override fun onHandleIntent(intent: DetailContract.Intent) {
        when (intent) {
            DetailContract.Intent.Appear -> {
                // Idempotent: a Compose `LaunchedEffect` can re-run for reasons that are not "the
                // user arrived", and a detail screen re-fetching on every one of them would put
                // phantom traffic in the HTTP data.
                if (started) return
                started = true
                fetch()
            }

            DetailContract.Intent.Retry -> fetch()

            DetailContract.Intent.OpenLinked -> {
                val item = state.value.item ?: return
                val (ids, title) = linked(item)
                Pantrix.trackEvent(linkedEventName, mapOf("count" to ids.size))
                setEffect { DetailContract.Effect.OpenLinked(ids, title) }
            }
        }
    }

    private fun fetch() {
        setState { copy(isLoading = true, message = null) }
        viewModelScope.launch {
            runCatching { load() }
                .onSuccess { loaded -> setState { copy(item = loaded, isLoading = false) } }
                .onFailure { throwable ->
                    // Handled failures still belong in Pantrix — an app that recovers and shows a
                    // retry looks identical to an app nobody opened unless this is reported.
                    Pantrix.trackException(throwable, mapOf("screen" to screenName))
                    setState {
                        copy(isLoading = false, message = throwable.message ?: "Could not load")
                    }
                }
        }
    }
}
