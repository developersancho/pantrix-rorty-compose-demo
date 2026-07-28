package com.pantrix.demo.rorty.compose.ui.locations

import com.pantrix.demo.rorty.compose.core.mvi.MviEffect
import com.pantrix.demo.rorty.compose.core.mvi.MviIntent
import com.pantrix.demo.rorty.compose.core.mvi.PagedListState
import com.pantrix.demo.rorty.compose.domain.entity.Location

object LocationsContract {

    data class State(
        override val items: List<Location> = emptyList(),
        override val query: String = "",
        override val isLoading: Boolean = false,
        override val isLoadingMore: Boolean = false,
        override val message: String? = null,
        override val hasMore: Boolean = true,
    ) : PagedListState<Location>

    sealed interface Intent : MviIntent {
        data object Appear : Intent
        data object Retry : Intent
        data class QueryChanged(val text: String) : Intent
        data object ReachedEnd : Intent
        data class Selected(val location: Location, val via: String) : Intent
    }

    sealed interface Effect : MviEffect {
        data class OpenDetail(val id: Int) : Effect
    }
}
