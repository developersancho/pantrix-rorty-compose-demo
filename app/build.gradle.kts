import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.pantrix.gradle)
}

// R8 mapping upload. The CI keys are secrets, so they come from the gitignored local.properties (or
// the environment on a build machine) — never from BuildConfig, which ships inside the APK. Each
// variant uploads to its OWN project: the mapping project must match the crash project, or the
// backend has no mapping for the build that crashed and stack traces stay obfuscated.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.let { load(FileInputStream(it)) }
}

fun ciKey(variant: String): String =
    (localProps["pantrix.ci.key.$variant"] as String?)
        ?: providers.environmentVariable("PANTRIX_CI_KEY_${variant.uppercase()}").orNull
        ?: ""

pantrix {
    variantFilter {
        val key = ciKey(name)
        apiKey = key
        // localhost, NOT 10.0.2.2: this task runs on the BUILD MACHINE. 10.0.2.2 is the emulator's
        // alias for the host loopback and only means anything from inside the emulator — the SDK's
        // runtime url (PANTRIX_URL below) uses that, this does not. Swapping them costs a build-long
        // socket timeout with no useful error.
        apiUrl = "http://localhost:8099/api"
        // debug is not minified, so R8 never runs and there is no mapping.txt to upload. An absent
        // key also disables the variant rather than failing the build on a machine with no creds.
        enabled = name != "debug" && key.isNotEmpty()
    }
}

android {
    namespace = "com.pantrix.demo.rorty.compose"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.pantrix.demo.rorty.compose"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The emulator reaches the host's loopback at 10.0.2.2 (a real device needs the Mac's LAN IP).
        // Every variant points at the local TEST backend today; only the release arm changes when a
        // production backend exists.
        buildConfigField("String", "PANTRIX_URL", "\"http://10.0.2.2:8099\"")
        // The SDK version this app is pinned to, read from the version catalog so the Profile screen
        // cannot drift from the dependency. `AppConstants.SDK_VERSION` would report the same value,
        // but it is `@PantrixInternalApi` — a demo has no business reaching in there.
        buildConfigField(
            "String",
            "PANTRIX_SDK_VERSION",
            "\"${libs.versions.pantrix.get()}\"",
        )
    }

    signingConfigs {
        create("signingConfigRelease") {
            val keystorePropertiesFile = project.rootProject.file("signing/release.signing.properties")
            if (!keystorePropertiesFile.exists()) {
                System.err.println("📜 Missing release.signing.properties file for release signing")
            } else {
                val keystoreProperties = Properties().apply {
                    load(FileInputStream(keystorePropertiesFile))
                }
                try {
                    storeFile =
                        project.rootProject.file(keystoreProperties["storeFile"] as String)
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                } catch (_: Exception) {
                    System.err.println("📜 release.signing.properties file is malformed")
                }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("signingConfigRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "RTCompose")
            buildConfigField("String", "PANTRIX_TOKEN", "\"px_yq7bml451uguz6kyexb30lmr5wm8kfgz4xti\"")
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "RTCompose Dev")
            buildConfigField("String", "PANTRIX_TOKEN", "\"px_5vte6pp4azh8prryawqthztp10momyg1zf00\"")
        }

        create("qaTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            isDebuggable = false
            // qaTest is a custom build type with no counterpart in the SDK library
            // modules (they only have debug/release). With the `projects.*` deps,
            // variant-aware resolution needs an explicit fallback, or every project
            // dependency FAILS to resolve for this variant (IDE sync shows "Failed to
            // resolve: project :pantrix-*"). The published-AAR path matched leniently;
            // project deps don't. Fall back to the modules' release variant — the same
            // variant the published AARs resolved to, so qaTest behaviour is unchanged.
            matchingFallbacks += "release"
            resValue("string", "app_name", "RTCompose Test")
            buildConfigField("String", "PANTRIX_TOKEN", "\"px_aqclf33jr3z4aowboyljrhp0h1mc9n964a7e\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation 3. The back stack is a plain observable list this app owns — there is no
    // NavController and no graph, which is also why Pantrix can track it with one composable.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Pantrix. `pantrix-sdk` is required; the four add-ons are opt-in and each needs its peers
    // (declared above) because it ships them `compileOnly`.
    implementation(libs.pantrix.sdk)
    implementation(libs.pantrix.compose)
    implementation(libs.pantrix.compose.navigation3)
    implementation(libs.pantrix.ktor)

    // The debug tools are twin pairs: the real module on debug/qaTest, the inert `-noop` on release,
    // so the tool's code is not in the shipped APK at all and no call site needs a BuildConfig.DEBUG
    // guard.
    debugImplementation(libs.pantrix.inspector)
    debugImplementation(libs.pantrix.feedback)
    "qaTestImplementation"(libs.pantrix.inspector)
    "qaTestImplementation"(libs.pantrix.feedback)
    releaseImplementation(libs.pantrix.inspector.noop)
    releaseImplementation(libs.pantrix.feedback.noop)

    // The home-screen widget is DEBUG only. It has no init call — its receiver arrives through the
    // merged manifest — so merely having the real module on a build type makes the widget appear in
    // the launcher's picker. That is why Glance must come with it: pantrix-widget declares Glance
    // `compileOnly`, and without it the system hits NoClassDefFoundError the moment the widget is
    // placed, not at build time. qaTest and release take the `-noop` twin, which registers nothing.
    debugImplementation(libs.pantrix.widget)
    debugImplementation(libs.glance.appwidget)
    "qaTestImplementation"(libs.pantrix.widget.noop)
    releaseImplementation(libs.pantrix.widget.noop)

    // SQLCipher, on exactly the variants that ask for `StorageEncryption.FULL`
    // (`BuildVariant.encryptStorage`). The SDK declares it `compileOnly`, so the app supplies it —
    // and if it doesn't, `Pantrix.init` throws, catches, logs "the SDK is disabled" and carries on.
    // Release also sets `enableLogging(false)`, so that line never appears: the app runs perfectly
    // and reports nothing at all. Measured — the release project had zero rows in ClickHouse while
    // debug and qaTest were fine. Keep this list and `encryptStorage` in step.
    //
    // ~7 MB of native libraries per ABI, which is why debug does not carry it.
    "qaTestImplementation"(libs.sqlcipher)
    releaseImplementation(libs.sqlcipher)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}