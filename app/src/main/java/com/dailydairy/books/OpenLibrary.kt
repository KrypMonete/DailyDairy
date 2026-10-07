package com.dailydairy.books

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class BookHit(
    val workKey: String,
    val title: String,
    val author: String,
    val year: String,
    val cover: String,
    val overview: String,
)

private const val AGENT = "DailyDairy/1.0 (personal book shelf)"
private const val TURKISH = "çğıöşüÇĞİÖŞÜ"
private const val FOREIGN = "àáâãäåæèéêëìíîïðñòóôõöøùúûýÿÀÁÂÃÄÅÆÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÝß"

object OpenLibrary {
    fun search(query: String): List<BookHit> {
        val needle = query.trim()
        if (needle.length < 2) return emptyList()
        val byAuthor = needle.startsWith("author:")
        val text = if (byAuthor) needle.removePrefix("author:").trim() else needle
        if (text.length < 2) return emptyList()
        val fields = "key,title,author_name,first_publish_year,cover_i,language"
        val address = if (byAuthor) {
            "https://openlibrary.org/search.json?author=${enc(text)}&limit=40&sort=editions&fields=$fields"
        } else {
            "https://openlibrary.org/search.json?q=${enc(text)}&limit=40&fields=$fields"
        }
        return parse(get(address), text)
    }

    fun description(workKey: String): String {
        val key = workKey.substringAfter("openlibrary.org", "").substringBefore(".json")
        if (!key.startsWith("/works/")) return ""
        val page = get("https://openlibrary.org$key.json")
        if (page.isBlank()) return ""
        val json = runCatching { JSONObject(page) }.getOrNull() ?: return ""
        val text = when (val raw = json.opt("description")) {
            is String -> raw
            is JSONObject -> raw.optString("value")
            else -> ""
        }.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        return listOf("", "", text).joinToString("\u0000")
    }

    private fun parse(body: String, query: String): List<BookHit> {
        if (body.isBlank()) return emptyList()
        val docs = runCatching { JSONObject(body).optJSONArray("docs") }.getOrNull() ?: return emptyList()
        val wantSummary = query.contains("summary", ignoreCase = true)
        val found = ArrayList<BookHit>()
        for (index in 0 until docs.length()) {
            val doc = docs.optJSONObject(index) ?: continue
            val title = doc.optString("title").trim()
            val key = doc.optString("key")
            if (title.isBlank() || !key.startsWith("/works/")) continue
            if (!wantSummary && title.startsWith("summary of", ignoreCase = true)) continue
            if (!readable(title, languages(doc))) continue
            val names = doc.optJSONArray("author_name")
            val author = buildString {
                val count = names?.length() ?: 0
                var written = 0
                for (nameIndex in 0 until count) {
                    val name = names?.optString(nameIndex).orEmpty().trim()
                    if (name.isBlank()) continue
                    if (written > 0) append(", ")
                    append(name)
                    written++
                    if (written == 2) break
                }
            }
            val year = doc.optInt("first_publish_year", 0).let { if (it > 0) it.toString() else "" }
            val coverId = doc.optInt("cover_i", 0)
            found += BookHit(
                workKey = "https://openlibrary.org$key",
                title = title,
                author = author,
                year = year,
                cover = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else "",
                overview = "",
            )
            if (found.size == 12) break
        }
        return found
    }

    private fun languages(doc: JSONObject): Set<String> {
        val raw = doc.optJSONArray("language") ?: return emptySet()
        return buildSet {
            for (index in 0 until raw.length()) add(raw.optString(index))
        }
    }

    private fun readable(title: String, languages: Set<String>): Boolean {
        if (title.any { it in TURKISH }) return true
        if (languages.isNotEmpty() && "eng" !in languages && "tur" !in languages) return false
        return title.none { it in FOREIGN }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun get(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", AGENT)
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
    }
}
