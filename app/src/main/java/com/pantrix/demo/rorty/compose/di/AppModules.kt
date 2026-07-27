package com.pantrix.demo.rorty.compose.di

import org.koin.core.module.Module

/**
 * Koin, not Hilt — and not by preference.
 *
 * Hilt needs KSP, and KSP's newest release is `2.3.10` while this project is on Kotlin `2.4.10`;
 * there is no KSP for Kotlin 2.4.x. Koin resolves at runtime, so it has no compiler plugin and no
 * Kotlin-version coupling. Together with kotlinx.serialization (whose plugin ships inside Kotlin) and
 * Coil 3 (no codegen), this app builds with **zero annotation processors**.
 *
 * `di` is also the only package that is allowed to know both `data` and `ui` — the wiring point where
 * a repository interface from `domain` meets its implementation from `data`.
 */
val appModules: List<Module> = listOf(
    // dataModule and viewModelModule land here in Faz 1 / Faz 2.
)
