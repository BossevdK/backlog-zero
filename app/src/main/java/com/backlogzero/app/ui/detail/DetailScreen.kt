@file:OptIn(ExperimentalMaterial3Api::class)

package com.backlogzero.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.Priority
import com.backlogzero.app.domain.withStatus
import com.backlogzero.app.ui.ChipRow
import com.backlogzero.app.ui.EmptyState
import com.backlogzero.app.ui.ItemCover
import com.backlogzero.app.ui.Load
import com.backlogzero.app.ui.LoadGate
import com.backlogzero.app.ui.RatingStars
import com.backlogzero.app.ui.StatusBadge
import com.backlogzero.app.ui.appViewModel
import com.backlogzero.app.ui.loader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class DetailViewModel(private val library: LibraryRepository, private val id: String) : ViewModel() {
    private val data = loader { library.observeItem(id) }
    val state get() = data.state
    fun retry() = data.retry()

    private fun change(transform: (MediaItem, Long) -> MediaItem) {
        viewModelScope.launch {
            val current = library.observeItem(id).first() ?: return@launch
            val now = System.currentTimeMillis()
            library.save(transform(current, now).copy(updatedAt = now))
        }
    }

    fun setStatus(status: ItemStatus) = change { item, now -> item.withStatus(status, now) }
    fun setPriority(priority: Priority) = change { item, _ -> item.copy(priority = priority) }
    fun setRating(rating: Int?) = change { item, _ -> item.copy(rating = rating) }
    fun toggleFavorite() = change { item, _ -> item.copy(favorite = !item.favorite) }
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            library.delete(id)
            onDone()
        }
    }
}

@Composable
fun DetailScreen(id: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val vm = appViewModel(key = "detail-$id") { DetailViewModel(it.library, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                actions = {
                    val item = (state as? Load.Ready<MediaItem?>)?.value
                    if (item != null) {
                        IconButton(onClick = vm::toggleFavorite) {
                            Icon(
                                if (item.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (item.favorite) "Verwijder uit favorieten" else "Maak favoriet",
                                tint = if (item.favorite) Color(0xFFFF7A9A) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { onEdit(item.id) }) { Icon(Icons.Filled.Edit, "Bewerken") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Verwijderen") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadGate(state, vm::retry) { item ->
                if (item == null) {
                    EmptyState(Icons.Filled.SearchOff, "Item niet gevonden", "Dit item bestaat niet meer.", actionLabel = "Terug", onAction = onBack)
                } else {
                    DetailContent(item, vm)
                }
            }
        }
    }

    val current = (state as? Load.Ready<MediaItem?>)?.value
    if (confirmDelete && current != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Item verwijderen?") },
            text = { Text("\"${current.title}\" wordt definitief uit je bibliotheek verwijderd.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) {
                    Text("Verwijderen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") } },
        )
    }
}

@Composable
private fun DetailContent(item: MediaItem, vm: DetailViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Box(Modifier.fillMaxWidth().height(260.dp)) {
            ItemCover(item, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to MaterialTheme.colorScheme.background)))
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(item.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            val meta = listOfNotNull(item.type.label, item.releaseYear?.toString(), item.creator.ifBlank { null }).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            StatusBadge(item.status)

            Spacer(Modifier.height(24.dp))
            Label("Status")
            ChipRow(ItemStatus.entries, item.status, { it.label }, { it?.let(vm::setStatus) }, allLabel = null)

            Spacer(Modifier.height(16.dp))
            Label("Prioriteit")
            ChipRow(Priority.entries, item.priority, { it.label }, { it?.let(vm::setPriority) }, allLabel = null)

            Spacer(Modifier.height(24.dp))
            Label("Jouw beoordeling")
            RatingStars(item.rating, onChange = vm::setRating)

            if (item.platform.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Label(item.type.platformLabel)
                Text(item.platform, style = MaterialTheme.typography.bodyLarge)
            }
            if (item.notes.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Label("Notities")
                Text(item.notes, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(24.dp))
            val dates = DateFormat.getDateInstance(DateFormat.MEDIUM)
            Text(
                buildString {
                    append("Toegevoegd op ${dates.format(Date(item.createdAt))}")
                    item.completedAt?.let { append(" · afgerond op ${dates.format(Date(it))}") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
}
