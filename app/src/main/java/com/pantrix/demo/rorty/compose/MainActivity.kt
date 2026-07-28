package com.pantrix.demo.rorty.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.demo.rorty.compose.app.RortyNavDisplay
import com.pantrix.demo.rorty.compose.app.ThemeController
import com.pantrix.demo.rorty.compose.app.ThemeMode
import com.pantrix.demo.rorty.compose.ui.theme.PantrixRortyTheme
import org.koin.compose.koinInject

/**
 * The only Activity. Everything above it is Compose, which is also why the SDK's automatic screen
 * tracking cannot help here: it watches Activity/Fragment lifecycles, so it would see exactly one
 * screen for the entire app and never change it. Screens are reported from the Navigation 3 back
 * stack instead — see [RortyNavDisplay].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The theme lives above the navigation because the Profile screen writes it and the whole
            // app reads it — a view model scoped to that tab would be gone the moment you left it.
            val themeController = koinInject<ThemeController>()
            val mode by themeController.mode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PantrixRortyTheme(darkTheme = dark) {
                RortyNavDisplay()
            }
        }
    }
}
