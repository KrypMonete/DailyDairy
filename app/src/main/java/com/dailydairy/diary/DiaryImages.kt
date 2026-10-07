package com.dailydairy.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

object DiaryImages {
    private fun dir(context: Context, folder: String): File =
        File(context.applicationContext.filesDir, folder).apply { mkdirs() }

    fun file(context: Context, name: String, folder: String = "diary_images"): File =
        File(dir(context, folder), name)

    fun copy(context: Context, uri: Uri, folder: String = "diary_images"): String? {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("gif") -> "gif"
            mime.startsWith("video") -> "mp4"
            else -> "jpg"
        }
        val name = "${UUID.randomUUID()}.$ext"
        val dest = file(context, name, folder)
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source -> dest.outputStream().use { source.copyTo(it) } }
        return name
    }

    fun delete(context: Context, names: List<String>, folder: String = "diary_images") {
        names.forEach { name -> file(context, name, folder).delete() }
    }

    fun isGif(name: String): Boolean = name.endsWith(".gif", ignoreCase = true)

    fun isVideo(name: String): Boolean = name.endsWith(".mp4", ignoreCase = true)

    fun download(context: Context, address: String, folder: String = "diary_images"): String? {
        val name = "${UUID.randomUUID()}.gif"
        val dest = file(context, name, folder)
        val connection = (java.net.URL(address).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { source -> dest.outputStream().use { source.copyTo(it) } }
            if (dest.length() == 0L) {
                dest.delete()
                null
            } else {
                name
            }
        } catch (_: Exception) {
            dest.delete()
            null
        } finally {
            connection.disconnect()
        }
    }

    fun decode(file: File, maxEdge: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }
}
