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

data class HomeData(val user: User?, val items: List<MediaItem>) {
    val inProgress get() = items.filter { it.status == ItemStatus.IN_PROGRESS }
    val upNext
        get() = items.filter { it.status == ItemStatus.BACKLOG || it.status == ItemStatus.PAUSED }
            .sortedWith(compareByDescending<MediaItem> { it.priority.level }.thenBy { it.createdAt })
            .take(10)
    val recentlyCompleted
        get() = items.filter { it.status == ItemStatus.COMPLETED }
            .sortedByDescending { it.completedAt ?: it.updatedAt }.take(10)
    fun count(status: ItemStatus) = items.count { it.status == status }
}

class HomeViewModel(auth: AuthRepository, library: LibraryRepository) : ViewModel() {
    private val data = loader { combine(auth.observeCurrentUser(), library.observeItems()) { u, i -> HomeData(u, i) } }
    val state get() = data.state
    fun retry() = data.retry()
}

@Composable
fun HomeScreen(
    onOpenItem: (String) -> Unit,
    onAdd: () -> Unit,
    onPick: () -> Unit,
    onOpenLibrary: (ItemStatus) -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.auth, it.library) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LoadGate(state, vm::retry) { data ->
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hoi ${data.user?.name?.substringBefore(' ').orEmpty()}", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                if (data.items.isEmpty()) "Je bibliotheek is nog leeg" else "${data.count(ItemStatus.BACKLOG) + data.count(ItemStatus.PAUSED)} items wachten op jou",
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
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(ItemStatus.BACKLOG, ItemStatus.IN_PROGRESS, ItemStatus.COMPLETED).forEach { s ->
                                StatTile(s, data.count(s), Modifier.weight(1f)) { onOpenLibrary(s) }
                            }
                        }
                    }
                    item { PickCard(Modifier.padding(horizontal = 20.dp), onPick) }

                    if (data.inProgress.isNotEmpty()) {
                        item { ItemRow("Bezig", data.inProgress, onOpenItem) }
                    }
                    if (data.upNext.isNotEmpty()) {
                        item { ItemRow("Volgende in je lijst", data.upNext, onOpenItem) }
                    }
                    if (data.recentlyCompleted.isNotEmpty()) {
                        item { ItemRow("Recent afgerond", data.recentlyCompleted, onOpenItem) }
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
private fun PickCard(modifier: Modifier, onPick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)))
            .clickable(onClick = onPick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Pick for me", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text("Weet je niet wat je moet doen? Laat de app kiezen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
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
