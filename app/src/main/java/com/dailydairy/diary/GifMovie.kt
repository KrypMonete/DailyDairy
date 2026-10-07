package com.dailydairy.diary

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileInputStream

@Composable
fun GifMovie(
    file: File,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    loops: Int = 0,
) {
    if (!file.exists()) return
    if (Build.VERSION.SDK_INT >= 28) {
        SystemGif(file, modifier, contentScale, loops)
    } else {
        LegacyGif(file, modifier, contentScale, loops)
    }
}

@Composable
private fun SystemGif(
    file: File,
    modifier: Modifier,
    contentScale: ContentScale,
    loops: Int,
) {
    val context = LocalContext.current
    val scale = viewScale(contentScale)
    val path = file.absolutePath
    val movie = remember(path) { decodeMovie(file) }
    val stopAfter = if (loops > 0) movie?.duration()?.times(loops)?.toLong() else null
    AndroidView(
        modifier = modifier,
        factory = {
            ImageView(context).apply {
                scaleType = scale
                adjustViewBounds = contentScale == ContentScale.FillWidth
            }
        },
        update = { view ->
            view.scaleType = scale
            if (view.tag == path) return@AndroidView
            (view.drawable as? AnimatedImageDrawable)?.stop()
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
            val animated = drawable as? AnimatedImageDrawable
            animated?.repeatCount = if (loops > 0) loops - 1 else AnimatedImageDrawable.REPEAT_INFINITE
            animated?.start()
            view.tag = path
            view.setImageDrawable(drawable)
            if (animated != null && stopAfter != null) {
                view.postDelayed({
                    if (view.tag != path) return@postDelayed
                    animated.stop()
                }, stopAfter)
            }
        },
        onRelease = { view ->
            (view.drawable as? AnimatedImageDrawable)?.stop()
            view.setImageDrawable(null)
        },
    )
}

@Composable
private fun LegacyGif(
    file: File,
    modifier: Modifier,
    contentScale: ContentScale,
    loops: Int,
) {
    val context = LocalContext.current
    val scale = viewScale(contentScale)
    val path = file.absolutePath
    val movie = remember(path) { decodeMovie(file) } ?: return
    val view = remember(path) {
        ImageView(context).apply {
            scaleType = scale
            adjustViewBounds = contentScale == ContentScale.FillWidth
        }
    }
    DisposableEffect(path, loops) {
        val buffer = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(buffer)
        val handler = Handler(Looper.getMainLooper())
        val started = SystemClock.uptimeMillis()
        val limit = if (loops > 0) movie.duration().toLong() * loops else Long.MAX_VALUE
        val tick = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - started
                val duration = movie.duration().coerceAtLeast(1)
                val time = if (elapsed >= limit) duration - 1 else (elapsed % duration).toInt()
                movie.setTime(time)
                buffer.eraseColor(0)
                movie.draw(canvas, 0f, 0f)
                view.invalidate()
                if (elapsed < limit) handler.postDelayed(this, 16L)
            }
        }
        view.setImageDrawable(BitmapDrawable(context.resources, buffer))
        handler.post(tick)
        onDispose {
            handler.removeCallbacks(tick)
            view.setImageDrawable(null)
            buffer.recycle()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { view },
        update = { it.scaleType = scale },
    )
}

private fun viewScale(contentScale: ContentScale): ImageView.ScaleType = when (contentScale) {
    ContentScale.Fit, ContentScale.FillWidth -> ImageView.ScaleType.FIT_CENTER
    else -> ImageView.ScaleType.CENTER_CROP
}

private fun decodeMovie(file: File): Movie? = runCatching {
    FileInputStream(file).use { Movie.decodeStream(it) }
}.getOrNull()?.takeIf { it.width() > 0 && it.height() > 0 && it.duration() > 0 }
