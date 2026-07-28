package com.pantrix.demo.rorty.compose.ui.crosslist

import com.pantrix.demo.rorty.compose.core.mvi.IdListViewModel
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharactersByIdsUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetEpisodesByIdsUseCase

/**
 * The two cross-lists. Both reuse the tab lists' `*_opened` event names on purpose: opening a
 * character is the same act whether it came from the Characters tab or from an episode's cast, and
 * splitting it into two names would mean adding them up by hand forever. `via = "cross_list"` is
 * what tells them apart, and that is [IdListViewModel]'s doing.
 */
class CharactersByIdsViewModel(
    ids: List<Int>,
    private val getCharacters: GetCharactersByIdsUseCase,
) : IdListViewModel<Character>(ids) {
    override val screenName = "CharactersByIdsPage"
    override val openedEventName = "character_opened"
    override suspend fun load(ids: List<Int>): List<Character> = getCharacters(ids)
}

class EpisodesByIdsViewModel(
    ids: List<Int>,
    private val getEpisodes: GetEpisodesByIdsUseCase,
) : IdListViewModel<Episode>(ids) {
    override val screenName = "EpisodesByIdsPage"
    override val openedEventName = "episode_opened"
    override suspend fun load(ids: List<Int>): List<Episode> = getEpisodes(ids)
}
