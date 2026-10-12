package com.pantrix.demo.rorty.compose.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The api is not ours and changes without notice. These pin the two promises the DTOs make about
 * that: a field that goes missing does not fail the page, and a field nobody models is ignored.
 */
class RickMortyDtosTest {

    /** The configuration the client decodes with. */
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a page is read with its cursor`() {
        val next = "https://rickandmortyapi.com/api/character?page=2"
        val body = """
            {
              "info": {"count": 826, "pages": 42, "next": "$next", "prev": null},
              "results": [
                {"id": 1, "name": "Rick Sanchez", "status": "Alive", "created": "2017-11-04T18:48:46.250Z"}
              ]
            }
        """.trimIndent()

        val page = json.decodeFromString<PageDto<CharacterDto>>(body)

        assertEquals(InfoDto(826, 42, next, null), page.info)
        assertEquals(listOf(CharacterDto(id = 1, name = "Rick Sanchez", status = "Alive")), page.results)
    }

    @Test
    fun `a field the server drops takes its default`() {
        val character = json.decodeFromString<CharacterDto>("""{"id": 7}""")

        assertEquals(CharacterDto(id = 7), character)
        assertEquals(RefDto(), character.origin)
        assertEquals(emptyList<String>(), character.episode)
    }

    @Test
    fun `the last page has no next`() {
        val page = json.decodeFromString<PageDto<LocationDto>>("""{"info": {"count": 1, "pages": 1}}""")

        assertNull(page.info.next)
        assertEquals(emptyList<LocationDto>(), page.results)
    }

    @Test
    fun `the air date of an episode comes from a snake case field`() {
        val body = """{"id": 1, "name": "Pilot", "air_date": "December 2, 2013", "episode": "S01E01"}"""

        val episode = json.decodeFromString<EpisodeDto>(body)

        assertEquals(EpisodeDto(1, "Pilot", "December 2, 2013", "S01E01"), episode)
    }
}
