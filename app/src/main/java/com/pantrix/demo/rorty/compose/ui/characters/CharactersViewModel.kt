package com.pantrix.demo.rorty.compose.ui.characters

import com.pantrix.api.Pantrix
import com.pantrix.demo.rorty.compose.core.mvi.PagedListViewModel
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.Page
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharactersUseCase

/**
 * Everything mechanical — debounce, paging, the stale-response guard — lives in
 * [PagedListViewModel]. What is left here is what makes this screen *Characters*: its status filter
 * and what a tap means.
 */
class CharactersViewModel(
    private val getCharacters: GetCharactersUseCase,
) : PagedListViewModel<CharactersContract.State, CharactersContract.Intent, CharactersContract.Effect, Character>(
    initialState = CharactersContract.State(),
) {

    override val screenName = "CharactersPage"

    override suspend fun fetch(page: Int, query: String): Page<Character> =
        getCharacters(page = page, query = query.ifBlank { null }, status = state.value.status)

    override fun CharactersContract.State.withItems(
        items: List<Character>,
        hasMore: Boolean,
        isLoading: Boolean,
        isLoadingMore: Boolean,
        message: String?,
    ) = copy(
        items = items,
        hasMore = hasMore,
        isLoading = isLoading,
        isLoadingMore = isLoadingMore,
        message = message,
    )

    override fun CharactersContract.State.withQuery(query: String) = copy(query = query)

    override fun onHandleIntent(intent: CharactersContract.Intent) {
        when (intent) {
            CharactersContract.Intent.Appear -> appearOnce()
            CharactersContract.Intent.Retry -> retry()
            CharactersContract.Intent.ReachedEnd -> loadMore()

            is CharactersContract.Intent.QueryChanged -> queryChanged(intent.text)

            is CharactersContract.Intent.StatusSelected -> {
                if (intent.status == state.value.status) return
                setState { copy(status = intent.status) }
                Pantrix.trackEvent(
                    "characters_filter_changed",
                    mapOf("status" to (intent.status?.queryValue ?: "all")),
                )
                // A segment tap is deliberate, so it reloads at once — unlike typing, which waits.
                refresh()
            }

            is CharactersContract.Intent.Selected -> {
                // No `trackInteraction(CLICK, …)` here: the row's `trackClicks` already emitted the
                // `ui_click` (or `ui_long_click`) before this intent arrived, and repeating it would
                // double every tap. The business event is this layer's to send; the interaction is
                // the view's. `via` is what makes the two views of the same act line up.
                Pantrix.trackEvent(
                    "character_opened",
                    mapOf(
                        "id" to intent.character.id,
                        "name" to intent.character.name,
                        "via" to intent.via,
                    ),
                )
                setEffect { CharactersContract.Effect.OpenDetail(intent.character.id) }
            }
        }
    }
}
