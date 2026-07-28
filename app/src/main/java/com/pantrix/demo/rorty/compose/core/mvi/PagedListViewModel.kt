package com.pantrix.demo.rorty.compose.core.mvi

import androidx.lifecycle.viewModelScope
import com.pantrix.api.Pantrix
import com.pantrix.demo.rorty.compose.domain.entity.Page
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The slice of state every list screen has. A screen's own `State` implements this. */
interface PagedListState<T> : MviState {
    val items: List<T>
    val query: String
    val isLoading: Boolean
    val isLoadingMore: Boolean
    val message: String?
    val hasMore: Boolean
}

/**
 * Everything mechanical about a paged, searchable list — so the three list screens differ only in
 * what they fetch and what a tap means.
 *
 * The pieces that are easy to get wrong and are therefore here once:
 *
 * - **Debounce.** A fast typist would otherwise produce one request per keystroke.
 * - **A generation counter.** Cancelling the debounce job is not enough: a request already in flight
 *   when the query changes will still return, and without the guard its results overwrite the newer
 *   query's. The counter makes a late response identify itself as stale and drop.
 * - **First load once.** `appearOnce` is idempotent, because a Compose screen's `LaunchedEffect` can
 *   re-run for reasons that are not "the user arrived".
 */
abstract class PagedListViewModel<S : PagedListState<T>, I : MviIntent, E : MviEffect, T>(
    initialState: S,
) : MviViewModel<S, I, E>(initialState) {

    private var page = 1
    private var searchJob: Job? = null
    private var generation = 0
    private var started = false

    /** The name this screen reports on a handled failure. */
    protected abstract val screenName: String

    /** `page` is 1-based; return the page plus whether the API has more. */
    protected abstract suspend fun fetch(page: Int, query: String): Page<T>

    protected abstract fun S.withItems(
        items: List<T>,
        hasMore: Boolean,
        isLoading: Boolean,
        isLoadingMore: Boolean,
        message: String?,
    ): S

    protected abstract fun S.withQuery(query: String): S

    protected fun appearOnce() {
        if (started) return
        started = true
        load(reset = true)
    }

    protected fun refresh() = load(reset = true)

    protected fun queryChanged(text: String) {
        if (text == state.value.query) return
        setState { withQuery(text) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true)
        }
    }

    /** Called from the list as it nears the end; a no-op unless there is more and nothing is in flight. */
    protected fun loadMore() {
        val current = state.value
        if (!current.hasMore || current.isLoading || current.isLoadingMore) return
        load(reset = false)
    }

    /** Re-runs the current query from page 1 — the retry button. */
    protected fun retry() = load(reset = true)

    private fun load(reset: Boolean) {
        if (reset) page = 1
        val mine = ++generation
        val query = state.value.query

        setState {
            withItems(
                items = if (reset) emptyList() else items,
                hasMore = hasMore,
                isLoading = reset,
                isLoadingMore = !reset,
                message = null,
            )
        }

        viewModelScope.launch {
            val result = runCatching { fetch(page, query) }

            // A response from a query the user has already moved past. Dropping it here is the whole
            // reason the counter exists — cancelling the debounce job does not cancel a request that
            // is already on the wire.
            if (mine != generation) return@launch

            result
                .onSuccess { fetched ->
                    page += 1
                    setState {
                        val merged = if (reset) fetched.items else items + fetched.items
                        withItems(
                            items = merged,
                            hasMore = fetched.hasMore,
                            isLoading = false,
                            isLoadingMore = false,
                            message = EMPTY_MESSAGE.takeIf { merged.isEmpty() },
                        )
                    }
                }
                .onFailure { throwable ->
                    // A handled failure: the app recovers and shows a retry, but Pantrix should still
                    // see it — otherwise a backend that is down looks like an app nobody is using.
                    Pantrix.trackException(
                        throwable,
                        mapOf("screen" to screenName, "page" to page, "query" to query),
                    )
                    setState {
                        withItems(
                            items = items,
                            hasMore = hasMore,
                            isLoading = false,
                            isLoadingMore = false,
                            message = throwable.message ?: "Could not load",
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val EMPTY_MESSAGE = "Nothing matched."
    }
}
