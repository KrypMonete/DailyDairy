package com.dailydairy.diary

import com.dailydairy.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GifHit(
    val id: String,
    val preview: String,
    val file: String,
)

object Giphy {
    val ready: Boolean get() = BuildConfig.GIPHY_KEY.isNotBlank()

    fun search(query: String): List<GifHit> {
        val key = BuildConfig.GIPHY_KEY
        if (key.isBlank()) return emptyList()
        val needle = query.trim()
        val path = if (needle.length < 2) "trending" else "search"
        val extra = if (needle.length < 2) "" else "&q=${URLEncoder.encode(needle, "UTF-8")}"
        val body = get(
            "https://api.giphy.com/v1/gifs/$path?api_key=$key&limit=24&rating=pg-13&lang=tr$extra",
        )
        return parse(body)
    }

    private fun parse(body: String): List<GifHit> {
        if (body.isBlank()) return emptyList()
        val data = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return emptyList()
        val found = ArrayList<GifHit>()
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val images = item.optJSONObject("images") ?: continue
            val preview = images.optJSONObject("fixed_width")?.optString("url").orEmpty()
                .ifBlank { images.optJSONObject("preview_gif")?.optString("url").orEmpty() }
            val file = images.optJSONObject("downsized")?.optString("url").orEmpty()
                .ifBlank { images.optJSONObject("original")?.optString("url").orEmpty() }
            if (preview.isBlank() || file.isBlank()) continue
            found += GifHit(item.optString("id"), preview, file)
        }
        return found
    }

    private fun get(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } catch (_: Exception) {
            ""
        } finally {
            connection.disconnect()
        }
    }
}
