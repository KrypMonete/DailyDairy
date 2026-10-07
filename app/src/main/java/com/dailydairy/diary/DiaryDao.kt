package com.dailydairy.diary

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT COUNT(*) FROM diary_entries WHERE createdAt >= :start AND createdAt < :end")
    fun countCreatedBetween(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM diary_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun get(id: Long): DiaryEntry?

    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
