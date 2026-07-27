import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
            isDebuggable = true
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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}