package com.pantrix.demo.rorty.compose.domain.usecase

import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.domain.entity.Page
import com.pantrix.demo.rorty.compose.domain.repository.RickMortyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A use case is one question to the repository. What can go wrong in one is asking another
 * question, or the same one with its arguments in another order.
 */
class UseCasesTest {

    private val repository = RecordingRepository()

    @Test
    fun `the character use cases ask for characters`() {
        runBlocking {
            GetCharactersUseCase(repository)(page = 2, query = "rick", status = CharacterStatus.DEAD)
            GetCharacterUseCase(repository)(id = 7)
            GetCharactersByIdsUseCase(repository)(listOf(1, 2, 3))
        }

        val asked = listOf("characters(2, rick, DEAD)", "character(7)", "charactersByIds([1, 2, 3], 20)")
        assertEquals(asked, repository.asked)
    }

    @Test
    fun `the episode use cases ask for episodes`() {
        runBlocking {
            GetEpisodesUseCase(repository)(page = 1, query = null)
            GetEpisodeUseCase(repository)(id = 28)
            GetEpisodesByIdsUseCase(repository)(listOf(4))
        }

        val asked = listOf("episodes(1, null)", "episode(28)", "episodesByIds([4], 30)")
        assertEquals(asked, repository.asked)
    }

    @Test
    fun `the location use cases ask for locations`() {
        runBlocking {
            GetLocationsUseCase(repository)(page = 3, query = "citadel")
            GetLocationUseCase(repository)(id = 3)
        }

        assertEquals(listOf("locations(3, citadel)", "location(3)"), repository.asked)
    }

    @Test
    fun `what the repository answers is what the screen gets`() {
        val page = runBlocking { GetCharactersUseCase(repository)(1, null, null) }

        assertEquals(Page(listOf(repository.rick), hasMore = true), page)
    }
}

/** Answers every question with the same few entities and remembers what it was asked. */
private class RecordingRepository : RickMortyRepository {

    val asked = mutableListOf<String>()

    val rick = Character(
        id = 1,
        name = "Rick Sanchez",
        status = CharacterStatus.ALIVE,
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth (C-137)",
        locationName = "Citadel of Ricks",
        imageUrl = "",
        episodeIds = listOf(1),
    )
    private val pilot = Episode(1, "Pilot", "December 2, 2013", "S01E01", listOf(1))
    private val earth = Location(1, "Earth (C-137)", "Planet", "Dimension C-137", listOf(1))

    override suspend fun characters(page: Int, query: String?, status: CharacterStatus?): Page<Character> {
        asked += "characters($page, $query, $status)"
        return Page(listOf(rick), hasMore = true)
    }

    override suspend fun character(id: Int): Character {
        asked += "character($id)"
        return rick
    }

    override suspend fun charactersByIds(ids: List<Int>, limit: Int): List<Character> {
        asked += "charactersByIds($ids, $limit)"
        return listOf(rick)
    }

    override suspend fun episodes(page: Int, query: String?): Page<Episode> {
        asked += "episodes($page, $query)"
        return Page(listOf(pilot), hasMore = false)
    }

    override suspend fun episode(id: Int): Episode {
        asked += "episode($id)"
        return pilot
    }

    override suspend fun episodesByIds(ids: List<Int>, limit: Int): List<Episode> {
        asked += "episodesByIds($ids, $limit)"
        return listOf(pilot)
    }

    override suspend fun locations(page: Int, query: String?): Page<Location> {
        asked += "locations($page, $query)"
        return Page(listOf(earth), hasMore = false)
    }

    override suspend fun location(id: Int): Location {
        asked += "location($id)"
        return earth
    }
}
