package com.dailydairy.books

import androidx.room.Entity
import androidx.room.PrimaryKey

const val BOOK_READING = "reading"
const val BOOK_WANT = "want"
const val BOOK_DONE = "done"

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workKey: String,
    val title: String,
    val author: String,
    val year: String,
    val cover: String,
    val overview: String,
    val shelf: String,
    val score: Int = 0,
    val note: String = "",
    val addedAt: Long,
)
