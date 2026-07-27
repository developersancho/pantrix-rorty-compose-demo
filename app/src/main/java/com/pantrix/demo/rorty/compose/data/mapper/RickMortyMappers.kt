package com.pantrix.demo.rorty.compose.data.mapper

import com.pantrix.demo.rorty.compose.data.dto.CharacterDto
import com.pantrix.demo.rorty.compose.data.dto.EpisodeDto
import com.pantrix.demo.rorty.compose.data.dto.LocationDto
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location

/**
 * The one place that knows both shapes. Everything above this file works in entities; everything
 * below it works in DTOs.
 */

/** `https://rickandmortyapi.com/api/episode/28` → `28`. Anything unparseable is dropped. */
private fun List<String>.toIds(): List<Int> = mapNotNull { it.substringAfterLast('/').toIntOrNull() }

fun CharacterDto.toEntity(): Character = Character(
    id = id,
    name = name,
    status = CharacterStatus.from(status),
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    locationName = location.name,
    imageUrl = image,
    episodeIds = episode.toIds(),
)

fun EpisodeDto.toEntity(): Episode = Episode(
    id = id,
    name = name,
    airDate = airDate,
    code = episode,
    characterIds = characters.toIds(),
)

fun LocationDto.toEntity(): Location = Location(
    id = id,
    name = name,
    type = type,
    dimension = dimension,
    residentIds = residents.toIds(),
)
