package com.dailydairy.diary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import java.io.File

@Composable
fun DiaryMedia(
    name: String,
    folder: String,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    full: Boolean = false,
    loops: Int = 0,
) {
    val context = LocalContext.current
    val file = DiaryImages.file(context, name, folder)
    val scale = when {
        full -> ContentScale.Fit
        wide -> ContentScale.FillWidth
        else -> ContentScale.Crop
    }
    if (DiaryImages.isGif(name)) {
        GifMovie(file = file, modifier = modifier, contentScale = scale, loops = loops)
        return
    }
    if (DiaryImages.isVideo(name)) {
        VideoClip(file = file, modifier = modifier, contentScale = scale)
        return
    }
    val edge = when {
        full -> 1600
        wide -> 1200
        else -> 480
    }
    val bitmap = remember(name, edge) { DiaryImages.decode(file, edge)?.asImageBitmap() } ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = scale,
        modifier = modifier,
    )
}

@Composable
fun MediaFill(
    name: String,
    folder: String,
    modifier: Modifier = Modifier,
    loops: Int = 0,
) {
    DiaryMedia(name, folder, modifier.fillMaxSize(), loops = loops)
}

@Composable
fun MediaWide(name: String, folder: String, modifier: Modifier = Modifier) {
    DiaryMedia(name, folder, modifier.fillMaxWidth(), wide = true)
}

@Composable
fun MediaFull(name: String, folder: String) {
    DiaryMedia(name, folder, Modifier.fillMaxSize(), full = true)
}

@Composable
private fun VideoClip(file: File, modifier: Modifier, contentScale: ContentScale) {
    if (!file.exists()) return
    val path = file.absolutePath
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.fromFile(file))
                setOnPreparedListener { player ->
                    player.isLooping = true
                    player.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { view ->
            if (view.tag == path) return@AndroidView
            view.tag = path
            view.setVideoURI(Uri.fromFile(file))
            view.start()
        },
        onRelease = { view ->
            view.stopPlayback()
        },
    )
}
