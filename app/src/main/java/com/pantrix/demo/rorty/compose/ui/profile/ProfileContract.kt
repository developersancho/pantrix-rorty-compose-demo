package com.pantrix.demo.rorty.compose.ui.profile

import com.pantrix.demo.rorty.compose.app.ThemeMode
import com.pantrix.demo.rorty.compose.core.mvi.MviEffect
import com.pantrix.demo.rorty.compose.core.mvi.MviIntent
import com.pantrix.demo.rorty.compose.core.mvi.MviState

object ProfileContract {

    /**
     * Note what this state is and is not.
     *
     * [cdId] is **read from the SDK** — `Pantrix.getCdId()` exists, so the screen can show what the
     * SDK actually holds. [userId] and [properties] are the app's own memory of what it set, because
     * there is no getter for either: identity goes one way into the SDK. Clearing the app's data
     * would empty this screen while the SDK still knows the user, which is exactly why the UI says so
     * rather than implying it is reading anything back.
     */
    data class State(
        val userId: String? = null,
        val userIdInput: String = "",
        val properties: Map<String, String> = emptyMap(),
        val propertyKeyInput: String = "",
        val propertyValueInput: String = "",
        val cdId: String? = null,
        val cdIdInput: String = "",
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        /** The last identity call made, echoed back so the screen shows something happened. */
        val lastAction: String? = null,
    ) : MviState

    sealed interface Intent : MviIntent {
        data object Appear : Intent

        data class UserIdChanged(val text: String) : Intent
        data object SetUser : Intent
        data object ClearUser : Intent

        data class PropertyKeyChanged(val text: String) : Intent
        data class PropertyValueChanged(val text: String) : Intent
        data object SetProperty : Intent
        data object SetPropertyBundle : Intent
        data class UnsetProperty(val key: String) : Intent

        data class CdIdChanged(val text: String) : Intent
        data object SetCdId : Intent
        data object ReadCdId : Intent

        data class ThemeSelected(val mode: ThemeMode) : Intent
    }

    sealed interface Effect : MviEffect {
        data class Toast(val message: String) : Effect
    }
}
