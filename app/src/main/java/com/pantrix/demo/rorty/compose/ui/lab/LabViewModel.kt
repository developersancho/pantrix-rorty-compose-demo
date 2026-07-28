package com.pantrix.demo.rorty.compose.ui.lab

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.pantrix.api.Pantrix
import com.pantrix.core.processors.event.data.interaction.InteractionType
import com.pantrix.demo.rorty.compose.app.BuildVariant
import com.pantrix.demo.rorty.compose.core.mvi.MviViewModel
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharacterUseCase
import kotlinx.coroutines.launch

/**
 * One call per row, so the Lab is a readable index of the SDK's surface.
 *
 * The HTTP section is this demo's distinctive part: four rows covering every way an `http` event can
 * reach Pantrix, producing data deliberately comparable with the Views demo's OkHttp rows.
 *
 * | row                       | client       | statusCode | failureReason           |
 * |---------------------------|--------------|------------|-------------------------|
 * | automatic                 | `ktor`       | 200        | absent                  |
 * | automatic, failing        | `ktor`       | 404        | absent                  |
 * | manual                    | `lab-manual` | 200        | absent                  |
 * | manual, transport failure | `lab-manual` | **null**   | `UnknownHostException`  |
 *
 * Two things measured here rather than assumed, both of which contradict the obvious guess:
 *
 * - **`method` is lowercase for every client**, `okhttp` and `alamofire` included, and even when the
 *   caller passes `"GET"` to `trackHttp` by hand. The SDK normalises it; it is not a Ktor quirk.
 * - **`protocol` is the genuine Ktor-vs-OkHttp difference.** Across every event this backend holds,
 *   `okhttp` reports `HTTP_2` when it can and `urlsession` reports `h2`/`h3`, while `ktor` has
 *   reported it on none of its requests — Ktor abstracts the engine away, and the OkHttp interceptor
 *   sits close enough to the socket to know. `dnsAddress` goes the same way.
 *
 * The last row is the one the instrumented client cannot demonstrate without unplugging the network,
 * and it is the shape a dashboard has to handle: `statusCode` is **null**, not `0`.
 */
class LabViewModel(
    private val getCharacter: GetCharacterUseCase,
) : MviViewModel<LabContract.State, LabContract.Intent, LabContract.Effect>(
    initialState = LabContract.State(),
) {

    private val variant = BuildVariant.current

    override fun onHandleIntent(intent: LabContract.Intent) {
        when (intent) {
            LabContract.Intent.Appear -> setState {
                // Not a local flag: remote config can turn body capture off, and this is how an app
                // asks what the SDK actually decided. `pantrix-ktor` reads the same thing to avoid
                // buffering a response the SDK would only drop.
                copy(httpBodyTracking = Pantrix.isHttpBodyTrackingEnabled())
            }

            // ── events ───────────────────────────────────────────────────────
            LabContract.Intent.TrackCustomEvent -> {
                Pantrix.trackEvent(
                    "lab_button_tapped",
                    mapOf("source" to "lab", "variant" to variant.name),
                )
                report("trackEvent(\"lab_button_tapped\", …)")
            }

            LabContract.Intent.TrackInteraction -> {
                // The same event family the Compose modifiers emit, called by hand — useful for a
                // gesture the SDK has no modifier for.
                Pantrix.trackInteraction(
                    InteractionType.CLICK,
                    mapOf("element" to "lab_interaction_row"),
                )
                report("trackInteraction(CLICK, …)")
            }

            LabContract.Intent.TrackManualScreen -> {
                // A screen name the app chooses rather than one derived from a class. In this app
                // `PantrixScreenNavTracking` covers every real destination, so this is for screens
                // that are not destinations — a bottom sheet, a wizard step, a paged carousel.
                Pantrix.trackComposeScreenView("LabManualScreen")
                report("trackComposeScreenView(\"LabManualScreen\")")
            }

            // ── HTTP ─────────────────────────────────────────────────────────
            LabContract.Intent.HttpAutomatic -> viewModelScope.launch {
                runCatching { getCharacter(1) }
                    .onSuccess { report("ktor → 200, one http event, client=\"ktor\"") }
                    .onFailure { report("ktor → failed: ${it::class.simpleName}") }
            }

            LabContract.Intent.HttpAutomaticFailing -> viewModelScope.launch {
                // A real 404 through the instrumented client. The request genuinely happened, so
                // there IS an http event — the failure is in its status code, not in its absence.
                runCatching { getCharacter(NONEXISTENT_ID) }
                    .onSuccess { report("unexpected: id $NONEXISTENT_ID resolved") }
                    .onFailure { report("ktor → 404, still one http event (${it::class.simpleName})") }
            }

            LabContract.Intent.HttpManual -> {
                val start = SystemClock.elapsedRealtime()
                Pantrix.trackHttp(
                    url = "https://rickandmortyapi.com/api/character/1",
                    path = "/api/character/1",
                    method = "GET",
                    startTime = start,
                    endTime = SystemClock.elapsedRealtime(),
                    statusCode = 200,
                    client = "lab-manual",
                )
                report("trackHttp(client=\"lab-manual\")")
            }

            LabContract.Intent.HttpManualFailure -> {
                // What a transport failure looks like: no status code at all, an exception instead.
                // The instrumented rows above cannot show this without unplugging the network, and it
                // is the shape a dashboard has to handle — `statusCode` null is not "0".
                val start = SystemClock.elapsedRealtime()
                Pantrix.trackHttp(
                    url = "https://rickandmortyapi.example/api/character/1",
                    path = "/api/character/1",
                    method = "GET",
                    startTime = start,
                    endTime = SystemClock.elapsedRealtime(),
                    statusCode = null,
                    error = java.net.UnknownHostException("rickandmortyapi.example"),
                    client = "lab-manual",
                )
                report("trackHttp(error = UnknownHostException, statusCode = null)")
            }

            // ── diagnostics ──────────────────────────────────────────────────
            LabContract.Intent.HandledException -> {
                runCatching { error("Lab: a deliberately handled failure") }
                    .onFailure { Pantrix.trackException(it, mapOf("screen" to "LabPage")) }
                report("trackException(…) — the app keeps running")
            }

            LabContract.Intent.ToggleCollection -> {
                val nowCollecting = !state.value.collecting
                // `stop()` pauses collection without tearing the SDK down; `start()` resumes it.
                // Events produced while stopped are not queued — they are not produced at all.
                if (nowCollecting) Pantrix.start() else Pantrix.stop()
                setState { copy(collecting = nowCollecting) }
                report(if (nowCollecting) "start() — collecting again" else "stop() — collection paused")
            }
        }
    }

    private fun report(result: String) {
        setState { copy(lastResult = result) }
        setEffect { LabContract.Effect.Toast(result) }
    }

    private companion object {
        /** Far past the API's last character, so it answers 404 rather than a row. */
        const val NONEXISTENT_ID = 999_999
    }
}
