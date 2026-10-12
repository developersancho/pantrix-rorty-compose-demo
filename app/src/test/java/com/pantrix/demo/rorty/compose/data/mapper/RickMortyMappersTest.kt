package com.pantrix.demo.rorty.compose.data.mapper

import com.pantrix.demo.rorty.compose.data.dto.CharacterDto
import com.pantrix.demo.rorty.compose.data.dto.EpisodeDto
import com.pantrix.demo.rorty.compose.data.dto.LocationDto
import com.pantrix.demo.rorty.compose.data.dto.RefDto
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The mappers are the only code that knows both the wire shape and the entities, so a field that
 * ends up in the wrong place is caught here or nowhere.
 */
class RickMortyMappersTest {

    private val api = "https://rickandmortyapi.com/api"

    @Test
    fun `a character keeps its fields and takes its episode ids out of the urls`() {
        val dto = CharacterDto(
            id = 1,
            name = "Rick Sanchez",
            status = "Alive",
            species = "Human",
            type = "",
            gender = "Male",
            origin = RefDto("Earth (C-137)", "$api/location/1"),
            location = RefDto("Citadel of Ricks", "$api/location/3"),
            image = "$api/character/avatar/1.jpeg",
            episode = listOf("$api/episode/1", "$api/episode/28"),
        )

        val expected = Character(
            id = 1,
            name = "Rick Sanchez",
            status = CharacterStatus.ALIVE,
            species = "Human",
            type = "",
            gender = "Male",
            originName = "Earth (C-137)",
            locationName = "Citadel of Ricks",
            imageUrl = "$api/character/avatar/1.jpeg",
            episodeIds = listOf(1, 28),
        )
        assertEquals(expected, dto.toEntity())
    }

    @Test
    fun `a status the app does not know is unknown, not a crash`() {
        assertEquals(CharacterStatus.DEAD, CharacterDto(status = "Dead").toEntity().status)
        assertEquals(CharacterStatus.UNKNOWN, CharacterDto(status = "Presumed dead").toEntity().status)
        assertEquals(CharacterStatus.UNKNOWN, CharacterDto().toEntity().status)
    }

    @Test
    fun `a url without an id at its end is dropped and the others stay in order`() {
        val urls = listOf("$api/episode/12", "", "$api/episode/", "not a url", "$api/episode/3")
        assertEquals(listOf(12, 3), CharacterDto(episode = urls).toEntity().episodeIds)
    }

    @Test
    fun `an episode takes its code from the field the api calls episode`() {
        val dto = EpisodeDto(
            id = 28,
            name = "The Ricklantis Mixup",
            airDate = "September 10, 2017",
            episode = "S03E07",
            characters = listOf("$api/character/1", "$api/character/2"),
        )

        val expected = Episode(28, "The Ricklantis Mixup", "September 10, 2017", "S03E07", listOf(1, 2))
        assertEquals(expected, dto.toEntity())
    }

    @Test
    fun `a location takes the ids of its residents`() {
        val dto = LocationDto(
            id = 3,
            name = "Citadel of Ricks",
            type = "Space station",
            dimension = "unknown",
            residents = listOf("$api/character/8", "$api/character/14"),
        )

        val expected = Location(3, "Citadel of Ricks", "Space station", "unknown", listOf(8, 14))
        assertEquals(expected, dto.toEntity())
    }
}
