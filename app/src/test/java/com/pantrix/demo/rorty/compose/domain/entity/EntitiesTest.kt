package com.pantrix.demo.rorty.compose.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/** The one-line summaries are what every list row shows, and they are built from optional parts. */
class EntitiesTest {

    private fun character(
        status: CharacterStatus = CharacterStatus.ALIVE,
        species: String = "Human",
        gender: String = "Male",
    ) = Character(
        id = 1,
        name = "Rick Sanchez",
        status = status,
        species = species,
        type = "",
        gender = gender,
        originName = "",
        locationName = "",
        imageUrl = "",
        episodeIds = emptyList(),
    )

    @Test
    fun `a character is summed up by status, species and gender`() {
        assertEquals("Alive · Human · Male", character().summary)
        assertEquals("Dead · Alien · Female", character(CharacterStatus.DEAD, "Alien", "Female").summary)
    }

    @Test
    fun `a part the api left blank leaves no gap in the summary`() {
        assertEquals("Unknown · Male", character(CharacterStatus.UNKNOWN, species = " ").summary)
        assertEquals("Alive", character(species = "", gender = "").summary)
    }

    @Test
    fun `a status is found whatever its case, and anything else is unknown`() {
        assertEquals(CharacterStatus.ALIVE, CharacterStatus.from("alive"))
        assertEquals(CharacterStatus.ALIVE, CharacterStatus.from("ALIVE"))
        assertEquals(CharacterStatus.DEAD, CharacterStatus.from("Dead"))
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.from("unknown"))
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.from(""))
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.from("undead"))
    }

    @Test
    fun `every status has the value the api filters by`() {
        val values = CharacterStatus.entries.associate { it.queryValue to it.display }
        assertEquals(mapOf("alive" to "Alive", "dead" to "Dead", "unknown" to "Unknown"), values)
    }

    @Test
    fun `an episode is summed up by its code and air date`() {
        val episode = Episode(1, "Pilot", "December 2, 2013", "S01E01", emptyList())
        assertEquals("S01E01 · December 2, 2013", episode.summary)
        assertEquals("S01E01", episode.copy(airDate = "").summary)
        assertEquals("", episode.copy(airDate = "", code = " ").summary)
    }

    @Test
    fun `a location says so when its type or dimension is not known`() {
        val location = Location(1, "Earth", "Planet", "Dimension C-137", emptyList())
        assertEquals("Planet · Dimension C-137", location.summary)
        assertEquals("Unknown · unknown dimension", location.copy(type = "", dimension = " ").summary)
    }
}
