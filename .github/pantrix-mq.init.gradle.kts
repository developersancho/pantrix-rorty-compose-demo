// Pantrix MQ for the CI of this app: the Gradle plugin of MQ and its engine, from GitHub Packages of
// developersancho/pantrix-mq, applied to the root project without a line in the build. The build names no token and
// builds without this script; the CI and a run by hand give it:
//
//     ./gradlew --init-script .github/pantrix-mq.init.gradle.kts \
//         lintReportDebug detekt koverXmlReportDebug mqAnalyze --continue -Pmq.project=rorty-android
//
// The packages are private, and GitHub Packages asks for a token even to read them: gpr.user and gpr.token of
// ~/.gradle/gradle.properties, or GPR_USER and GPR_TOKEN in the environment, a token that may read packages. The
// version of MQ is in pantrix-mq.version next to this script, which the CI reads for pmq too.

import io.github.developersancho.pantrix.mq.gradle.PantrixMqPlugin

initscript {
    // The block is compiled on its own: it reaches nothing declared further down in the script.
    val version = sourceFile!!.resolveSibling("pantrix-mq.version").readText().trim()
    val saved = java.util.Properties().apply {
        val home = System.getenv("GRADLE_USER_HOME") ?: "${System.getProperty("user.home")}/.gradle"
        val file = java.io.File(home, "gradle.properties")
        if (file.isFile) file.inputStream().use { load(it) }
    }
    repositories {
        maven("https://maven.pkg.github.com/developersancho/pantrix-mq") {
            content { includeGroup("io.github.developersancho.pantrix.mq") }
            credentials {
                username = saved.getProperty("gpr.user") ?: System.getenv("GPR_USER")
                password = saved.getProperty("gpr.token") ?: System.getenv("GPR_TOKEN")
            }
        }
    }
    dependencies {
        classpath("io.github.developersancho.pantrix.mq:pantrix-mq-scanner-gradle-plugin:$version")
    }
}

/** The value of [key] in ~/.gradle/gradle.properties, or of [variable] in the environment. */
fun credential(key: String, variable: String): String? {
    val home = System.getenv("GRADLE_USER_HOME") ?: "${System.getProperty("user.home")}/.gradle"
    val file = java.io.File(home, "gradle.properties")
    val saved = java.util.Properties().apply { if (file.isFile) file.inputStream().use { load(it) } }
    return saved.getProperty(key) ?: System.getenv(variable)
}

// The plugin resolves the engine as a dependency of the root project, from the repositories of the settings.
beforeSettings {
    dependencyResolutionManagement.repositories.maven("https://maven.pkg.github.com/developersancho/pantrix-mq") {
        content { includeGroup("io.github.developersancho.pantrix.mq") }
        credentials {
            username = credential("gpr.user", "GPR_USER")
            password = credential("gpr.token", "GPR_TOKEN")
        }
    }
}

// A plugin on the class path of an init script is applied by its class: its id is not known to the build.
rootProject {
    pluginManager.apply(PantrixMqPlugin::class.java)
}
