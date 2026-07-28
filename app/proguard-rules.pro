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

# ── Pantrix screen names under R8: no rule needed here ───────────────────────
#
# `PantrixScreenNavTracking` reports a screen as `key::class.simpleName`, and the backend does NOT
# deobfuscate screen names — only crash stack traces go through the mapping. So if R8 renamed the
# NavKeys, every screen on a release build would report as a letter and nothing would fail to say so.
#
# It does not, and this app needs no rule for it: `pantrix-compose-navigation3` ships
#
#     -keepnames class * implements androidx.navigation3.runtime.NavKey
#
# as a CONSUMER rule, so it arrives with the dependency that needs it. An app-level copy would be a
# byte-identical duplicate. Measured, not assumed: with no app rule at all, a release `mapping.txt`
# keeps every NavKey name while renaming 73 of this app's 87 classes (`BuildVariant -> cj`,
# `RootTab -> g62`, and the keys' own `$$serializer` / `$Companion`). A probe object that implements
# `NavKey` survived; a byte-identical one that does not was renamed to `s43`.
#
# ── Why `qaTest` is not debuggable ───────────────────────────────────────────
#
# AGP turns obfuscation OFF for a debuggable build type even when `isMinifyEnabled = true`, so while
# `qaTest` had `isDebuggable = true` it showed readable screen names no matter what the rules said —
# any check run there passed for the wrong reason, and only a `release` build could tell the truth.
#
# `qaTest` is now `isDebuggable = false`, which is the point of the variant: minified, obfuscated and
# signed like release, but pointed at the test backend. Measured after the switch — 72 of the app's
# 85 classes renamed, all 11 NavKeys kept — so it is now a faithful stand-in for release, and the
# crash/mapping path can be exercised on the variant that is actually shipped-shaped.
