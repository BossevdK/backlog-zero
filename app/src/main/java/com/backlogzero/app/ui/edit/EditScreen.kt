@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.backlogzero.app.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.data.CoverStorage
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.data.MetadataSearch
import com.backlogzero.app.data.SearchResult
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.util.UUID

class EditViewModel(
    private val library: LibraryRepository,
    private val covers: CoverStorage,
    private val metadata: MetadataSearch,
    private val itemId: String?,
) : ViewModel() {
    var loaded by mutableStateOf(itemId == null)
        private set
    var notFound by mutableStateOf(false)
        private set

    var type by mutableStateOf(MediaType.GAME)
        private set
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
    /** Omslag van een online zoekresultaat; wordt pas bij opslaan opgehaald. */
    var pendingCoverUrl by mutableStateOf<String?>(null)
        private set

    var searchQuery by mutableStateOf("")
        private set
    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    private var searchJob: Job? = null

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
    val hasCover get() = pendingCover != null || pendingCoverUrl != null || existingCover != null
    val searchAvailable get() = metadata.isAvailable(type)

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

    fun selectType(newType: MediaType) {
        type = newType
        runSearch()
    }

    fun onSearchQuery(query: String) {
        searchQuery = query
        runSearch()
    }

    private fun runSearch() {
        searchJob?.cancel()
        searchError = null
        if (searchQuery.trim().length < 2 || !metadata.isAvailable(type)) {
            results = emptyList()
            searching = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // wachten tot je uitgetypt bent
            searching = true
            try {
                results = metadata.search(type, searchQuery)
            } catch (e: IOException) {
                results = emptyList()
                searchError = "Zoeken lukt nu niet. Controleer je verbinding."
            } catch (e: JSONException) {
                results = emptyList()
                searchError = "Zoeken lukt nu niet."
            } finally {
                searching = false
            }
        }
    }

    /** Vult de velden in met het gekozen resultaat; wat je al had ingevuld wordt overschreven. */
    fun pickResult(result: SearchResult) {
        searchJob?.cancel()
        searching = false
        results = emptyList()
        searchQuery = ""
        viewModelScope.launch {
            val full = metadata.withDetails(type, result)
            title = full.title
            creator = full.creator
            full.year?.let { year = it.toString() }
            if (full.platform.isNotEmpty()) platform = full.platform
            pendingCover = null
            pendingCoverUrl = full.coverUrl
        }
    }

    fun pickCover(uri: Uri?) {
        if (uri != null) {
            pendingCover = uri
            pendingCoverUrl = null
        }
    }

    fun removeCover() {
        pendingCover = null
        pendingCoverUrl = null
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
                val newCover = when {
                    pendingCover != null -> covers.import(pendingCover!!)
                    // Lukt het ophalen van een online omslag niet, dan sla je het item gewoon zonder nieuwe omslag op.
                    pendingCoverUrl != null -> runCatching { covers.importUrl(pendingCoverUrl!!) }.getOrNull() ?: existingCover
                    else -> existingCover
                }
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
    val vm = appViewModel(key = "edit-${itemId ?: "new"}") { EditViewModel(it.library, it.covers, it.metadata, itemId) }
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
                        val model: Any? = vm.pendingCover ?: vm.pendingCoverUrl ?: vm.existingCover?.let { CoverStorage.file(context, it) }
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
                        ChipRow(MediaType.entries, vm.type, { it.label }, { t -> t?.let { vm.selectType(it) } }, allLabel = null)
                    }

                    if (vm.isNew) SearchSection(vm)

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
private fun SearchSection(vm: EditViewModel) {
    // Het toetsenbord neemt de onderste helft van het scherm in; scroll de zoekresultaten dus in beeld zodra ze er zijn.
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(vm.results) {
        if (vm.results.isNotEmpty()) {
            delay(100) // wachten tot de lijst getekend is
            requester.bringIntoView()
        }
    }
    Column(Modifier.bringIntoViewRequester(requester), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    OutlinedTextField(
        value = vm.searchQuery, onValueChange = vm::onSearchQuery,
        label = { Text("Zoek ${vm.type.label.lowercase()} online") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = { if (vm.searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
        supportingText = {
            Text(
                when {
                    !vm.searchAvailable -> "Online zoeken voor ${vm.type.plural.lowercase()} staat uit: er is geen API-sleutel ingebouwd. Vul de velden zelf in."
                    vm.searchError != null -> vm.searchError.orEmpty()
                    vm.searchQuery.trim().length >= 2 && !vm.searching && vm.results.isEmpty() -> "Niets gevonden."
                    else -> "Kies een resultaat om titel, maker, jaar en omslag in te vullen."
                },
            )
        },
        enabled = vm.searchAvailable,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    vm.results.forEach { r ->
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { vm.pickResult(r) }.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(r.title, vm.type, r.coverUrl, Modifier.width(40.dp).height(60.dp).clip(MaterialTheme.shapes.small))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(r.creator.ifEmpty { null }, r.year?.toString(), r.platform.ifEmpty { null }).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
