package com.backlogzero.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.data.AuthRepository
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.User
import com.backlogzero.app.ui.LoadGate
import com.backlogzero.app.ui.appViewModel
import com.backlogzero.app.ui.loader
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileData(val user: User?, val itemCount: Int)

class ProfileViewModel(private val auth: AuthRepository, private val library: LibraryRepository) : ViewModel() {
    private val data = loader { combine(auth.observeCurrentUser(), library.observeItems()) { u, i -> ProfileData(u, i.size) } }
    val state get() = data.state
    fun retry() = data.retry()

    fun logout() = auth.logout()

    fun deleteAccount() {
        viewModelScope.launch {
            library.purgeCovers()
            auth.deleteAccount()
        }
    }
}

@Composable
fun ProfileScreen() {
    val vm = appViewModel { ProfileViewModel(it.auth, it.library) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Profiel", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp))
        LoadGate(state, vm::retry) { data ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(data.user?.name?.firstOrNull()?.uppercase().orEmpty(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(data.user?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    Text(data.user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("${data.itemCount} items in je bibliotheek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    "Je gegevens staan alleen op dit toestel en zijn niet toegankelijk voor andere accounts. Synchronisatie tussen apparaten is nog niet beschikbaar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Uitloggen") }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Account en alle gegevens verwijderen", color = MaterialTheme.colorScheme.error)
                }
                Text("Backlog Zero $version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Account verwijderen?") },
            text = { Text("Je account en alle items worden definitief van dit toestel verwijderd. Dit kan niet ongedaan worden gemaakt.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAccount() }) { Text("Definitief verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") } },
        )
    }
}
