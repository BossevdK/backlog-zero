package com.backlogzero.app.data

import com.backlogzero.app.BuildConfig
import com.backlogzero.app.domain.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Eén zoekresultaat van een online bron. `detailId` dient om de maker/studio pas bij kiezen op te halen. */
data class SearchResult(
    val title: String,
    val creator: String = "",
    val year: Int? = null,
    val platform: String = "",
    val coverUrl: String? = null,
    val detailId: String? = null,
)

/** Zoekt titels online, zodat titel, maker, jaar en omslag niet met de hand ingevuld hoeven te worden. */
interface MetadataSearch {
    /** Is er voor dit type een bron beschikbaar (sleutel ingesteld)? */
    fun isAvailable(type: MediaType): Boolean
    suspend fun search(type: MediaType, query: String): List<SearchResult>
    /** Vult de maker/studio aan voor een gekozen resultaat; bij een fout blijft het resultaat ongewijzigd. */
    suspend fun withDetails(type: MediaType, result: SearchResult): SearchResult
}

/** Boeken: Open Library (geen sleutel). Films en series: TMDB. Games: RAWG. De laatste twee vragen een gratis sleutel. */
class OnlineMetadataSearch(
    private val tmdbKey: String = BuildConfig.TMDB_API_KEY,
    private val rawgKey: String = BuildConfig.RAWG_API_KEY,
) : MetadataSearch {

    override fun isAvailable(type: MediaType) = when (type) {
        MediaType.BOOK -> true
        MediaType.MOVIE, MediaType.SERIES -> tmdbKey.isNotBlank()
        MediaType.GAME -> rawgKey.isNotBlank()
    }

    override suspend fun search(type: MediaType, query: String): List<SearchResult> {
        val q = query.trim()
        if (q.length < 2 || !isAvailable(type)) return emptyList()
        val enc = URLEncoder.encode(q, "UTF-8")
        return when (type) {
            MediaType.BOOK -> MetadataParsers.openLibrary(
                get("https://openlibrary.org/search.json?q=$enc&limit=10&fields=title,author_name,first_publish_year,cover_i"),
            )
            MediaType.MOVIE -> MetadataParsers.tmdb(
                get("https://api.themoviedb.org/3/search/movie?api_key=$tmdbKey&language=nl-NL&include_adult=false&query=$enc"), tv = false,
            )
            MediaType.SERIES -> MetadataParsers.tmdb(
                get("https://api.themoviedb.org/3/search/tv?api_key=$tmdbKey&language=nl-NL&include_adult=false&query=$enc"), tv = true,
            )
            MediaType.GAME -> MetadataParsers.rawg(get("https://api.rawg.io/api/games?key=$rawgKey&page_size=10&search=$enc"))
        }
    }

    override suspend fun withDetails(type: MediaType, result: SearchResult): SearchResult {
        val id = result.detailId ?: return result
        return try {
            when (type) {
                MediaType.MOVIE -> result.copy(
                    creator = MetadataParsers.director(get("https://api.themoviedb.org/3/movie/$id/credits?api_key=$tmdbKey")),
                )
                MediaType.SERIES -> result.copy(
                    creator = MetadataParsers.creators(get("https://api.themoviedb.org/3/tv/$id?api_key=$tmdbKey&language=nl-NL")),
                )
                MediaType.GAME -> result.copy(
                    creator = MetadataParsers.developers(get("https://api.rawg.io/api/games/$id?key=$rawgKey")),
                )
                MediaType.BOOK -> result
            }
        } catch (e: IOException) {
            result
        }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "BacklogZero")
        try {
            if (conn.responseCode != 200) throw IOException("Zoeken mislukt (${conn.responseCode})")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

/** Pure JSON-verwerking, los van het netwerk zodat het te testen is. */
object MetadataParsers {

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun yearOf(date: String?): Int? = date?.take(4)?.toIntOrNull()

    fun openLibrary(json: String): List<SearchResult> =
        JSONObject(json).optJSONArray("docs").objects().mapNotNull { d ->
            val title = d.optString("title").trim().ifEmpty { return@mapNotNull null }
            val authors = d.optJSONArray("author_name")
            val cover = d.optInt("cover_i", 0).takeIf { it > 0 }
            SearchResult(
                title = title,
                creator = if (authors != null && authors.length() > 0) authors.optString(0) else "",
                year = d.optInt("first_publish_year", 0).takeIf { it > 0 },
                coverUrl = cover?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" },
            )
        }

    fun tmdb(json: String, tv: Boolean): List<SearchResult> =
        JSONObject(json).optJSONArray("results").objects().mapNotNull { r ->
            val title = r.optString(if (tv) "name" else "title").trim().ifEmpty { return@mapNotNull null }
            val poster = r.optString("poster_path").takeIf { it.isNotEmpty() && it != "null" }
            SearchResult(
                title = title,
                year = yearOf(r.optString(if (tv) "first_air_date" else "release_date")),
                coverUrl = poster?.let { "https://image.tmdb.org/t/p/w500$it" },
                detailId = r.optInt("id", 0).takeIf { it > 0 }?.toString(),
            )
        }

    fun rawg(json: String): List<SearchResult> =
        JSONObject(json).optJSONArray("results").objects().mapNotNull { r ->
            val title = r.optString("name").trim().ifEmpty { return@mapNotNull null }
            val platforms = r.optJSONArray("platforms").objects()
                .mapNotNull { it.optJSONObject("platform")?.optString("name")?.takeIf(String::isNotEmpty) }
            SearchResult(
                title = title,
                year = yearOf(r.optString("released")),
                platform = platforms.take(3).joinToString(", "),
                coverUrl = r.optString("background_image").takeIf { it.startsWith("https://") },
                detailId = r.optInt("id", 0).takeIf { it > 0 }?.toString(),
            )
        }

    fun director(json: String): String =
        JSONObject(json).optJSONArray("crew").objects()
            .filter { it.optString("job") == "Director" }.map { it.optString("name") }
            .distinct().take(2).joinToString(", ")

    fun creators(json: String): String =
        JSONObject(json).optJSONArray("created_by").objects().map { it.optString("name") }
            .filter(String::isNotEmpty).take(2).joinToString(", ")

    fun developers(json: String): String =
        JSONObject(json).optJSONArray("developers").objects().map { it.optString("name") }
            .filter(String::isNotEmpty).take(2).joinToString(", ")
}
