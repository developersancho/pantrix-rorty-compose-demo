package com.pantrix.demo.rorty.compose.di

import com.pantrix.demo.rorty.compose.data.remote.RickMortyClient
import com.pantrix.demo.rorty.compose.data.repository.RickMortyRepositoryImpl
import com.pantrix.demo.rorty.compose.domain.repository.RickMortyRepository
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharacterUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharactersByIdsUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetCharactersUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetEpisodeUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetEpisodesByIdsUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetEpisodesUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetLocationUseCase
import com.pantrix.demo.rorty.compose.domain.usecase.GetLocationsUseCase
import org.koin.dsl.module

/**
 * The wiring point — the only place that knows both `data` and `domain`.
 *
 * `single` for the client: it owns an `HttpClient`, which owns a connection pool and the installed
 * `PantrixKtor` plugin. One per app, not one per screen.
 */
val dataModule = module {
    single { RickMortyClient() }
    single<RickMortyRepository> { RickMortyRepositoryImpl(client = get()) }

    factory { GetCharactersUseCase(get()) }
    factory { GetCharacterUseCase(get()) }
    factory { GetCharactersByIdsUseCase(get()) }
    factory { GetEpisodesUseCase(get()) }
    factory { GetEpisodeUseCase(get()) }
    factory { GetEpisodesByIdsUseCase(get()) }
    factory { GetLocationsUseCase(get()) }
    factory { GetLocationUseCase(get()) }
}
