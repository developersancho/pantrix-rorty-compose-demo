package com.pantrix.demo.rorty.compose.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Two consequences:
 *
 * 1. **The `Page` suffix is the naming convention**, because these names are what a dashboard shows.
 *    Renaming a class here renames a metric.
 * 2. **R8 leaves these names alone, and the app needs no rule for it.**
 *    `pantrix-compose-navigation3` ships `-keepnames class * implements …NavKey` as a consumer rule,
 *    so it arrives with the dependency. Verified on a release `mapping.txt`: every key below is
 *    `X -> X` while 73 of the app's 87 classes are renamed. See `app/proguard-rules.pro` for the
 *    measurement — and for why a qaTest build cannot be used to check this.
 */
@Serializable data object CharactersPage : NavKey
@Serializable data class CharacterDetailPage(val id: Int) : NavKey

@Serializable data object LocationsPage : NavKey
@Serializable data class LocationDetailPage(val id: Int) : NavKey

@Serializable data object EpisodesPage : NavKey
@Serializable data class EpisodeDetailPage(val id: Int) : NavKey

/** Cross-lists: an episode's characters, a character's episodes, a location's residents. */
@Serializable data class CharactersByIdsPage(val ids: List<Int>, val title: String) : NavKey
@Serializable data class EpisodesByIdsPage(val ids: List<Int>, val title: String) : NavKey

@Serializable data object ProfilePage : NavKey
@Serializable data object LabPage : NavKey
@Serializable data object CrashLabPage : NavKey

/**
 * The five tabs. Each owns its own back stack, which is the platform convention (switching tabs
 * preserves where you were) and also what keeps screen reporting honest: the tracked stack is
 * whichever one is visible.
 */
enum class RootTab(val label: String, val icon: ImageVector, val root: NavKey) {
    CHARACTERS("Characters", Icons.Filled.Groups, CharactersPage),
    LOCATIONS("Locations", Icons.Filled.LocationOn, LocationsPage),
    EPISODES("Episodes", Icons.Filled.Tv, EpisodesPage),
    PROFILE("Profile", Icons.Filled.Person, ProfilePage),
    LAB("Lab", Icons.Filled.Science, LabPage),
}
