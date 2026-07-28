package com.pantrix.demo.rorty.compose.di

import com.pantrix.demo.rorty.compose.ui.characters.CharactersViewModel
import com.pantrix.demo.rorty.compose.ui.crosslist.CharactersByIdsViewModel
import com.pantrix.demo.rorty.compose.ui.crosslist.EpisodesByIdsViewModel
import com.pantrix.demo.rorty.compose.ui.detail.CharacterDetailViewModel
import com.pantrix.demo.rorty.compose.ui.detail.EpisodeDetailViewModel
import com.pantrix.demo.rorty.compose.ui.detail.LocationDetailViewModel
import com.pantrix.demo.rorty.compose.ui.episodes.EpisodesViewModel
import com.pantrix.demo.rorty.compose.ui.locations.LocationsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * View models. `viewModelOf` binds the constructor, so a new dependency on a use case needs no change
 * here — which is the practical reason a runtime container costs so little in a project this size.
 *
 * Every view model here takes **use cases**, never a repository and never an `HttpClient`. That is the
 * layer rule made mechanical: if a constructor below ever mentioned a `data` type, this file would be
 * the place it became obvious.
 *
 * The detail and cross-list ones take a navigation argument, so they use the explicit `viewModel { }`
 * form and read it from Koin's parameters — `koinViewModel { parametersOf(id) }` at the call site.
 */
val viewModelModule = module {
    viewModelOf(::CharactersViewModel)
    viewModelOf(::LocationsViewModel)
    viewModelOf(::EpisodesViewModel)

    viewModel { (id: Int) -> CharacterDetailViewModel(id, get()) }
    viewModel { (id: Int) -> LocationDetailViewModel(id, get()) }
    viewModel { (id: Int) -> EpisodeDetailViewModel(id, get()) }

    viewModel { (ids: List<Int>) -> CharactersByIdsViewModel(ids, get()) }
    viewModel { (ids: List<Int>) -> EpisodesByIdsViewModel(ids, get()) }
}
