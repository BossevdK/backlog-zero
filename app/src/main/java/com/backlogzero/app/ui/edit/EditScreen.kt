@file:OptIn(ExperimentalMaterial3Api::class)

package com.backlogzero.app.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.data.CoverStorage
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.domain.Priority
import com.backlogzero.app.domain.withStatus
import com.backlogzero.app.ui.ChipRow
import com.backlogzero.app.ui.Cover
import com.backlogzero.app.ui.EmptyState
import com.backlogzero.app.ui.LoadingBox
import com.backlogzero.app.ui.RatingStars
import com.backlogzero.app.ui.appViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class EditViewModel(
    private val library: LibraryRepository,
    private val covers: CoverStorage,
    private val itemId: String?,
) : ViewModel() {
    var loaded by mutableStateOf(itemId == null)
        private set
    var notFound by mutableStateOf(false)
        private set

    var type by mutableStateOf(MediaType.GAME)
    var title by mutableStateOf("")
    var creator by mutableStateOf("")
    var platform by mutableStateOf("")
    var year by mutableStateOf("")
    var notes by mutableStateOf("")
    var status by mutableStateOf(ItemStatus.BACKLOG)
    var priority by mutableStateOf(Priority.MEDIUM)
    var favorite by mutableStateOf(false)
    var rating by mutableStateOf<Int?>(null)

    var existingCover by mutableStateOf<String?>(null)
        private set
    var pendingCover by mutableStateOf<Uri?>(null)
        private set

    var titleError by mutableStateOf<String?>(null)
        private set
    var yearError by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    private var original: MediaItem? = null

    val isNew get() = itemId == null
    val hasCover get() = pendingCover != null || existingCover != null

    init {
        if (itemId != null) {
            viewModelScope.launch {
                val item = library.observeItem(itemId).first()
                if (item == null) {
                    notFound = true
                } else {
                    original = item
                    type = item.type; title = item.title; creator = item.creator; platform = item.platform
                    year = item.releaseYear?.toString().orEmpty(); notes = item.notes
                    status = item.status; priority = item.priority; favorite = item.favorite; rating = item.rating
                    existingCover = item.coverFile
                }
                loaded = true
            }
        }
    }

    fun pickCover(uri: Uri?) {
        if (uri != null) pendingCover = uri
    }

    fun removeCover() {
        pendingCover = null
        existingCover = null
    }

    fun save(onDone: (String) -> Unit) {
        if (saving) return
        val cleanTitle = title.trim()
        val cleanYear = year.trim()
        val yearValue = cleanYear.toIntOrNull()
        titleError = if (cleanTitle.isEmpty()) "Vul een titel in." else null
        yearError = if (cleanYear.isNotEmpty() && (yearValue == null || yearValue !in 1000..2200)) "Vul een geldig jaartal in." else null
        if (titleError != null || yearError != null) return

        saving = true
        error = null
        viewModelScope.launch {
            try {
                val originalCover = original?.coverFile
                val newCover = pendingCover?.let { covers.import(it) } ?: existingCover
                val now = System.currentTimeMillis()
                val base = original ?: MediaItem(id = UUID.randomUUID().toString(), type = type, title = cleanTitle, createdAt = now, updatedAt = now)
                val item = base.copy(
                    type = type,
                    title = cleanTitle,
                    creator = creator.trim(),
                    platform = platform.trim(),
                    releaseYear = yearValue,
                    coverFile = newCover,
                    notes = notes.trim(),
                    rating = rating,
                    priority = priority,
                    favorite = favorite,
                    updatedAt = now,
                ).withStatus(status, now)
                library.save(item)
                if (originalCover != null && originalCover != newCover) covers.delete(originalCover)
                onDone(item.id)
            } catch (e: Exception) {
                error = "Opslaan is niet gelukt. Probeer het opnieuw."
            } finally {
                saving = false
            }
        }
    }
}

@Composable
fun EditScreen(itemId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val vm = appViewModel(key = "edit-${itemId ?: "new"}") { EditViewModel(it.library, it.covers, itemId) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { vm.pickCover(it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "Nieuw item" else "Item bewerken") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                actions = {
                    TextButton(onClick = { vm.save(onSaved) }, enabled = vm.loaded && !vm.saving) { Text("Opslaan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vm.notFound -> EmptyState(Icons.Filled.SearchOff, "Item niet gevonden", "Dit item bestaat niet meer.", actionLabel = "Terug", onAction = onBack)
                !vm.loaded -> LoadingBox()
                else -> Column(
                    Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                        val model: Any? = vm.pendingCover ?: vm.existingCover?.let { CoverStorage.file(context, it) }
                        Cover(
                            vm.title.ifBlank { "Nieuw" }, vm.type, model,
                            Modifier.width(110.dp).height(160.dp).clip(MaterialTheme.shapes.medium),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (vm.hasCover) "Andere omslag" else "Omslag kiezen")
                            }
                            if (vm.hasCover) {
                                OutlinedButton(onClick = vm::removeCover) { Text("Omslag verwijderen") }
                            }
                        }
                    }

                    Field("Categorie") {
                        ChipRow(MediaType.entries, vm.type, { it.label }, { t -> t?.let { vm.type = it } }, allLabel = null)
                    }

                    OutlinedTextField(
                        value = vm.title, onValueChange = { vm.title = it }, label = { Text("Titel") },
                        isError = vm.titleError != null, supportingText = vm.titleError?.let { e -> ({ Text(e) }) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = vm.creator, onValueChange = { vm.creator = it }, label = { Text(vm.type.creatorLabel) },
                        singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = vm.platform, onValueChange = { vm.platform = it }, label = { Text(vm.type.platformLabel) },
                            singleLine = true, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = vm.year, onValueChange = { vm.year = it.filter(Char::isDigit).take(4) }, label = { Text("Jaar") },
                            isError = vm.yearError != null, supportingText = vm.yearError?.let { e -> ({ Text(e) }) },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp),
                        )
                    }

                    Field("Status") {
                        ChipRow(ItemStatus.entries, vm.status, { it.label }, { s -> s?.let { vm.status = it } }, allLabel = null)
                    }
                    Field("Prioriteit") {
                        ChipRow(Priority.entries, vm.priority, { it.label }, { p -> p?.let { vm.priority = it } }, allLabel = null)
                    }
                    Field("Jouw beoordeling") { RatingStars(vm.rating, onChange = { vm.rating = it }) }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Favoriet", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = vm.favorite, onCheckedChange = { vm.favorite = it })
                    }

                    OutlinedTextField(
                        value = vm.notes, onValueChange = { vm.notes = it }, label = { Text("Notities") },
                        minLines = 3, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { vm.save(onSaved) }, enabled = !vm.saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (vm.saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(if (vm.isNew) "Toevoegen" else "Opslaan")
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}
