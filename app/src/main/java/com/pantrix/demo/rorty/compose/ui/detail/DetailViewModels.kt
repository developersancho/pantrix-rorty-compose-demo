package com.pantrix.demo.rorty.compose.ui.detail

import com.pantrix.demo.rorty.compose.core.mvi.DetailViewModel
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharacterUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetEpisodeUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetLocationUseCase

/**
 * The three detail view models. Everything mechanical is in [DetailViewModel]; what is left is what
 * each screen loads and what it links to.
 *
 * Each takes its `id` as a constructor parameter, resolved through Koin's `parametersOf`. That works
 * because `NavDisplay` is given a `ViewModelStoreNavEntryDecorator`, which gives every back-stack
 * entry its own `ViewModelStoreOwner` — without it `koinViewModel()` would resolve against the
 * Activity and `CharacterDetailPage(1)` would hand its view model to `CharacterDetailPage(2)`.
 */
class CharacterDetailViewModel(
    private val id: Int,
    private val getCharacter: GetCharacterUseCase,
) : DetailViewModel<Character>() {
    override val screenName = "CharacterDetailPage"
    override val linkedEventName = "character_episodes_opened"
    override suspend fun load(): Character = getCharacter(id)
    override fun linked(item: Character) = item.episodeIds to "${item.name} · episodes"
}

class LocationDetailViewModel(
    private val id: Int,
    private val getLocation: GetLocationUseCase,
) : DetailViewModel<Location>() {
    override val screenName = "LocationDetailPage"
    override val linkedEventName = "location_residents_opened"
    override suspend fun load(): Location = getLocation(id)
    override fun linked(item: Location) = item.residentIds to "${item.name} · residents"
}

class EpisodeDetailViewModel(
    private val id: Int,
    private val getEpisode: GetEpisodeUseCase,
) : DetailViewModel<Episode>() {
    override val screenName = "EpisodeDetailPage"
    override val linkedEventName = "episode_characters_opened"
    override suspend fun load(): Episode = getEpisode(id)
    override fun linked(item: Episode) = item.characterIds to "${item.code} · characters"
}
