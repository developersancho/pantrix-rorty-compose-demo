package com.pantrix.demo.rorty.compose.data.remote

import com.pantrix.demo.rorty.compose.data.dto.CharacterDto
import com.pantrix.demo.rorty.compose.data.dto.EpisodeDto
import com.pantrix.demo.rorty.compose.data.dto.LocationDto
import com.pantrix.demo.rorty.compose.data.dto.PageDto
import com.pantrix.ktor.PantrixKtor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The Rick & Morty API over Ktor — and the one place Pantrix touches this app's networking.
 *
 * ```
 * install(PantrixKtor)
 * ```
 *
 * That single line is the whole integration. `PantrixKtorConfig` has no public members on purpose:
 * headers, bodies, redaction and URL blocklisting are all decided by the `PantrixConfig` passed to
 * `Pantrix.init`, and `trackHttpBody` is re-read **per request**, so a remote-config change takes
 * effect without a restart.
 *
 * What it records: url, path, method, timings, status, request/response headers, and the bodies when
 * `trackHttpBody` is on. Two things to know before comparing this against the OkHttp path (which the
 * Views demo uses):
 *
 * - the method arrives **lower-cased** (`"get"`, not `"GET"`) and the client is `"ktor"`;
 * - `protocol` and `dnsAddress` are **absent**. Ktor abstracts the engine away, so the plugin cannot
 *   see them; the OkHttp integration reports both. That is a real trade, not an oversight, and the
 *   Lab screen shows it in the data.
 *
 * Response-body capture calls `call.save()`, which buffers the entire response in memory so the SDK
 * and this client can both read it. Fine at this size; worth knowing before streaming anything large.
 */
class RickMortyClient {

    private val http = HttpClient(OkHttp) {
        // Pantrix first: it hooks `on(Send)`, so it wraps everything installed after it.
        install(PantrixKtor)
        install(ContentNegotiation) {
            json(
                Json {
                    // The API sends fields this app does not model (`created`, `url`); without this
                    // the whole decode fails on a field nobody reads.
                    ignoreUnknownKeys = true
                }
            )
        }
        // Turn a 4xx/5xx into an exception rather than a decode failure further down. The repository
        // turns 404 back into an empty page — see the comment there for why that is not laziness.
        expectSuccess = true
    }

    suspend fun characters(page: Int, name: String?, status: String?): PageDto<CharacterDto> =
        http.get("$BASE/character") {
            parameter("page", page)
            name?.takeIf { it.isNotBlank() }?.let { parameter("name", it) }
            status?.let { parameter("status", it) }
        }.body()

    suspend fun character(id: Int): CharacterDto = http.get("$BASE/character/$id").body()

    /** Batch lookup. Only valid for 2+ ids — see [RickMortyRepositoryImpl] for the single-id rule. */
    suspend fun charactersByIds(ids: List<Int>): List<CharacterDto> =
        http.get("$BASE/character/${ids.joinToString(",")}").body()

    suspend fun episodes(page: Int, name: String?): PageDto<EpisodeDto> =
        http.get("$BASE/episode") {
            parameter("page", page)
            name?.takeIf { it.isNotBlank() }?.let { parameter("name", it) }
        }.body()

    suspend fun episode(id: Int): EpisodeDto = http.get("$BASE/episode/$id").body()

    suspend fun episodesByIds(ids: List<Int>): List<EpisodeDto> =
        http.get("$BASE/episode/${ids.joinToString(",")}").body()

    suspend fun locations(page: Int, name: String?): PageDto<LocationDto> =
        http.get("$BASE/location") {
            parameter("page", page)
            name?.takeIf { it.isNotBlank() }?.let { parameter("name", it) }
        }.body()

    suspend fun location(id: Int): LocationDto = http.get("$BASE/location/$id").body()

    private companion object {
        const val BASE = "https://rickandmortyapi.com/api"
    }
}
