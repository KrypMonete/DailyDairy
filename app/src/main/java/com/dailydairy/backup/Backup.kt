package com.dailydairy.backup

import android.content.Context
import android.net.Uri
import com.dailydairy.books.BookDatabase
import com.dailydairy.diary.DiaryDatabase
import com.dailydairy.notes.NoteDatabase
import com.dailydairy.shopping.ShoppingDatabase
import com.dailydairy.watch.WatchDatabase
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Backup {
    private val databases = listOf("diary.db", "notes.db", "shopping.db", "watch.db", "books.db")
    private val stores = listOf("diary_lock.preferences_pb", "theme.preferences_pb")
    private val folders = listOf("diary_images", "note_images", "book_covers")

    fun export(context: Context, dest: Uri) {
        checkpoint(context)
        val root = context.applicationContext.filesDir
        context.contentResolver.openOutputStream(dest)?.use { raw ->
            ZipOutputStream(raw).use { zip ->
                databases.forEach { name ->
                    val dir = File(root.parentFile, "databases")
                    listOf(name, "$name-wal", "$name-shm").forEach { file ->
                        addFile(zip, File(dir, file), "databases/$file")
                    }
                }
                stores.forEach { addFile(zip, File(root, "datastore/$it"), "datastore/$it") }
                folders.forEach { folder ->
                    val dir = File(root, folder)
                    dir.listFiles()?.filter { it.isFile }?.forEach { file ->
                        addFile(zip, file, "$folder/${file.name}")
                    }
                }
            }
        } ?: error("Yedek yazılamadı.")
    }

    fun restore(context: Context, source: Uri) {
        val app = context.applicationContext
        val root = app.filesDir
        val stage = File(app.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        app.contentResolver.openInputStream(source)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                var count = 0
                while (entry != null) {
                    if (!entry.isDirectory && safe(entry.name)) {
                        val file = File(stage, entry.name)
                        file.parentFile?.mkdirs()
                        file.outputStream().use { zip.copyTo(it) }
                        count++
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                if (count == 0) error("Bu dosya yedek değil.")
            }
        } ?: error("Yedek okunamadı.")

        closeDatabases()
        databases.forEach { name ->
            listOf(name, "$name-wal", "$name-shm").forEach { file ->
                val incoming = File(stage, "databases/$file")
                val target = File(app.getDatabasePath(name).parentFile, file)
                if (incoming.exists()) {
                    target.parentFile?.mkdirs()
                    incoming.copyTo(target, overwrite = true)
                } else if (file != name) {
                    target.delete()
                }
            }
        }
        stores.forEach { name ->
            val incoming = File(stage, "datastore/$name")
            if (incoming.exists()) incoming.copyTo(File(root, "datastore/$name"), overwrite = true)
        }
        folders.forEach { folder ->
            val incoming = File(stage, folder)
            val target = File(root, folder)
            if (incoming.exists()) {
                target.deleteRecursively()
                incoming.copyRecursively(target, overwrite = true)
            }
        }
        stage.deleteRecursively()
    }

    private fun safe(name: String): Boolean {
        if (name.contains("..") || name.startsWith("/")) return false
        return databases.any { name == "databases/$it" || name == "databases/$it-wal" || name == "databases/$it-shm" } ||
            stores.any { name == "datastore/$it" } ||
            folders.any { name.startsWith("$it/") && !name.endsWith("/") }
    }

    private fun checkpoint(context: Context) {
        DiaryDatabase.get(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        NoteDatabase.get(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        ShoppingDatabase.get(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        WatchDatabase.get(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        BookDatabase.get(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    private fun closeDatabases() {
        DiaryDatabase.closeInstance()
        NoteDatabase.closeInstance()
        ShoppingDatabase.closeInstance()
        WatchDatabase.closeInstance()
        BookDatabase.closeInstance()
    }

    private fun addFile(zip: ZipOutputStream, file: File, name: String) {
        if (!file.exists() || !file.isFile) return
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
