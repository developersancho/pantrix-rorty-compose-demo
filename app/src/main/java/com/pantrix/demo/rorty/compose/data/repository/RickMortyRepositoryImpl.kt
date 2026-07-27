package com.pantrix.demo.rorty.compose.data.repository

import com.pantrix.demo.rorty.compose.data.mapper.toEntity
import com.pantrix.demo.rorty.compose.data.remote.RickMortyClient
import com.pantrix.demo.rorty.compose.domain.entity.Character
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus
import com.pantrix.demo.rorty.compose.domain.entity.Episode
import com.pantrix.demo.rorty.compose.domain.entity.Location
import com.pantrix.demo.rorty.compose.domain.entity.Page
import com.pantrix.demo.rorty.compose.domain.repository.RickMortyRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

/**
 * Where the API's two surprises are absorbed, so no screen has to know about either.
 */
class RickMortyRepositoryImpl(private val client: RickMortyClient) : RickMortyRepository {

    override suspend fun characters(page: Int, query: String?, status: CharacterStatus?): Page<Character> =
        paged { client.characters(page, query, status?.queryValue).let { it.results to it.info.next } }
            .map { it.toEntity() }

    override suspend fun character(id: Int): Character = client.character(id).toEntity()

    override suspend fun charactersByIds(ids: List<Int>, limit: Int): List<Character> =
        byIds(
            ids = ids,
            limit = limit,
            single = { client.character(it).toEntity() },
            batch = { client.charactersByIds(it).map { dto -> dto.toEntity() } },
        )

    override suspend fun episodes(page: Int, query: String?): Page<Episode> =
        paged { client.episodes(page, query).let { it.results to it.info.next } }
            .map { it.toEntity() }

    override suspend fun episode(id: Int): Episode = client.episode(id).toEntity()

    override suspend fun episodesByIds(ids: List<Int>, limit: Int): List<Episode> =
        byIds(
            ids = ids,
            limit = limit,
            single = { client.episode(it).toEntity() },
            batch = { client.episodesByIds(it).map { dto -> dto.toEntity() } },
        )

    override suspend fun locations(page: Int, query: String?): Page<Location> =
        paged { client.locations(page, query).let { it.results to it.info.next } }
            .map { it.toEntity() }

    override suspend fun location(id: Int): Location = client.location(id).toEntity()

    /**
     * **Surprise 1: a search with no matches answers 404, not an empty page.**
     *
     * Left alone, typing a nonsense name looks exactly like the network breaking. 404 on a list
     * endpoint means "nothing matched", so it becomes an empty page; every other status still throws.
     */
    private inline fun <T> paged(block: () -> Pair<List<T>, String?>): Page<T> = try {
        val (items, next) = block()
        Page(items, hasMore = next != null)
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) Page(emptyList(), hasMore = false) else throw e
    }

    /**
     * **Surprise 2: one id answers an OBJECT, several answer an ARRAY.**
     *
     * `/character/1` is `{...}` while `/character/1,2` is `[{...},{...}]`, so a one-element list
     * would fail to decode as `List<CharacterDto>`. Routing a single id to the single-item endpoint
     * fixes it at the call site instead of teaching the decoder about both shapes.
     */
    private suspend fun <T> byIds(
        ids: List<Int>,
        limit: Int,
        single: suspend (Int) -> T,
        batch: suspend (List<Int>) -> List<T>,
    ): List<T> {
        val wanted = ids.take(limit)
        return when {
            wanted.isEmpty() -> emptyList()
            wanted.size == 1 -> listOf(single(wanted.first()))
            else -> batch(wanted)
        }
    }

    private fun <A, B> Page<A>.map(transform: (A) -> B): Page<B> =
        Page(items.map(transform), hasMore)
}
