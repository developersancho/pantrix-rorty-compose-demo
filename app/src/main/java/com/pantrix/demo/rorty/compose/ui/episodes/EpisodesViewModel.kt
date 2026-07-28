package com.pantrix.demo.rorty.compose.ui.episodes

import com.pantrix.api.Pantrix
import com.pantrix.demo.rorty.compose.core.mvi.PagedListViewModel
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Page
import com.pantrix.demo.rorty.compose.domain.usecase.GetEpisodesUseCase

class EpisodesViewModel(
    private val getEpisodes: GetEpisodesUseCase,
) : PagedListViewModel<EpisodesContract.State, EpisodesContract.Intent, EpisodesContract.Effect, Episode>(
    initialState = EpisodesContract.State(),
) {

    override val screenName = "EpisodesPage"

    override suspend fun fetch(page: Int, query: String): Page<Episode> =
        getEpisodes(page = page, query = query.ifBlank { null })

    override fun EpisodesContract.State.withItems(
        items: List<Episode>,
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

    override fun EpisodesContract.State.withQuery(query: String) = copy(query = query)

    override fun onHandleIntent(intent: EpisodesContract.Intent) {
        when (intent) {
            EpisodesContract.Intent.Appear -> appearOnce()
            EpisodesContract.Intent.Retry -> retry()
            EpisodesContract.Intent.ReachedEnd -> loadMore()

            is EpisodesContract.Intent.QueryChanged -> queryChanged(intent.text)

            is EpisodesContract.Intent.Selected -> {
                // `code` rather than the name: "S01E01" groups usefully in a dashboard, where episode
                // titles are 51 distinct strings that group into nothing.
                Pantrix.trackEvent(
                    "episode_opened",
                    mapOf(
                        "id" to intent.episode.id,
                        "code" to intent.episode.code,
                        "characters" to intent.episode.characterIds.size,
                        "via" to intent.via,
                    ),
                )
                setEffect { EpisodesContract.Effect.OpenDetail(intent.episode.id) }
            }
        }
    }
}
