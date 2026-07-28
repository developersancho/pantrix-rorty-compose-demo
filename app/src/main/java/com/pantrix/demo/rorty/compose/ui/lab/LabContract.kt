package com.pantrix.demo.rorty.compose.ui.lab

import com.pantrix.demo.rorty.compose.core.mvi.MviEffect
import com.pantrix.demo.rorty.compose.core.mvi.MviIntent
import com.pantrix.demo.rorty.compose.core.mvi.MviState

object LabContract {

    data class State(
        /** What the SDK says about body capture right now — a remote-config-derived value. */
        val httpBodyTracking: Boolean = false,
        val collecting: Boolean = true,
        val lastResult: String? = null,
    ) : MviState

    sealed interface Intent : MviIntent {
        data object Appear : Intent

        // Events
        data object TrackCustomEvent : Intent
        data object TrackInteraction : Intent
        data object TrackManualScreen : Intent

        // HTTP — the three ways an HTTP event can reach Pantrix
        data object HttpAutomatic : Intent
        data object HttpAutomaticFailing : Intent
        data object HttpManual : Intent
        data object HttpManualFailure : Intent

        // Diagnostics
        data object HandledException : Intent
        data object ToggleCollection : Intent
    }

    sealed interface Effect : MviEffect {
        data class Toast(val message: String) : Effect
    }
}
