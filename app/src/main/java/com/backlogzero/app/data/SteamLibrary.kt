package com.backlogzero.app.data

import android.content.Context
import com.backlogzero.app.data.db.SteamDao
import com.backlogzero.app.data.db.SteamGameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Fout met een tekst die direct aan de gebruiker getoond mag worden. */
class SteamException(message: String) : Exception(message)

data class SteamGame(val appId: Long, val name: String)

/** Pure JSON-verwerking van de Steam Web API, los van het netwerk zodat het te testen is. */
object SteamParsers {
    /**
     * Geeft de games, een lege lijst als het account echt geen games heeft, en `null` als Steam
     * niets teruggeeft (meestal: "Game details" staat niet op openbaar).
     */
    fun ownedGames(json: String): List<SteamGame>? {
        val response = JSONObject(json).optJSONObject("response") ?: return null
        val games = response.optJSONArray("games")
        if (games == null) return if (response.optInt("game_count", -1) == 0) emptyList() else null
        return (0 until games.length()).mapNotNull { i ->
            val g = games.optJSONObject(i) ?: return@mapNotNull null
            val name = g.optString("name").trim().ifEmpty { return@mapNotNull null }
            val id = g.optLong("appid", 0).takeIf { it > 0 } ?: return@mapNotNull null
            SteamGame(id, name)
        }
    }

    /** Het 64-bit Steam-ID bij een profielnaam, of null als die niet bestaat. */
    fun vanity(json: String): String? {
        val response = JSONObject(json).optJSONObject("response") ?: return null
        return if (response.optInt("success") == 1) response.optString("steamid").ifEmpty { null } else null
    }

    fun isSteamId(input: String) = input.length == 17 && input.all(Char::isDigit)
}

/**
 * Je eigen Steam-bibliotheek, alleen om online zoekresultaten te markeren en bovenaan te zetten.
 * De games komen niet automatisch in je backlog. Steam-ID en API-sleutel staan alleen in de privé-opslag van de app.
 */
class SteamLibrary(
    context: Context,
    private val dao: SteamDao,
    private val auth: AuthRepository,
) {
    private val prefs = context.getSharedPreferences("steam", Context.MODE_PRIVATE)

    private fun userId(): String = auth.currentUserId.value ?: throw IllegalStateException("Niet ingelogd.")

    fun savedSteamId(): String = auth.currentUserId.value?.let { prefs.getString("id_$it", null) }.orEmpty()
    fun savedApiKey(): String = auth.currentUserId.value?.let { prefs.getString("key_$it", null) }.orEmpty()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCount(): Flow<Int> =
        auth.currentUserId.flatMapLatest { id -> if (id == null) flowOf(0) else dao.observeCount(id) }

    /** Genormaliseerde titels van je games, om mee te vergelijken. Leeg als je niets hebt opgehaald. */
    suspend fun ownedNormalizedTitles(): Set<String> {
        val id = auth.currentUserId.value ?: return emptySet()
        return dao.normalizedNames(id).toSet()
    }

    /** Haalt je games op en vervangt de vorige lijst. Geeft het aantal games. */
    suspend fun sync(steamIdOrName: String, apiKey: String): Int {
        val input = steamIdOrName.trim().substringAfterLast("/profiles/").substringAfterLast("/id/").trim('/')
        val key = apiKey.trim()
        if (input.isEmpty()) throw SteamException("Vul je Steam-ID of profielnaam in.")
        if (key.length < 16) throw SteamException("Vul je Steam API-sleutel in.")
        val user = userId()

        val steamId = if (SteamParsers.isSteamId(input)) input else {
            val json = get("https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=${enc(key)}&vanityurl=${enc(input)}")
            SteamParsers.vanity(json) ?: throw SteamException("Die profielnaam is niet gevonden. Probeer je 17-cijferige Steam-ID.")
        }
        val json = get("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=${enc(key)}&steamid=$steamId&include_appinfo=1&format=json")
        val games = SteamParsers.ownedGames(json)
            ?: throw SteamException("Steam geeft geen games terug. Zet in je Steam-privacyinstellingen 'Gamedetails' op openbaar.")

        dao.replaceAll(user, games.map { SteamGameEntity(user, it.appId, it.name, normalizeTitle(it.name)) })
        prefs.edit().putString("id_$user", steamId).putString("key_$user", key).apply()
        return games.size
    }

    /** Verwijdert de opgehaalde games en de bewaarde gegevens. */
    suspend fun clear() {
        val user = userId()
        dao.clear(user)
        prefs.edit().remove("id_$user").remove("key_$user").apply()
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("User-Agent", "BacklogZero")
        try {
            when (val code = conn.responseCode) {
                200 -> conn.inputStream.bufferedReader().use { it.readText() }
                401, 403 -> throw SteamException("Steam weigert de API-sleutel. Controleer of die klopt.")
                400 -> throw SteamException("Steam herkent dit Steam-ID niet.")
                else -> throw IOException("Steam antwoordde met $code")
            }
        } finally {
            conn.disconnect()
        }
    }
}
