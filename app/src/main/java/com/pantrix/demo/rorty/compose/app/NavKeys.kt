package com.pantrix.demo.rorty.compose.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every destination in the app, as a `NavKey`.
 *
 * **The class name IS the screen name.** `PantrixScreenNavTracking` reports `key::class.simpleName`
 * and reads nothing else — so `CharacterDetailPage(id = 42)` is reported as `"CharacterDetailPage"`
 * and the id never leaves the device. That is the SDK's design, not a limitation to work around:
 * arguments are exactly the part of a route most likely to be personal.
 *
 * Two consequences to keep in mind:
 *
 * 1. **The `Page` suffix is the naming convention**, because these names are what a dashboard shows.
 *    Renaming a class here renames a metric.
 * 2. **R8 renames classes.** On a minified build (`qaTest` / `release`) these would report as `a`,
 *    `b`, `c` unless a `-keepnames` rule protects them — see `app/proguard-rules.pro`. The Views
 *    demo hits the same problem with Fragment names and solves it the same way.
 */
@Serializable
data object CharactersPage : NavKey

@Serializable
data class CharacterDetailPage(val id: Int) : NavKey

@Serializable
data object LabPage : NavKey
