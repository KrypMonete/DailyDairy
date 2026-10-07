package com.dailydairy.watch

import androidx.room.Entity
import androidx.room.PrimaryKey

const val KIND_MOVIE = "movie"
const val KIND_TV = "tv"

const val SHELF_WANT = "want"
const val SHELF_WATCHING = "watching"
const val SHELF_DONE = "done"

@Entity(tableName = "watch_titles")
data class WatchTitle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Int,
    val kind: String,
    val title: String,
    val year: String,
    val poster: String,
    val overview: String,
    val shelf: String,
    val score: Int = 0,
    val note: String = "",
    val addedAt: Long,
)
