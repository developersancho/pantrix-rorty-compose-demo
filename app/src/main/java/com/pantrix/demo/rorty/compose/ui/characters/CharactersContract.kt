package com.pantrix.demo.rorty.compose.ui.characters

import com.pantrix.demo.rorty.compose.core.mvi.MviEffect
import com.pantrix.demo.rorty.compose.core.mvi.MviIntent
import com.pantrix.demo.rorty.compose.core.mvi.PagedListState
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus

/**
 * One closed description of what this screen can show and what can happen to it — the Kotlin
 * counterpart of the iOS demos' `enum XContract`.
 */
object CharactersContract {

    data class State(
        override val items: List<Character> = emptyList(),
        override val query: String = "",
        override val isLoading: Boolean = false,
        override val isLoadingMore: Boolean = false,
        override val message: String? = null,
        override val hasMore: Boolean = true,
        /** This screen's own filter — the one thing Characters has that the other two lists do not. */
        val status: CharacterStatus? = null,
    ) : PagedListState<Character>

    sealed interface Intent : MviIntent {
        data object Appear : Intent
        data object Retry : Intent
        data class QueryChanged(val text: String) : Intent
        data class StatusSelected(val status: CharacterStatus?) : Intent
        /** The list scrolled close enough to the end to fetch the next page. */
        data object ReachedEnd : Intent

        /**
         * A character was chosen. [via] is how — tap or long press. Both gestures reach the same
         * intent on purpose: the outcome is identical, so the business event must be identical too.
         * Routing only the tap through the view model would make "opened a character" undercount by
         * however many people use the shortcut, while the `ui_click` / `ui_long_click` pair already
         * records which gesture it was.
         */
        data class Selected(val character: Character, val via: String) : Intent
    }

    sealed interface Effect : MviEffect {
        data class OpenDetail(val id: Int) : Effect
    }
}
