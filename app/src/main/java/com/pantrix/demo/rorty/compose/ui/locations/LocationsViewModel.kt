package com.pantrix.demo.rorty.compose.ui.locations

import com.pantrix.api.Pantrix
import com.pantrix.demo.rorty.compose.core.mvi.PagedListViewModel
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.domain.entity.Page
import com.pantrix.demo.rorty.compose.domain.usecase.GetLocationsUseCase

/**
 * Characters without the filter — which is the point of [PagedListViewModel] being where it is. The
 * debounce, the paging cursor and the stale-response guard are not restated here, so the two screens
 * cannot drift apart in how they page.
 */
class LocationsViewModel(
    private val getLocations: GetLocationsUseCase,
) : PagedListViewModel<LocationsContract.State, LocationsContract.Intent, LocationsContract.Effect, Location>(
    initialState = LocationsContract.State(),
) {

    override val screenName = "LocationsPage"

    override suspend fun fetch(page: Int, query: String): Page<Location> =
        getLocations(page = page, query = query.ifBlank { null })

    override fun LocationsContract.State.withItems(
        items: List<Location>,
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

    override fun LocationsContract.State.withQuery(query: String) = copy(query = query)

    override fun onHandleIntent(intent: LocationsContract.Intent) {
        when (intent) {
            LocationsContract.Intent.Appear -> appearOnce()
            LocationsContract.Intent.Retry -> retry()
            LocationsContract.Intent.ReachedEnd -> loadMore()

            is LocationsContract.Intent.QueryChanged -> queryChanged(intent.text)

            is LocationsContract.Intent.Selected -> {
                // The row's `trackClicks` already emitted the `ui_click`; this is the business event
                // that says WHICH location, and `residents` is here because "people open the crowded
                // ones" is a question the id alone cannot answer.
                Pantrix.trackEvent(
                    "location_opened",
                    mapOf(
                        "id" to intent.location.id,
                        "name" to intent.location.name,
                        "residents" to intent.location.residentIds.size,
                        "via" to intent.via,
                    ),
                )
                setEffect { LocationsContract.Effect.OpenDetail(intent.location.id) }
            }
        }
    }
}
