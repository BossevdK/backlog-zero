package com.backlogzero.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.backlogzero.app.BacklogZeroApp
import com.backlogzero.app.data.SteamException
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Steam-bibliotheek ophalen. Games die je daar hebt komen bij het online zoeken van games bovenaan
 * en krijgen een markering; ze komen niet vanzelf in je backlog.
 */
@Composable
fun SteamSection() {
    val steam = (LocalContext.current.applicationContext as BacklogZeroApp).container.steam
    val scope = rememberCoroutineScope()
    val count by steam.observeCount().collectAsStateWithLifecycle(initialValue = 0)

    var steamId by remember { mutableStateOf(steam.savedSteamId()) }
    var apiKey by remember { mutableStateOf(steam.savedApiKey()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Steam-bibliotheek", style = MaterialTheme.typography.titleSmall)
        Text(
            if (count > 0) "$count games geladen. Bij het zoeken naar games staan ze bovenaan, met een markering."
            else "Haal je games op, dan zie je bij het zoeken meteen welke je al hebt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = steamId, onValueChange = { steamId = it },
            label = { Text("Steam-ID of profielnaam") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it },
            label = { Text("Steam API-sleutel") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = {
                Text("Vraag je eigen gratis sleutel aan op steamcommunity.com/dev/apikey. Zet 'Gamedetails' in je Steam-privacyinstellingen op openbaar.")
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = {
                    busy = true
                    status = "Bezig met ophalen…"
                    scope.launch {
                        status = try {
                            val n = steam.sync(steamId, apiKey)
                            "Klaar: $n games opgehaald."
                        } catch (e: SteamException) {
                            e.message
                        } catch (e: IOException) {
                            "Ophalen lukt nu niet. Controleer je verbinding."
                        } catch (e: Exception) {
                            "Er ging iets mis bij het ophalen."
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(if (count > 0) "Opnieuw ophalen" else "Games ophalen") }
            if (count > 0) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            steam.clear()
                            steamId = ""
                            apiKey = ""
                            status = "Steam-gegevens verwijderd."
                        }
                    },
                ) { Text("Wissen") }
            }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
