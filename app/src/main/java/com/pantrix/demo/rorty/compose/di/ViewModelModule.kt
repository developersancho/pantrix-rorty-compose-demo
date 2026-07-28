package com.pantrix.demo.rorty.compose.di

import com.pantrix.demo.rorty.compose.ui.characters.CharactersViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * View models. `viewModelOf` binds the constructor, so a new dependency on a use case needs no change
 * here — which is the practical reason a runtime container costs so little in a project this size.
 *
 * Every view model here takes **use cases**, never a repository and never an `HttpClient`. That is the
 * layer rule made mechanical: if a constructor below ever mentioned a `data` type, this file would be
 * the place it became obvious.
 */
val viewModelModule = module {
    viewModelOf(::CharactersViewModel)
}
