package com.dailydairy.books

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

class BooksViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = BookDatabase.get(application).bookDao()

    val saved: StateFlow<List<Book>> = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val results = MutableStateFlow<List<BookHit>>(emptyList())
    val hits: StateFlow<List<BookHit>> = results.asStateFlow()

    private val busy = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = busy.asStateFlow()

    private val offline = MutableStateFlow(false)
    val unreachable: StateFlow<Boolean> = offline.asStateFlow()

    private val opened = MutableStateFlow<BookHit?>(null)
    val detail: StateFlow<BookHit?> = opened.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        val needle = query.trim()
        if (needle.length < 2) {
            results.value = emptyList()
            busy.value = false
            offline.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            busy.value = true
            offline.value = false
            results.value = emptyList()
            val found = withContext(Dispatchers.IO) {
                runCatching { OpenLibrary.search(needle) }
            }
            offline.value = found.exceptionOrNull() is java.io.IOException
            results.value = found.getOrDefault(emptyList())
            busy.value = false
        }
    }

    fun clearSearch() {
        results.value = emptyList()
        offline.value = false
    }

    fun open(hit: BookHit) {
        opened.value = hit
        if (hit.overview.isNotBlank()) return
        if (hit.workKey.startsWith("own:")) return
        viewModelScope.launch {
            val packed = withContext(Dispatchers.IO) {
                runCatching { OpenLibrary.description(hit.workKey) }.getOrDefault("")
            }
            val parts = packed.split("\u0000")
            val author = parts.getOrNull(0).orEmpty()
            val year = parts.getOrNull(1).orEmpty()
            val text = parts.getOrNull(2).orEmpty()
            if (text.isNotBlank()) {
                dao.find(hit.workKey)?.let {
                    dao.update(it.copy(overview = text, author = author.ifBlank { it.author }, year = year.ifBlank { it.year }))
                }
            }
            val current = opened.value
            if (current != null && current.workKey == hit.workKey) {
                opened.value = current.copy(
                    author = author.ifBlank { current.author },
                    year = year.ifBlank { current.year },
                    overview = text,
                )
            }
        }
    }

    fun close() {
        opened.value = null
    }

    fun save(hit: BookHit, shelf: String) {
        viewModelScope.launch {
            val existing = dao.find(hit.workKey)
            if (existing == null) {
                dao.insert(
                    Book(
                        workKey = hit.workKey,
                        title = hit.title,
                        author = hit.author,
                        year = hit.year,
                        cover = hit.cover,
                        overview = hit.overview,
                        shelf = shelf,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                dao.update(
                    existing.copy(
                        shelf = shelf,
                        overview = hit.overview.ifBlank { existing.overview },
                        addedAt = if (shelf == BOOK_READING) System.currentTimeMillis() else existing.addedAt,
                    ),
                )
            }
        }
    }

    fun rate(hit: BookHit, score: Int, note: String) {
        viewModelScope.launch {
            val existing = dao.find(hit.workKey) ?: return@launch
            dao.update(existing.copy(score = score.coerceIn(0, 10), note = note.trim()))
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    fun addOwn(title: String, author: String, year: String, overview: String, cover: String) {
        viewModelScope.launch {
            dao.insert(
                Book(
                    workKey = "own:${java.util.UUID.randomUUID()}",
                    title = title.trim(),
                    author = author.trim(),
                    year = year.trim(),
                    cover = cover,
                    overview = overview.trim(),
                    shelf = BOOK_WANT,
                    addedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
