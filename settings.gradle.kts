pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Pantrix SDK + its Gradle plugin. A raw-content Maven repo on the distribution repo's
        // `maven-repo` branch. It has to be in BOTH blocks: the plugin resolves through
        // `pluginManagement` and the libraries through `dependencyResolutionManagement`, so
        // declaring it in one place leaves the other half unresolvable.
        maven {
            url = uri("https://raw.githubusercontent.com/developersancho/pantrix-sdk-android-aar/maven-repo/")
            // Not decoration: without the filter Gradle asks raw.githubusercontent.com for EVERY
            // dependency before falling through to Central.
            content { includeGroupByRegex("com\\.pantrix.*") }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The other half — see the comment in `pluginManagement` above.
        maven {
            url = uri("https://raw.githubusercontent.com/developersancho/pantrix-sdk-android-aar/maven-repo/")
            content { includeGroupByRegex("com\\.pantrix.*") }
        }
    }
}

rootProject.name = "pantrix-rorty-compose-demo"
include(":app")
 