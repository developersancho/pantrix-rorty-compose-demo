package com.pantrix.demo.rorty.compose.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pantrix.demo.rorty.compose.domain.entity.CharacterStatus

/** Status → colour. One definition, so a list row and a detail row can never disagree. */
@Composable
fun statusColor(status: CharacterStatus): Color = when (status) {
    CharacterStatus.ALIVE -> Color(0xFF2E7D32)
    CharacterStatus.DEAD -> Color(0xFFC62828)
    CharacterStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
}

/** The list row shape shared by all three tabs. Carries no click handling — the caller owns that. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    imageUrl: String? = null,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> leading()
            !imageUrl.isNullOrBlank() -> AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Full-width message — an empty result or a load failure. */
@Composable
fun StateMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
    )
}

/** Footer spinner shown while the next page is in flight. */
@Composable
fun LoadingMoreFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}
