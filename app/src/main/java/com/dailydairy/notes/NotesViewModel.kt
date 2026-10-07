package com.dailydairy.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailydairy.diary.DiaryImages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = NoteDatabase.get(application).noteDao()

    val notes: StateFlow<List<Note>> = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val folders: StateFlow<List<NoteFolder>> = dao.observeFolders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun createFolder(name: String, noteIds: Collection<Long> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = dao.insertFolder(NoteFolder(name = trimmed, createdAt = System.currentTimeMillis()))
            noteIds.forEach { dao.setFolder(it, id) }
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            dao.clearFolder(id)
            dao.deleteFolder(id)
        }
    }

    fun togglePin(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val notes = dao.getAll(ids.toList())
            val pin = notes.any { !it.pinned }
            notes.forEach { dao.setPinned(it.id, pin) }
        }
    }

    fun renameFolder(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { dao.renameFolder(id, trimmed) }
    }

    fun toggleFolderPin(id: Long, pinned: Boolean) {
        viewModelScope.launch { dao.setFolderPinned(id, !pinned) }
    }

    fun moveNotes(ids: Collection<Long>, folderId: Long) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { dao.setFolder(it, folderId) }
        }
    }

    fun save(id: Long?, title: String, body: String, bodyHtml: String, images: String, folderId: Long = 0) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id == null) {
                dao.insert(
                    Note(
                        title = title.trim(),
                        body = body.trim(),
                        bodyHtml = bodyHtml,
                        images = images,
                        folderId = folderId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                val existing = dao.get(id) ?: return@launch
                val dropped = existing.imageNames() - images.split('|').filter { it.isNotBlank() }.toSet()
                DiaryImages.delete(getApplication(), dropped, "note_images")
                dao.update(
                    existing.copy(
                        title = title.trim(),
                        body = body.trim(),
                        bodyHtml = bodyHtml,
                        images = images,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    fun delete(id: Long) = deleteAll(listOf(id))

    fun deleteAll(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val names = dao.getAll(ids.toList()).flatMap { it.imageNames() }
            DiaryImages.delete(getApplication(), names, "note_images")
            ids.forEach { dao.delete(it) }
        }
    }
}
