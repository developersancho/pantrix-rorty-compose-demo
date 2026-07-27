package com.pantrix.demo.rorty.compose.domain.usecase

import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.domain.entity.Page
import com.pantrix.demo.rorty.compose.domain.repository.RickMortyRepository

/**
 * One callable per thing a screen can ask for.
 *
 * They are thin on purpose — this app's domain has no business rules to hold, and inventing some to
 * justify the layer would be worse than the layer being thin. What they DO buy is that a view model
 * declares exactly the one capability it needs instead of taking the whole repository, so what a
 * screen can reach is visible in its constructor.
 */
class GetCharactersUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(page: Int, query: String?, status: CharacterStatus?): Page<Character> =
        repository.characters(page, query, status)
}

class GetCharacterUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(id: Int): Character = repository.character(id)
}

class GetCharactersByIdsUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(ids: List<Int>): List<Character> = repository.charactersByIds(ids)
}

class GetEpisodesUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(page: Int, query: String?): Page<Episode> =
        repository.episodes(page, query)
}

class GetEpisodeUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(id: Int): Episode = repository.episode(id)
}

class GetEpisodesByIdsUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(ids: List<Int>): List<Episode> = repository.episodesByIds(ids)
}

class GetLocationsUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(page: Int, query: String?): Page<Location> =
        repository.locations(page, query)
}

class GetLocationUseCase(private val repository: RickMortyRepository) {
    suspend operator fun invoke(id: Int): Location = repository.location(id)
}
