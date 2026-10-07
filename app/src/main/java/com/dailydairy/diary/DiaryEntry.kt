package com.dailydairy.diary

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val bodyHtml: String = "",
    val images: String = "",
    val place: String = "",
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun imageNames(): List<String> =
        images.split('|').map { it.trim() }.filter { it.isNotEmpty() }
}
