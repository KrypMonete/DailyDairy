package com.dailydairy.watch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WatchDatabase.get(application).watchDao()

    val saved: StateFlow<List<WatchTitle>> = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val results = MutableStateFlow<List<TitleHit>>(emptyList())
    val hits: StateFlow<List<TitleHit>> = results.asStateFlow()

    private val busy = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = busy.asStateFlow()

    private val opened = MutableStateFlow<TitleDetail?>(null)
    val detail: StateFlow<TitleDetail?> = opened.asStateFlow()

    private val score = MutableStateFlow("")
    val imdb: StateFlow<String> = score.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        val needle = query.trim()
        if (needle.length < 2) {
            results.value = emptyList()
            busy.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            busy.value = true
            results.value = withContext(Dispatchers.IO) {
                runCatching { Tmdb.search(needle) }.getOrDefault(emptyList())
            }
            busy.value = false
        }
    }

    fun clearSearch() {
        results.value = emptyList()
    }

    fun open(hit: TitleHit) {
        opened.value = TitleDetail(hit, "", emptyList())
        score.value = ""
        viewModelScope.launch {
            val full = withContext(Dispatchers.IO) {
                runCatching { Tmdb.detail(hit.tmdbId, hit.kind) }.getOrNull()
            } ?: return@launch
            opened.value = full
            if (full.imdb.isNotBlank()) {
                score.value = withContext(Dispatchers.IO) {
                    runCatching { Omdb.rating(full.imdb) }.getOrDefault("")
                }
            }
        }
    }

    fun close() {
        opened.value = null
        score.value = ""
    }

    fun save(hit: TitleHit, shelf: String) {
        viewModelScope.launch {
            val existing = dao.find(hit.tmdbId, hit.kind)
            if (existing == null) {
                dao.insert(
                    WatchTitle(
                        tmdbId = hit.tmdbId,
                        kind = hit.kind,
                        title = hit.title,
                        year = hit.year,
                        poster = hit.poster,
                        overview = hit.overview,
                        shelf = shelf,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                dao.update(existing.copy(shelf = shelf, overview = hit.overview.ifBlank { existing.overview }))
            }
        }
    }

    fun rate(hit: TitleHit, score: Int, note: String) {
        viewModelScope.launch {
            val existing = dao.find(hit.tmdbId, hit.kind) ?: return@launch
            dao.update(existing.copy(score = score.coerceIn(0, 10), note = note.trim()))
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}
