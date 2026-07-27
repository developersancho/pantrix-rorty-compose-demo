# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ── Pantrix: keep the screen names readable ──────────────────────────────────
#
# `PantrixScreenNavTracking` reports a screen as `key::class.simpleName`, and the backend does NOT
# deobfuscate screen names — only crash stack traces go through the mapping. Without this rule every
# screen on a minified build reports as `a`, `b`, `c`: the app looks instrumented and the dashboard
# is useless, with nothing failing to say so.
#
# `-keepnames` keeps the NAME while still letting R8 shrink and optimise; a full `-keep` would also
# stop it removing anything unreachable.
#
# The Views demo needs the same rule for Fragments and Activities. In Compose the screen identity
# moved to the NavKeys, so this is where the rule has to point instead. Verified in Faz 6 by reading
# the screen names off a real qaTest build, not by trusting that the rule matches.
-keepnames class * implements androidx.navigation3.runtime.NavKey