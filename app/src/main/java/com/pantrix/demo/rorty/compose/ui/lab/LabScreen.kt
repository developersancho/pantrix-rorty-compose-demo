package com.pantrix.demo.rorty.compose.ui.lab

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.demo.rorty.compose.ui.shared.ActionRow
import com.pantrix.demo.rorty.compose.ui.shared.SectionHeader
import com.pantrix.feedback.api.PantrixFeedback
import com.pantrix.inspector.api.PantrixInspector
import org.koin.androidx.compose.koinViewModel

@Composable
fun LabScreen(
    onOpenCrashLab: () -> Unit,
    viewModel: LabViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) { viewModel.onIntent(LabContract.Intent.Appear) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LabContract.Effect.Toast -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Events")
            ActionRow(
                title = "Track a custom event",
                subtitle = "Pantrix.trackEvent(\"lab_button_tapped\", …)",
                name = "lab_track_event",
            ) { viewModel.onIntent(LabContract.Intent.TrackCustomEvent) }
            ActionRow(
                title = "Track an interaction by hand",
                subtitle = "trackInteraction(CLICK, …) — the same family the modifiers emit",
                name = "lab_track_interaction",
            ) { viewModel.onIntent(LabContract.Intent.TrackInteraction) }
            ActionRow(
                title = "Track a screen manually",
                subtitle = "For a screen that is not a Nav3 destination — a sheet, a wizard step",
                name = "lab_track_screen",
            ) { viewModel.onIntent(LabContract.Intent.TrackManualScreen) }

            SectionHeader("HTTP — three ways in")
            Text(
                text = "Body capture is currently ${if (state.httpBodyTracking) "ON" else "OFF"} " +
                    "(Pantrix.isHttpBodyTrackingEnabled() — remote config decides, not this app).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ActionRow(
                title = "Automatic — a real Ktor request",
                subtitle = "install(PantrixKtor) did it: client=\"ktor\", and no protocol — Ktor hides the engine",
                name = "lab_http_auto",
            ) { viewModel.onIntent(LabContract.Intent.HttpAutomatic) }
            ActionRow(
                title = "Automatic — a request that 404s",
                subtitle = "The request happened, so the event exists; the failure is its status",
                name = "lab_http_auto_fail",
            ) { viewModel.onIntent(LabContract.Intent.HttpAutomaticFailing) }
            ActionRow(
                title = "Manual — a client the SDK does not instrument",
                subtitle = "Pantrix.trackHttp(client = \"lab-manual\")",
                name = "lab_http_manual",
            ) { viewModel.onIntent(LabContract.Intent.HttpManual) }
            ActionRow(
                title = "Manual — a transport failure",
                subtitle = "error = UnknownHostException, statusCode = null (not 0)",
                name = "lab_http_manual_fail",
            ) { viewModel.onIntent(LabContract.Intent.HttpManualFailure) }

            SectionHeader("Diagnostics")
            ActionRow(
                title = "Report a handled exception",
                subtitle = "trackException(…) — the app recovers and Pantrix still sees it",
                name = "lab_handled_exception",
            ) { viewModel.onIntent(LabContract.Intent.HandledException) }
            ActionRow(
                title = if (state.collecting) "Pause collection" else "Resume collection",
                subtitle = "stop() / start() — events made while stopped are never produced",
                name = "lab_toggle_collection",
            ) { viewModel.onIntent(LabContract.Intent.ToggleCollection) }

            SectionHeader("Debug tools")
            ActionRow(
                title = "Crash Lab",
                subtitle = "Real crashes, reported on the NEXT launch",
                name = "lab_open_crash_lab",
            ) { onOpenCrashLab() }
            ActionRow(
                title = "Open Inspector",
                subtitle = "PantrixInspector — or the floating button",
                name = "lab_open_inspector",
            ) { activity?.let { PantrixInspector.show(it) } }
            ActionRow(
                title = "Send Feedback",
                subtitle = "PantrixFeedback — or shake the device",
                name = "lab_open_feedback",
            ) { activity?.let { PantrixFeedback.show(it) } }

            state.lastResult?.let {
                Text(
                    text = "Last: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        SnackbarHost(snackbar)
    }
}
