package com.pantrix.demo.rorty.compose.ui.lab

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        ActionRow(
            title = "Block the main thread (12s)",
            subtitle = "Long enough for the watchdog to fire",
            name = "crash_anr",
        ) { Handler(Looper.getMainLooper()).post { Thread.sleep(12_000) } }
    }
}

// Named, non-inlined frames so the dashboard has something recognisable to show — and, in a minified
// build, something that is only recognisable when the mapping was uploaded.
private fun crashDeepA(): Nothing = crashDeepB()
private fun crashDeepB(): Nothing = crashDeepC()
private fun crashDeepC(): Nothing = throw IllegalStateException("CrashLab: three frames deep")

private fun recurse(depth: Int): Int = recurse(depth + 1) + 1
