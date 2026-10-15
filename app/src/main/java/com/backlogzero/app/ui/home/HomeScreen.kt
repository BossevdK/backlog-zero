package com.backlogzero.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.backlogzero.app.domain.HomeRows
import com.backlogzero.app.ui.ChipRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.backlogzero.app.data.AuthRepository
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.domain.Priority
import com.backlogzero.app.domain.User
import com.backlogzero.app.ui.EmptyState
import com.backlogzero.app.ui.LoadGate
import com.backlogzero.app.ui.MiniCard
import com.backlogzero.app.ui.SectionTitle
import com.backlogzero.app.ui.appViewModel
import com.backlogzero.app.ui.loader
import com.backlogzero.app.ui.theme.color
import kotlinx.coroutines.flow.combine

data class HomeData(val user: User?, val items: List<MediaItem>)

class HomeViewModel(auth: AuthRepository, library: LibraryRepository) : ViewModel() {
    private val data = loader { combine(auth.observeCurrentUser(), library.observeItems()) { u, i -> HomeData(u, i) } }
    val state get() = data.state
    fun retry() = data.retry()
}

@Composable
fun HomeScreen(
    onOpenItem: (String) -> Unit,
    onAdd: () -> Unit,
    onPick: (MediaType?) -> Unit,
    onOpenLibrary: (ItemStatus) -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.auth, it.library) }
    val state by vm.state.collectAsStateWithLifecycle()

    var typeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val type = typeKey?.let(MediaType::fromKey)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LoadGate(state, vm::retry) { data ->
            val rows = remember(data.items, type) { HomeRows.of(data.items, type) }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hoi ${data.user?.name?.substringBefore(' ').orEmpty()}", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                if (data.items.isEmpty()) "Je bibliotheek is nog leeg"
                                else "${(rows.counts[ItemStatus.BACKLOG] ?: 0) + (rows.counts[ItemStatus.PAUSED] ?: 0)} ${type?.plural?.lowercase() ?: "items"} wachten op jou",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Item toevoegen") }
                    }
                }

                if (data.items.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.LibraryAdd,
                            title = "Begin met je eerste item",
                            text = "Voeg een game, film, serie of boek toe en bouw je backlog op.",
                            actionLabel = "Item toevoegen",
                            onAction = onAdd,
                        )
                    }
                } else {
                    item {
                        ChipRow(
                            MediaType.entries, type, { it.plural }, { t -> typeKey = t?.key },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    item {
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(ItemStatus.BACKLOG, ItemStatus.IN_PROGRESS, ItemStatus.COMPLETED).forEach { s ->
                                StatTile(s, rows.counts[s] ?: 0, Modifier.weight(1f)) { onOpenLibrary(s) }
                            }
                        }
                    }
                    item { PickCard(Modifier.padding(horizontal = 20.dp), onPick) }

                    if (rows.inProgress.isNotEmpty()) item { ItemRow("Bezig", rows.inProgress, onOpenItem) }
                    if (rows.upNext.isNotEmpty()) item { ItemRow("Volgende uit je backlog", rows.upNext, onOpenItem) }
                    if (rows.recentlyAdded.isNotEmpty()) item { ItemRow("Recent toegevoegd", rows.recentlyAdded, onOpenItem) }
                    if (rows.recentlyCompleted.isNotEmpty()) item { ItemRow("Recent afgerond", rows.recentlyCompleted, onOpenItem) }
                    if (rows.total == 0) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.LibraryAdd,
                                title = "Niets in deze categorie",
                                text = "Voeg iets toe, of kies een andere categorie.",
                                actionLabel = "Item toevoegen",
                                onAction = onAdd,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(status: ItemStatus, count: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick).padding(14.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = status.color())
        Text(status.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PickCard(modifier: Modifier, onPick: (MediaType?) -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pick for me", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text("Laat de app iets voor je kiezen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickPill("Alles", Modifier.weight(1f)) { onPick(null) }
            MediaType.entries.forEach { type -> PickPill(type.plural, Modifier.weight(1f)) { onPick(type) } }
        }
    }
}


@Composable
private fun PickPill(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f))
            .clickable(onClick = onClick).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
}

@Composable
private fun ItemRow(title: String, items: List<MediaItem>, onOpen: (String) -> Unit) {
    Column {
        SectionTitle(title, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { item -> MiniCard(item) { onOpen(item.id) } }
        }
    }
}
