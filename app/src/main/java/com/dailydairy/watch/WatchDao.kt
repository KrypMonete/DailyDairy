package com.dailydairy.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {
    @Query("SELECT * FROM watch_titles ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchTitle>>

    @Query("SELECT * FROM watch_titles WHERE tmdbId = :tmdbId AND kind = :kind LIMIT 1")
    suspend fun find(tmdbId: Int, kind: String): WatchTitle?

    @Insert
    suspend fun insert(title: WatchTitle): Long

    @Update
    suspend fun update(title: WatchTitle)

    @Query("DELETE FROM watch_titles WHERE id = :id")
    suspend fun delete(id: Long)
}
