package com.dailydairy.watch

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WatchTitle::class], version = 2, exportSchema = false)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun watchDao(): WatchDao

    companion object {
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_titles ADD COLUMN score INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE watch_titles ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile private var instance: WatchDatabase? = null

        fun closeInstance() {
            instance?.close()
            instance = null
        }

        fun get(context: Context): WatchDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WatchDatabase::class.java,
                    "watch.db",
                ).addMigrations(migration1To2).build().also { instance = it }
            }
    }
}
