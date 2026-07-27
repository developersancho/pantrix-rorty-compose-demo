package com.pantrix.demo.rorty.compose.domain.repository

import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.domain.entity.Page

/**
 * What the UI is allowed to ask for. Declared in `domain` and implemented in `data`, so `ui` can
 * depend on this without ever seeing a DTO or an `HttpClient` — only `di` knows both sides.
 */
interface RickMortyRepository {

    suspend fun characters(page: Int, query: String?, status: CharacterStatus?): Page<Character>
    suspend fun character(id: Int): Character
    suspend fun charactersByIds(ids: List<Int>, limit: Int = 20): List<Character>

    suspend fun episodes(page: Int, query: String?): Page<Episode>
    suspend fun episode(id: Int): Episode
    suspend fun episodesByIds(ids: List<Int>, limit: Int = 30): List<Episode>

    suspend fun locations(page: Int, query: String?): Page<Location>
    suspend fun location(id: Int): Location
}
