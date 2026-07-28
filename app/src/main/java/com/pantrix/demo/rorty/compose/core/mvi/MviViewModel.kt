package com.pantrix.demo.rorty.compose.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the screen can show. One immutable snapshot, replaced whole. */
interface MviState

/** Everything that can happen to it. The only way in. */
interface MviIntent

/** Something that happens once and is not state — navigate, show a snackbar. */
interface MviEffect

/**
 * The base every screen's view model extends.
 *
 * Two channels, deliberately different types:
 *
 * - **`state` is a `StateFlow`** — it has a current value, and a screen that recomposes or a device
 *   that rotates re-reads it. That is what "state" means.
 * - **`effect` is a `SharedFlow` with `replay = 0`** — a navigation is not a value the screen *has*,
 *   it is something that happened. Replaying it on recomposition would navigate twice. This is the
 *   Kotlin counterpart of the iOS demos' `PassthroughSubject`.
 */
abstract class MviViewModel<S : MviState, I : MviIntent, E : MviEffect>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>(replay = 0)
    val effect: SharedFlow<E> = _effect.asSharedFlow()

    /** The one door in. Screens call this; they never touch state directly. */
    fun onIntent(intent: I) = onHandleIntent(intent)

    protected abstract fun onHandleIntent(intent: I)

    protected fun setState(reduce: S.() -> S) = _state.update(reduce)

    protected fun setEffect(build: () -> E) {
        viewModelScope.launch { _effect.emit(build()) }
    }
}
