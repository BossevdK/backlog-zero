@file:OptIn(ExperimentalMaterial3Api::class)

package com.backlogzero.app.ui.pick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.domain.PickCriteria
import com.backlogzero.app.domain.Recommender
import com.backlogzero.app.domain.withStatus
import com.backlogzero.app.ui.ChipRow
import com.backlogzero.app.ui.EmptyState
import com.backlogzero.app.ui.ItemCover
import com.backlogzero.app.ui.LoadGate
import com.backlogzero.app.ui.StatusBadge
import com.backlogzero.app.ui.appViewModel
import com.backlogzero.app.ui.loader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class PickState(val criteria: PickCriteria, val picked: MediaItem?, val hasCandidates: Boolean)

class PickViewModel(
    private val library: LibraryRepository,
    private val recommender: Recommender,
) : ViewModel() {
    private val criteria = MutableStateFlow(PickCriteria())
    private val pickedId = MutableStateFlow<String?>(null)

    private val data = loader {
        combine(library.observeItems(), criteria, pickedId) { items, c, id ->
            val picked = items.firstOrNull { it.id == id && it.status != ItemStatus.COMPLETED }
            PickState(c, picked, hasCandidates = recommender.pick(items, c) != null)
        }
    }
    val state get() = data.state
    fun retry() = data.retry()

    init {
        reroll()
    }

    fun reroll() {
        viewModelScope.launch {
            val items = library.observeItems().first()
            pickedId.value = recommender.pick(items, criteria.value, exclude = setOfNotNull(pickedId.value))?.id
        }
    }

    fun setType(type: MediaType?) {
        criteria.value = criteria.value.copy(type = type)
        reroll()
    }

    fun setIncludeInProgress(include: Boolean) {
        criteria.value = criteria.value.copy(includeInProgress = include)
        reroll()
    }

    fun start(item: MediaItem, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (item.status != ItemStatus.IN_PROGRESS) library.save(item.withStatus(ItemStatus.IN_PROGRESS, now))
            onDone(item.id)
        }
    }
}

@Composable
fun PickScreen(onBack: () -> Unit, onOpenItem: (String) -> Unit) {
    val vm = appViewModel { PickViewModel(it.library, it.recommender) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Pick for me") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LoadGate(state, vm::retry) { pick ->
                ChipRow(
                    MediaType.entries, pick.criteria.type, { it.plural }, vm::setType,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    leading = null,
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = pick.criteria.includeInProgress,
                        onClick = { vm.setIncludeInProgress(!pick.criteria.includeInProgress) },
                        label = { Text("Ook items waar ik mee bezig ben") },
                    )
                }
                Spacer(Modifier.height(12.dp))

                val picked = pick.picked
                if (picked == null) {
                    EmptyState(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Niets om uit te kiezen",
                        text = if (pick.hasCandidates) "Even geduld, er wordt gekozen." else "Er zijn geen onafgeronde items die bij deze keuze passen.",
                    )
                } else {
                    PickResult(picked, vm, onOpenItem)
                }
            }
        }
    }
}

@Composable
private fun PickResult(item: MediaItem, vm: PickViewModel, onOpenItem: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ItemCover(item, Modifier.width(200.dp).height(290.dp).clip(MaterialTheme.shapes.large))
        Spacer(Modifier.height(20.dp))
        Text(item.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(item.type.label, item.releaseYear?.toString(), item.creator.ifBlank { null }).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        StatusBadge(item.status)
        Spacer(Modifier.height(8.dp))
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - item.createdAt)
        Text(
            buildString {
                append("Prioriteit: ${item.priority.label.lowercase()}")
                if (days >= 7) append(" · staat al $days dagen in je lijst")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.start(item, onOpenItem) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Begin hiermee")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = vm::reroll, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Casino, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Kies iets anders")
        }
    }
}
