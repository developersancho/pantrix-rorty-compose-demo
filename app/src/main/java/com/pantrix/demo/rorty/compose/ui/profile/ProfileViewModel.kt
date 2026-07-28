package com.pantrix.demo.rorty.compose.ui.profile

import com.pantrix.api.Pantrix
import com.pantrix.demo.rorty.compose.app.ThemeController
import com.pantrix.demo.rorty.compose.core.mvi.MviViewModel

/**
 * The whole identity surface, one control each.
 *
 * Identity is the one part of the SDK that is **write-mostly**: `setUser`, `setUserProperty`,
 * `setUserProperties`, `unsetUserProperty` and `clearUser` all go one way, and only `getCdId` reads
 * back. That asymmetry is the thing to understand, so this screen deliberately does not pretend
 * otherwise — everything except the custom device id is echoed from what this session set.
 */
class ProfileViewModel(
    private val themeController: ThemeController,
) : MviViewModel<ProfileContract.State, ProfileContract.Intent, ProfileContract.Effect>(
    initialState = ProfileContract.State(),
) {

    @Suppress("CyclomaticComplexMethod")
    override fun onHandleIntent(intent: ProfileContract.Intent) {
        when (intent) {
            ProfileContract.Intent.Appear -> setState {
                copy(themeMode = themeController.mode.value, cdId = Pantrix.getCdId()?.ifBlank { null })
            }

            // ── identity ─────────────────────────────────────────────────────
            is ProfileContract.Intent.UserIdChanged -> setState { copy(userIdInput = intent.text) }

            ProfileContract.Intent.SetUser -> {
                val id = state.value.userIdInput.trim()
                if (id.isEmpty()) return
                // The properties overload sets the id AND a starting property set in one call. Every
                // event from here on carries `userId`, including ones already queued but not yet sent.
                val initial = mapOf("signup_source" to "demo")
                Pantrix.setUser(id, initial)
                setState {
                    copy(userId = id, properties = properties + initial, lastAction = "setUser(\"$id\", …)")
                }
                announce("Identified as $id")
            }

            ProfileContract.Intent.ClearUser -> {
                // Sign-out. It drops the id AND the properties — the SDK does not keep half an
                // identity around, which is what makes it usable on a shared device.
                Pantrix.clearUser()
                setState {
                    copy(userId = null, properties = emptyMap(), lastAction = "clearUser()")
                }
                announce("Identity cleared")
            }

            // ── user properties ──────────────────────────────────────────────
            is ProfileContract.Intent.PropertyKeyChanged -> setState { copy(propertyKeyInput = intent.text) }
            is ProfileContract.Intent.PropertyValueChanged -> setState { copy(propertyValueInput = intent.text) }

            ProfileContract.Intent.SetProperty -> {
                val key = state.value.propertyKeyInput.trim()
                val value = state.value.propertyValueInput.trim()
                if (key.isEmpty()) return
                Pantrix.setUserProperty(key, value)
                setState {
                    copy(
                        properties = properties + (key to value),
                        propertyKeyInput = "",
                        propertyValueInput = "",
                        lastAction = "setUserProperty(\"$key\", \"$value\")",
                    )
                }
                announce("Set $key")
            }

            ProfileContract.Intent.SetPropertyBundle -> {
                // The plural form is not a loop over the singular: it is one write, so the properties
                // land together and no event can be sent carrying half of them.
                val bundle = mapOf(
                    "plan" to "demo",
                    "locale" to "tr-TR",
                    "beta_tester" to "true",
                )
                Pantrix.setUserProperties(bundle)
                setState {
                    copy(properties = properties + bundle, lastAction = "setUserProperties(${bundle.size} keys)")
                }
                announce("Set ${bundle.size} properties at once")
            }

            is ProfileContract.Intent.UnsetProperty -> {
                Pantrix.unsetUserProperty(intent.key)
                setState {
                    copy(
                        properties = properties - intent.key,
                        lastAction = "unsetUserProperty(\"${intent.key}\")",
                    )
                }
                announce("Removed ${intent.key}")
            }

            // ── custom device id ─────────────────────────────────────────────
            is ProfileContract.Intent.CdIdChanged -> setState { copy(cdIdInput = intent.text) }

            ProfileContract.Intent.SetCdId -> {
                val cdId = state.value.cdIdInput.trim()
                if (cdId.isEmpty()) return
                // A DEVICE id, not a user id: it survives `clearUser()` and is meant for correlating
                // this install with an id the host already has (a CRM row, a support ticket).
                Pantrix.setCdId(cdId)
                setState { copy(cdId = Pantrix.getCdId(), cdIdInput = "", lastAction = "setCdId(\"$cdId\")") }
                announce("Custom device id set")
            }

            ProfileContract.Intent.ReadCdId -> {
                // The only getter on the whole identity surface, so it is worth its own button: this
                // value is read back OUT of the SDK, unlike everything else on this screen.
                val current = Pantrix.getCdId()
                setState { copy(cdId = current?.ifBlank { null }, lastAction = "getCdId()") }
                announce(current?.ifBlank { null }?.let { "getCdId() → $it" } ?: "getCdId() → not set")
            }

            // ── theme ────────────────────────────────────────────────────────
            is ProfileContract.Intent.ThemeSelected -> {
                themeController.set(intent.mode)
                // A durable fact about this user rather than a one-off event attribute, so it belongs
                // on the profile: every later event carries it without anyone passing it along.
                Pantrix.setUserProperty("theme", intent.mode.name.lowercase())
                setState {
                    copy(
                        themeMode = intent.mode,
                        properties = properties + ("theme" to intent.mode.name.lowercase()),
                        lastAction = "setUserProperty(\"theme\", \"${intent.mode.name.lowercase()}\")",
                    )
                }
            }
        }
    }

    private fun announce(message: String) = setEffect { ProfileContract.Effect.Toast(message) }
}
