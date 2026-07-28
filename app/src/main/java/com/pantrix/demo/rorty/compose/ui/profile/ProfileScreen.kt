package com.pantrix.demo.rorty.compose.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantrix.compose.TrackScroll
import com.pantrix.compose.trackedClick
import com.pantrix.demo.rorty.compose.BuildConfig
import com.pantrix.demo.rorty.compose.app.BuildVariant
import com.pantrix.demo.rorty.compose.app.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // The `ScrollState` overload again, on a screen that genuinely overflows — which is what makes
    // the number readable: `scrollOffset` in pixels answers "does anyone ever reach the build card",
    // where `firstVisibleItem` would have nothing to count.
    TrackScroll(name = "profile_column", state = scrollState)

    LaunchedEffect(Unit) { viewModel.onIntent(ProfileContract.Intent.Appear) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileContract.Effect.Toast -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IdentityCard(state, viewModel)
            PropertiesCard(state, viewModel)
            CustomDeviceIdCard(state, viewModel)
            ThemeCard(state, viewModel)
            BuildCard()

            state.lastAction?.let {
                Text(
                    text = "Last call: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        SnackbarHost(snackbar)
    }
}

@Composable
private fun IdentityCard(state: ProfileContract.State, viewModel: ProfileViewModel) {
    Section("Identity") {
        Text(
            text = state.userId?.let { "Signed in as $it" } ?: "Not identified",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            // Said plainly because the screen looks like it is reading the SDK and it is not.
            text = "The SDK has no getter for the user id — this line is what this session set.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = state.userIdInput,
            onValueChange = { viewModel.onIntent(ProfileContract.Intent.UserIdChanged(it)) },
            label = { Text("User id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = trackedClick("profile_set_user") {
                    viewModel.onIntent(ProfileContract.Intent.SetUser)
                },
            ) { Text("setUser") }
            OutlinedButton(
                onClick = trackedClick("profile_clear_user") {
                    viewModel.onIntent(ProfileContract.Intent.ClearUser)
                },
            ) { Text("clearUser") }
        }
    }
}

@Composable
private fun PropertiesCard(state: ProfileContract.State, viewModel: ProfileViewModel) {
    Section("User properties") {
        if (state.properties.isEmpty()) {
            Text("None set.", style = MaterialTheme.typography.bodyMedium)
        } else {
            state.properties.forEach { (key, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$key = $value",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = trackedClick(
                            name = "profile_unset_property",
                            metadata = mapOf("key" to key),
                        ) { viewModel.onIntent(ProfileContract.Intent.UnsetProperty(key)) },
                    ) { Icon(Icons.Filled.Close, contentDescription = "Remove $key") }
                }
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = state.propertyKeyInput,
            onValueChange = { viewModel.onIntent(ProfileContract.Intent.PropertyKeyChanged(it)) },
            label = { Text("Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.propertyValueInput,
            onValueChange = { viewModel.onIntent(ProfileContract.Intent.PropertyValueChanged(it)) },
            label = { Text("Value") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = trackedClick("profile_set_property") {
                    viewModel.onIntent(ProfileContract.Intent.SetProperty)
                },
            ) { Text("setUserProperty") }
            OutlinedButton(
                onClick = trackedClick("profile_set_properties") {
                    viewModel.onIntent(ProfileContract.Intent.SetPropertyBundle)
                },
            ) { Text("setUserProperties") }
        }
    }
}

@Composable
private fun CustomDeviceIdCard(state: ProfileContract.State, viewModel: ProfileViewModel) {
    Section("Custom device id") {
        Text(
            text = state.cdId?.let { "cdId = $it" } ?: "cdId not set",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Read back with getCdId() — the one identity call that returns a value. " +
                "It is a device id, so clearUser() does not touch it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = state.cdIdInput,
            onValueChange = { viewModel.onIntent(ProfileContract.Intent.CdIdChanged(it)) },
            label = { Text("Custom device id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = trackedClick("profile_set_cdid") {
                    viewModel.onIntent(ProfileContract.Intent.SetCdId)
                },
            ) { Text("setCdId") }
            OutlinedButton(
                onClick = trackedClick("profile_read_cdid") {
                    viewModel.onIntent(ProfileContract.Intent.ReadCdId)
                },
            ) { Text("getCdId") }
        }
    }
}

@Composable
private fun ThemeCard(state: ProfileContract.State, viewModel: ProfileViewModel) {
    Section("Theme") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.themeMode == mode,
                    onClick = trackedClick(
                        name = "profile_theme_chip",
                        metadata = mapOf("mode" to mode.name.lowercase()),
                    ) { viewModel.onIntent(ProfileContract.Intent.ThemeSelected(mode)) },
                    label = { Text(mode.label) },
                )
            }
        }
        Text(
            text = "Also written as the `theme` user property, so every later event carries it " +
                "without any screen having to pass it along.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun BuildCard() {
    val variant = BuildVariant.current
    Section("This build") {
        Line("Variant", BuildConfig.BUILD_TYPE)
        Line("Application id", BuildConfig.APPLICATION_ID)
        Line("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Line("Pantrix SDK", BuildConfig.PANTRIX_SDK_VERSION)
        Line("Backend", variant.backendUrl)
        // The ingest key is public by design — it ships inside every APK and is extractable from any
        // of them. Shown truncated because a full key is 38 characters of noise, not because it is a
        // secret. The CI key, which IS secret, is in local.properties and never reaches the app.
        Line("Ingest key", variant.ingestToken.take(11) + "…")
        Line("Storage", if (variant.encryptStorage) "SQLCipher (FULL)" else "plaintext (NONE)")
        Line("SDK logging", if (variant.enableSdkLogging) "on" else "off")
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
