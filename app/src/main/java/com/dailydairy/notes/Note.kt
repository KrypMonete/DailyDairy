package com.dailydairy.notes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_folders")
data class NoteFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val pinned: Boolean = false,
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val bodyHtml: String = "",
    val images: String = "",
    val folderId: Long = 0,
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun imageNames(): List<String> =
        images.split('|').map { it.trim() }.filter { it.isNotEmpty() }
}
