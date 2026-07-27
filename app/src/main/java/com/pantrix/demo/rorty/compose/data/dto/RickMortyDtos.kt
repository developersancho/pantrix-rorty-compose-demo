package com.pantrix.demo.rorty.compose.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape, and nothing else. Every field carries a default so a server that drops one does
 * not fail the whole decode — `ignoreUnknownKeys` covers the other direction.
 *
 * kotlinx.serialization rather than Moshi (the Views demo's choice): it is Ktor's native converter,
 * and its compiler plugin ships inside Kotlin, so it needs no KSP — which is what lets this project
 * stay on Kotlin 2.4.10 at all.
 */
@Serializable
data class PageDto<T>(
    val info: InfoDto = InfoDto(),
    val results: List<T> = emptyList(),
)

@Serializable
data class InfoDto(
    val count: Int = 0,
    val pages: Int = 0,
    /** Absolute URL of the next page, or null on the last one — the only paging cursor this API gives. */
    val next: String? = null,
    val prev: String? = null,
)

@Serializable
data class CharacterDto(
    val id: Int = 0,
    val name: String = "",
    val status: String = "",
    val species: String = "",
    val type: String = "",
    val gender: String = "",
    val origin: RefDto = RefDto(),
    val location: RefDto = RefDto(),
    val image: String = "",
    /** Resource URLs — `/api/episode/1`. The id is parsed out by the mapper. */
    val episode: List<String> = emptyList(),
)

/** A named cross-reference (origin / location); `url` is empty when the API has no record. */
@Serializable
data class RefDto(
    val name: String = "",
    val url: String = "",
)

@Serializable
data class EpisodeDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("air_date") val airDate: String = "",
    /** The API calls this field `episode` and it holds "S01E01" — renamed to `code` on the entity. */
    val episode: String = "",
    val characters: List<String> = emptyList(),
)

@Serializable
data class LocationDto(
    val id: Int = 0,
    val name: String = "",
    val type: String = "",
    val dimension: String = "",
    val residents: List<String> = emptyList(),
)
