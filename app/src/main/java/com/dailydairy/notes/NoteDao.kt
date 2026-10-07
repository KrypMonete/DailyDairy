package com.dailydairy.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<Note>

    @Query("SELECT * FROM note_folders ORDER BY pinned DESC, name COLLATE NOCASE")
    fun observeFolders(): Flow<List<NoteFolder>>

    @Insert
    suspend fun insertFolder(folder: NoteFolder): Long

    @Query("DELETE FROM note_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("UPDATE notes SET folderId = 0 WHERE folderId = :id")
    suspend fun clearFolder(id: Long)

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :id")
    suspend fun setFolder(id: Long, folderId: Long)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE note_folders SET pinned = :pinned WHERE id = :id")
    suspend fun setFolderPinned(id: Long, pinned: Boolean)

    @Query("UPDATE note_folders SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String)
}
