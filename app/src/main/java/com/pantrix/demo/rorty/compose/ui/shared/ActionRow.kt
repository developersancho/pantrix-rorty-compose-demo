package com.pantrix.demo.rorty.compose.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pantrix.compose.trackClicks

/**
 * A titled row that does one thing — the Lab's whole vocabulary, and the Compose counterpart of the
 * Views demo's `actionRow` so the two labs read the same.
 *
 * `trackClicks` rather than `trackedClick`: a Column owns no click handler, so the modifier form
 * installs the only one there is. The row's [name] doubles as the tracked element, which means every
 * Lab row is already in the data without any per-row instrumentation.
 */
@Composable
fun ActionRow(
    title: String,
    subtitle: String,
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .trackClicks(name = name) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
    HorizontalDivider()
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}
