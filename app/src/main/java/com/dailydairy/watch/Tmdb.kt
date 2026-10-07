package com.dailydairy.watch

import com.dailydairy.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TitleHit(
    val tmdbId: Int,
    val kind: String,
    val title: String,
    val year: String,
    val poster: String,
    val overview: String,
)

data class CastMember(val name: String, val role: String, val photo: String)

data class TitleDetail(
    val hit: TitleHit,
    val imdb: String,
    val cast: List<CastMember>,
)

private const val IMAGE = "https://image.tmdb.org/t/p/w342"

object Tmdb {
    val ready: Boolean get() = BuildConfig.TMDB_KEY.isNotBlank()

    fun search(query: String): List<TitleHit> {
        val key = BuildConfig.TMDB_KEY
        if (key.isBlank() || query.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val json = get(
            "https://api.themoviedb.org/3/search/multi?api_key=$key&language=tr-TR&include_adult=false&query=$encoded",
        )
        val results = json.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<TitleHit>(results.length())
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val kind = item.optString("media_type")
            if (kind != KIND_MOVIE && kind != KIND_TV) continue
            val name = if (kind == KIND_MOVIE) item.optString("title") else item.optString("name")
            if (name.isBlank()) continue
            val date = if (kind == KIND_MOVIE) item.optString("release_date") else item.optString("first_air_date")
            out += TitleHit(
                tmdbId = item.optInt("id"),
                kind = kind,
                title = name,
                year = date.take(4),
                poster = posterOf(item.optString("poster_path")),
                overview = item.optString("overview"),
            )
            if (out.size == 20) break
        }
        return out
    }

    fun detail(tmdbId: Int, kind: String): TitleDetail? {
        val key = BuildConfig.TMDB_KEY
        if (key.isBlank()) return null
        val json = get(
            "https://api.themoviedb.org/3/$kind/$tmdbId?api_key=$key&language=tr-TR&append_to_response=credits",
        )
        if (json.length() == 0) return null
        val name = if (kind == KIND_MOVIE) json.optString("title") else json.optString("name")
        if (name.isBlank()) return null
        val date = if (kind == KIND_MOVIE) json.optString("release_date") else json.optString("first_air_date")
        val hit = TitleHit(
            tmdbId = tmdbId,
            kind = kind,
            title = name,
            year = date.take(4),
            poster = posterOf(json.optString("poster_path")),
            overview = json.optString("overview"),
        )
        val credits = json.optJSONObject("credits")?.optJSONArray("cast")
        val cast = ArrayList<CastMember>()
        if (credits != null) {
            val limit = minOf(credits.length(), 12)
            for (i in 0 until limit) {
                val person = credits.optJSONObject(i) ?: continue
                val who = person.optString("name")
                if (who.isBlank()) continue
                cast += CastMember(
                    name = who,
                    role = person.optString("character"),
                    photo = posterOf(person.optString("profile_path")),
                )
            }
        }
        return TitleDetail(hit, json.optString("imdb_id"), cast)
    }

    private fun posterOf(path: String): String =
        if (path.isBlank() || path == "null") "" else IMAGE + path

    private fun get(address: String): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}

object Omdb {
    val ready: Boolean get() = BuildConfig.OMDB_KEY.isNotBlank()

    fun rating(imdbId: String): String {
        val key = BuildConfig.OMDB_KEY
        if (key.isBlank() || imdbId.isBlank()) return ""
        val json = get("https://www.omdbapi.com/?apikey=$key&i=$imdbId")
        val value = json.optString("imdbRating")
        return if (value.isBlank() || value == "N/A") "" else value
    }

    private fun get(address: String): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val text = connection.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (_: Exception) {
            JSONObject()
        } finally {
            connection.disconnect()
        }
    }
}
