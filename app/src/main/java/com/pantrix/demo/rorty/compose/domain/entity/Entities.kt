package com.pantrix.demo.rorty.compose.domain.entity

/**
 * What the app understands, as opposed to what the server sends.
 *
 * Deliberately **not** `@Serializable`. The wire shape lives in `data/dto`, and only a mapper knows
 * both — so a field rename on the server reaches one file instead of every screen. The Views demo
 * lets one type be both, which is fine there; here the layering is the point.
 */
data class Character(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    /** Free-text sub-species. Frequently empty — render as `—`, do not hide the row. */
    val type: String,
    val gender: String,
    val originName: String,
    val locationName: String,
    val imageUrl: String,
    /** Ids of the episodes this character appears in, parsed out of the API's resource URLs. */
    val episodeIds: List<Int>,
) {
    /** "Alive · Human · Male" — the list row's subtitle. */
    val summary: String
        get() = listOf(status.display, species, gender).filter { it.isNotBlank() }.joinToString(" · ")
}

enum class CharacterStatus(val queryValue: String, val display: String) {
    ALIVE("alive", "Alive"),
    DEAD("dead", "Dead"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        /**
         * `unknown` is a real filter value on this API, not a stand-in for "no filter" — absence of
         * a filter is `null`, which is why callers pass `CharacterStatus?`.
         */
        fun from(raw: String): CharacterStatus =
            entries.firstOrNull { it.queryValue.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

data class Episode(
    val id: Int,
    val name: String,
    /** "December 2, 2013" — the API's own formatting, shown verbatim. */
    val airDate: String,
    /** "S01E01" */
    val code: String,
    val characterIds: List<Int>,
) {
    val summary: String get() = listOf(code, airDate).filter { it.isNotBlank() }.joinToString(" · ")
}

data class Location(
    val id: Int,
    val name: String,
    val type: String,
    val dimension: String,
    val residentIds: List<Int>,
) {
    val summary: String
        get() = listOf(
            type.ifBlank { "Unknown" },
            dimension.ifBlank { "unknown dimension" },
        ).joinToString(" · ")
}

/**
 * One page plus whether the API has more.
 *
 * `hasMore` comes from the envelope's `next` URL being non-null — the only paging cursor this API
 * gives. The URL itself is never followed; the page index is counted locally.
 */
data class Page<T>(val items: List<T>, val hasMore: Boolean)
