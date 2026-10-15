@file:OptIn(ExperimentalMaterial3Api::class)

package com.backlogzero.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.LibraryFilter
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.domain.Priority
import com.backlogzero.app.domain.filtered
import com.backlogzero.app.ui.ChipRow
import com.backlogzero.app.ui.EmptyState
import com.backlogzero.app.ui.LoadGate
import com.backlogzero.app.ui.PosterCard
import com.backlogzero.app.ui.appViewModel
import com.backlogzero.app.ui.loader
import com.backlogzero.app.ui.Load
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LibraryData(val all: List<MediaItem>, val shown: List<MediaItem>)

class LibraryViewModel(library: LibraryRepository, initialStatus: ItemStatus?) : ViewModel() {
    private val _filter = MutableStateFlow(LibraryFilter(status = initialStatus))
    val filter: StateFlow<LibraryFilter> = _filter

    private val data = loader {
        combine(library.observeItems(), _filter) { items, f -> LibraryData(items, items.filtered(f)) }
    }
    val state get() = data.state
    fun retry() = data.retry()

    fun update(transform: (LibraryFilter) -> LibraryFilter) = _filter.update(transform)
    fun clear() { _filter.value = LibraryFilter() }

    private fun <T> MutableStateFlow<T>.update(f: (T) -> T) { value = f(value) }
}

@Composable
fun LibraryScreen(initialStatus: ItemStatus?, onOpenItem: (String) -> Unit, onAdd: () -> Unit) {
    val vm = appViewModel(key = "library-${initialStatus?.key}") { LibraryViewModel(it.library, initialStatus) }
    val state by vm.state.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Bibliotheek", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            FilledIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Item toevoegen") }
        }
        val extraActive = listOf(filter.status, filter.priority).count { it != null } + if (filter.favoritesOnly) 1 else 0
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = filter.query,
            onValueChange = { q -> vm.update { it.copy(query = q) } },
            placeholder = { Text("Zoek op titel") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { vm.update { it.copy(query = "") } }) { Icon(Icons.Filled.Clear, "Zoekopdracht wissen") }
                    }
                    IconButton(onClick = { showFilters = true }) {
                        BadgedBox(badge = { if (extraActive > 0) Badge { Text(extraActive.toString()) } }) {
                            Icon(Icons.Filled.Tune, contentDescription = "Meer filters")
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        ChipRow(
            MediaType.entries, filter.type, { it.plural }, { t -> vm.update { it.copy(type = t) } },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))

        LoadGate(state, vm::retry) { data ->
            when {
                data.all.isEmpty() -> EmptyState(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Nog niets in je bibliotheek",
                    text = "Voeg je eerste game, film, serie of boek toe.",
                    actionLabel = "Item toevoegen",
                    onAction = onAdd,
                )
                data.shown.isEmpty() -> EmptyState(
                    icon = Icons.Filled.FilterAltOff,
                    title = "Niets gevonden",
                    text = "Er is geen item dat bij deze zoekopdracht en filters past.",
                    actionLabel = "Filters wissen",
                    onAction = vm::clear,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "${data.shown.size} ${if (data.shown.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(data.shown, key = { it.id }) { item -> PosterCard(item, onClick = { onOpenItem(item.id) }) }
                }
            }
        }
    if (showFilters) FilterSheet(filter, vm::update, vm::clear) { showFilters = false }
    }
}

@Composable
private fun FilterSheet(filter: LibraryFilter, update: ((LibraryFilter) -> LibraryFilter) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChipRow(ItemStatus.entries, filter.status, { it.label }, { s -> update { it.copy(status = s) } })
            Text("Prioriteit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChipRow(Priority.entries, filter.priority, { it.label }, { p -> update { it.copy(priority = p) } })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Alleen favorieten", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = filter.favoritesOnly, onCheckedChange = { f -> update { it.copy(favoritesOnly = f) } })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Alles wissen") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Klaar") }
            }
        }
    }
}
