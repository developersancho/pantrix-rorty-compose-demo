package com.pantrix.demo.rorty.compose.app

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * The app's theme preference, persisted so it survives a restart.
 *
 * It is here rather than in a view model because the whole app reads it — `MainActivity` wraps its
 * content in the theme, and the Profile screen writes it. A view model scoped to Profile would be
 * gone the moment you left the tab.
 *
 * It also gives the Profile screen a **real** user property to set. `theme = dark` is the kind of
 * thing `setUserProperty` is actually for: a durable fact about this user that every later event
 * should carry, as opposed to something that belongs in one event's attributes.
 */
class ThemeController(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        _mode.value = mode
        prefs.edit { putString(KEY, mode.name) }
    }

    private fun read(): ThemeMode {
        val stored = prefs.getString(KEY, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    private companion object {
        const val PREFS = "rorty_prefs"
        const val KEY = "theme_mode"
    }
}
