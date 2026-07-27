package com.pantrix.demo.rorty.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pantrix.demo.rorty.compose.app.RortyNavDisplay
import com.pantrix.demo.rorty.compose.ui.theme.PantrixRortyTheme

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
            PantrixRortyTheme {
                RortyNavDisplay()
            }
        }
    }
}
