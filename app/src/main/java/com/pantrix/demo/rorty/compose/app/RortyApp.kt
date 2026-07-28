package com.pantrix.demo.rorty.compose.app

import android.app.Application
import com.pantrix.api.Pantrix
import com.pantrix.api.PantrixConfig
import com.pantrix.core.config.StorageEncryption
import com.pantrix.demo.rorty.compose.di.appModules
import com.pantrix.feedback.api.FeedbackConfig
import com.pantrix.feedback.api.PantrixFeedback
import com.pantrix.inspector.api.InspectorConfig
import com.pantrix.inspector.api.PantrixInspector
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RortyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val variant = BuildVariant.current

        // Initialize as early as possible: autoStart defaults to true, so collection begins
        // immediately and the first screen is attributed correctly.
        Pantrix.init(
            context = this,
            config = PantrixConfig(
                token = variant.ingestToken,
                url = variant.backendUrl
            ) {
                enableLogging(variant.enableSdkLogging)
                // A pure-Compose app has ONE Activity, so the SDK's automatic Activity tracking
                // contributes exactly one screen — `MainActivity` — and it means "the whole app".
                // Measured: without this the screen list is MainActivity (category ACTIVITY) sitting
                // alongside CharactersPage / CharacterDetailPage / LabPage (category COMPOSE), and
                // any "which screen do users spend time on" answer is skewed by a container that is
                // never actually a destination. The Nav3 tracking below is the real screen source.
                screenBlocklist(listOf("MainActivity"))
                // Twin of `usesCleartextTraffic` in the manifest. BOTH are required to reach a plain
                // http backend and they fail in different places: without this one `init` refuses
                // outright and every later tracking call is a silent no-op.
                allowInsecureConnection(variant.allowInsecureConnection)
                trackHttpHeaders(true)
                // Drives `pantrix-ktor`'s response-body capture. When on, the plugin calls
                // `call.save()` and buffers the whole response in memory so both the SDK and this
                // app can read it — fine here, worth knowing before streaming anything large.
                trackHttpBody(true)
                // 0 = unlimited on both, not "keep nothing". Dev variants keep everything so the
                // Inspector has something to show; release prunes.
                retentionDays(if (variant.isRelease) 30 else 0)
                maxStoredEvents(if (variant.isRelease) 50_000 else 0)
                // Off would delete events right after upload — and the Inspector reads that same
                // store, which is what makes events look like they are "disappearing" from it.
                keepSentEvents(!variant.isRelease)
                // FULL needs SQLCipher on the classpath (the SDK declares it `compileOnly`); this app
                // ships it on the same variants — see `BuildVariant.encryptStorage` for the failure
                // that made the pairing explicit. Get it wrong and `init` disables the SDK silently.
                storageEncryption(
                    if (variant.encryptStorage) StorageEncryption.FULL else StorageEncryption.NONE
                )
                // On — and that makes everything above a REQUEST, not a decision. Remote config is a
                // full override: a project with no SDK Config row gets the backend's baseline, which
                // has `trackHttpBody = false` and `keepSentEvents = false`. On a brand new project
                // that silently turns body capture off and empties the Inspector. Set it false to
                // make this local config authoritative; it is left on because it is a real SDK
                // feature and hiding it would make the demo lie about how the SDK behaves.
                enableRemoteConfig(true)
            }
        )

        // On release these hit the `-noop` twins linked in place of the real modules (see
        // app/build.gradle.kts), so the tools' code is not in the shipped APK at all.
        PantrixInspector.init(
            context = this,
            config = InspectorConfig(
                showFloatingButton = !variant.isRelease,
                // Feedback owns the shake gesture; two listeners on one shake would race.
                enableShakeGesture = false
            )
        )
        PantrixFeedback.init(
            context = this,
            config = FeedbackConfig(
                recipientEmail = "developersanchez1903@gmail.com",
                enableShakeGesture = true
            )
        )

        startKoin {
            androidContext(this@RortyApp)
            modules(appModules)
        }
    }
}
