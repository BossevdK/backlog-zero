@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.backlogzero.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.backlogzero.app.data.CoverStorage
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.ui.theme.Gold
import com.backlogzero.app.ui.theme.color

fun MediaType.icon(): ImageVector = when (this) {
    MediaType.GAME -> Icons.Filled.SportsEsports
    MediaType.MOVIE -> Icons.Filled.Movie
    MediaType.SERIES -> Icons.Filled.Tv
    MediaType.BOOK -> Icons.AutoMirrored.Filled.MenuBook
}

/** Omslag, of een rustige kleurvlak-placeholder per titel als er geen afbeelding is. */
@Composable
fun Cover(
    title: String,
    type: MediaType,
    coverModel: Any?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(placeholderBrush(title)), contentAlignment = Alignment.Center) {
        if (coverModel != null) {
            AsyncImage(
                model = coverModel,
                contentDescription = "Omslag van $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(type.icon(), contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun ItemCover(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Cover(item.title, item.type, item.coverFile?.let { CoverStorage.file(context, it) }, modifier)
}

private fun placeholderBrush(seed: String): Brush {
    val hue = ((seed.hashCode() and 0x7fffffff) % 360).toFloat()
    return Brush.linearGradient(
        listOf(Color.hsv(hue, 0.45f, 0.42f), Color.hsv((hue + 45f) % 360f, 0.55f, 0.22f)),
    )
}

@Composable
fun StatusBadge(status: ItemStatus, modifier: Modifier = Modifier) {
    val color = status.color()
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(status.label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun PosterCard(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(210.dp).clip(MaterialTheme.shapes.medium)) {
            ItemCover(item, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)),
                ),
            )
            StatusBadge(
                item.status,
                Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50)),
            )
            if (item.favorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Favoriet",
                    tint = Color(0xFFFF7A9A),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Smalle kaart voor horizontale rijen op het dashboard. */
@Composable
fun MiniCard(item: MediaItem, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        ItemCover(item, Modifier.fillMaxWidth().height(170.dp).clip(MaterialTheme.shapes.medium))
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        trailing?.invoke()
    }
}

@Composable
fun RatingStars(rating: Int?, onChange: ((Int?) -> Unit)? = null, size: Int = 28) {
    Row(Modifier.semantics { contentDescription = "Beoordeling ${rating ?: 0} van 5" }) {
        for (i in 1..5) {
            val filled = (rating ?: 0) >= i
            val vector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder
            val tint = if (filled) Gold else MaterialTheme.colorScheme.outline
            if (onChange != null) {
                IconButton(onClick = { onChange(if (rating == i) null else i) }, modifier = Modifier.size((size + 12).dp)) {
                    Icon(vector, contentDescription = "$i ${if (i == 1) "ster" else "sterren"}", tint = tint, modifier = Modifier.size(size.dp))
                }
            } else {
                Icon(vector, contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
            }
        }
    }
}

/** Horizontaal scrollende rij filterchips. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit,
    allLabel: String? = "Alles",
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (allLabel != null) {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(allLabel) })
        }
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(if (selected == option) null else option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Er ging iets mis", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            message ?: "De gegevens konden niet worden geladen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Text("Opnieuw proberen") }
    }
}

/** Toont laad-, fout- of inhoudstoestand van een [Load]. */
@Composable
fun <T> LoadGate(state: Load<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        Load.Loading -> LoadingBox()
        is Load.Failed -> ErrorBox(state.message, onRetry)
        is Load.Ready -> content(state.value)
    }
}
