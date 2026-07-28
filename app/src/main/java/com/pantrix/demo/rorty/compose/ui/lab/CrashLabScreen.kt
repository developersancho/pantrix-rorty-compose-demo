package com.pantrix.demo.rorty.compose.ui.lab

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pantrix.compose.TrackInteractions
import com.pantrix.demo.rorty.compose.ui.shared.ActionRow
import com.pantrix.demo.rorty.compose.ui.shared.SectionHeader

/**
 * Real crashes, captured by the SDK and reported on the **next** launch — so after tapping one,
 * reopen the app before looking in the dashboard.
 *
 * The same seven triggers as the Views demo, deliberately: two Android apps built completely
 * differently should produce the same crash records, and any difference between them is a finding.
 * The iOS demos' set differs on purpose — iOS can raise signals directly (SIGABRT, SIGBUS, SIGILL);
 * on Android the interesting cases are the JVM ones plus ANR.
 *
 * No view model. A crash is not an intent to reduce over — routing it through a reducer would put
 * frames between the tap and the throw for no benefit, and on a minified build those frames are what
 * the mapping has to resolve.
 */
@Composable
fun CrashLabScreen() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = "Each row really crashes the app. The report is written now and sent on the " +
                "next launch, so reopen the app afterwards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(16.dp),
        )

        SectionHeader("Uncaught exceptions")
        ActionRow(
            title = "RuntimeException",
            subtitle = "The ordinary case — thrown on the main thread",
            name = "crash_runtime_exception",
        ) { throw RuntimeException("CrashLab: deliberate RuntimeException") }
        ActionRow(
            title = "IllegalStateException from a nested call",
            subtitle = "Exercises a deeper stack",
            name = "crash_deep_stack",
        ) { crashDeepA() }
        ActionRow(
            title = "Crash on a background thread",
            subtitle = "An uncaught throwable off the main thread",
            name = "crash_background_thread",
        ) { Thread { throw IllegalArgumentException("CrashLab: background thread crash") }.start() }
        ActionRow(
            title = "NullPointerException",
            subtitle = "A platform NPE, not a Kotlin null check",
            name = "crash_npe",
        ) {
            val nothing: String? = null
            @Suppress("KotlinConstantConditions")
            nothing!!.length
        }

        SectionHeader("Errors")
        ActionRow(
            title = "StackOverflowError",
            subtitle = "Unbounded recursion",
            name = "crash_stack_overflow",
        ) { recurse(0) }
        ActionRow(
            title = "OutOfMemoryError",
            subtitle = "Allocate until the heap gives up",
            name = "crash_oom",
        ) {
            val hog = mutableListOf<ByteArray>()
            while (true) hog += ByteArray(16 * 1024 * 1024)
        }

        SectionHeader("ANR")

        // A real control, not a demo hook: the SDK's watchdog has a threshold, and the only way to
        // see where it sits is to block for a chosen number of seconds and watch which values report.
        // The Views demo hardcodes 12s, so this is the one crash row the two labs do not share.
        var blockSeconds by remember { mutableFloatStateOf(DEFAULT_BLOCK_SECONDS) }
        val sliderInteractions = remember { MutableInteractionSource() }

        // The third arm of `TrackInteractions`. A slider is the one control in this app that produces
        // a real `ui_drag`; the search fields cover focus. Sharing the source means the slider keeps
        // its own behaviour untouched — the SDK only watches.
        //
        // It is also the one handler-owning component here with no `trackedClick`, deliberately: a
        // slider is dragged, not clicked, and wrapping `onValueChange` would emit a `ui_click` per
        // pixel of travel. The interaction source reports the gesture once, which is the answer.
        TrackInteractions(name = "crash_anr_duration", interactionSource = sliderInteractions)

        Text(
            text = "Block for ${blockSeconds.toInt()}s",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Slider(
            value = blockSeconds,
            onValueChange = { blockSeconds = it },
            valueRange = MIN_BLOCK_SECONDS..MAX_BLOCK_SECONDS,
            steps = (MAX_BLOCK_SECONDS - MIN_BLOCK_SECONDS).toInt() - 1,
            interactionSource = sliderInteractions,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        ActionRow(
            title = "Block the main thread",
            subtitle = "Freezes for the duration above — the watchdog decides what counts as an ANR",
            name = "crash_anr",
        ) {
            val millis = blockSeconds.toLong() * 1_000
            Handler(Looper.getMainLooper()).post { Thread.sleep(millis) }
        }
    }
}

private const val MIN_BLOCK_SECONDS = 3f
private const val DEFAULT_BLOCK_SECONDS = 12f
private const val MAX_BLOCK_SECONDS = 20f

// Named, non-inlined frames so the dashboard has something recognisable to show — and, in a minified
// build, something that is only recognisable when the mapping was uploaded.
private fun crashDeepA(): Nothing = crashDeepB()
private fun crashDeepB(): Nothing = crashDeepC()
private fun crashDeepC(): Nothing = throw IllegalStateException("CrashLab: three frames deep")

private fun recurse(depth: Int): Int = recurse(depth + 1) + 1
