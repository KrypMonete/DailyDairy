package com.dailydairy.books

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Book::class], version = 1, exportSchema = false)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile private var instance: BookDatabase? = null

        fun closeInstance() {
            instance?.close()
            instance = null
        }

        fun get(context: Context): BookDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "books.db",
                ).build().also { instance = it }
            }
    }
}
