package com.pantrix.demo.rorty.compose.core.mvi

import androidx.lifecycle.viewModelScope
import com.pantrix.api.Pantrix
import kotlinx.coroutines.launch

/**
 * A list built from a fixed set of ids rather than a page cursor — the cross-lists.
 *
 * Deliberately not [PagedListViewModel]: there is no next page and no search here. The whole set is
 * known up front (it came out of the parent item), so paging machinery would be dead weight and a
 * search box would be a lie. The API caps how many ids one request may carry, which is why the
 * repository truncates and why [IdListContract.State.truncated] exists to say so on screen instead
 * of quietly showing a short list.
 */
object IdListContract {

    data class State<T>(
        val items: List<T> = emptyList(),
        val isLoading: Boolean = false,
        val message: String? = null,
        /** True when the parent referenced more ids than one request may fetch. */
        val truncated: Boolean = false,
    ) : MviState

    sealed interface Intent : MviIntent {
        data object Appear : Intent
        data object Retry : Intent
        data class Selected(val id: Int) : Intent
    }

    sealed interface Effect : MviEffect {
        data class OpenDetail(val id: Int) : Effect
    }
}

abstract class IdListViewModel<T>(
    private val ids: List<Int>,
) : MviViewModel<IdListContract.State<T>, IdListContract.Intent, IdListContract.Effect>(
    initialState = IdListContract.State(),
) {

    private var started = false

    protected abstract val screenName: String

    /** The event this screen sends when a row is opened, e.g. `episode_opened`. */
    protected abstract val openedEventName: String

    protected abstract suspend fun load(ids: List<Int>): List<T>

    override fun onHandleIntent(intent: IdListContract.Intent) {
        when (intent) {
            IdListContract.Intent.Appear -> {
                if (started) return
                started = true
                fetch()
            }

            IdListContract.Intent.Retry -> fetch()

            is IdListContract.Intent.Selected -> {
                Pantrix.trackEvent(openedEventName, mapOf("id" to intent.id, "via" to "cross_list"))
                setEffect { IdListContract.Effect.OpenDetail(intent.id) }
            }
        }
    }

    private fun fetch() {
        if (ids.isEmpty()) {
            setState { copy(items = emptyList(), isLoading = false, message = EMPTY_MESSAGE) }
            return
        }
        setState { copy(isLoading = true, message = null) }
        viewModelScope.launch {
            runCatching { load(ids) }
                .onSuccess { loaded ->
                    setState {
                        copy(
                            items = loaded,
                            isLoading = false,
                            message = EMPTY_MESSAGE.takeIf { loaded.isEmpty() },
                            truncated = loaded.size < ids.size,
                        )
                    }
                }
                .onFailure { throwable ->
                    Pantrix.trackException(
                        throwable,
                        mapOf("screen" to screenName, "ids" to ids.size),
                    )
                    setState {
                        copy(isLoading = false, message = throwable.message ?: "Could not load")
                    }
                }
        }
    }

    private companion object {
        const val EMPTY_MESSAGE = "Nothing to show."
    }
}
