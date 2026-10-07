package com.dailydairy.ui

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import java.util.Locale

private val tr = Locale.forLanguageTag("tr")

val highlightMark = Color(0xFFFFF3A0)
private val highlightInk = Color(0xFF1C1408)

fun highlight(text: String, query: String): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty() || text.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase(tr)
    val look = needle.lowercase(tr)
    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val at = lower.indexOf(look, start)
            if (at < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, at))
            withStyle(SpanStyle(background = highlightMark, color = highlightInk)) {
                append(text.substring(at, at + look.length))
            }
            start = at + look.length
        }
    }
}

fun headline(title: String, body: String): String {
    val named = title.trim()
    if (named.isNotEmpty()) return named
    val line = body.trim().lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    if (line.isEmpty()) return "Başlıksız"
    val end = line.indexOfFirst { it == '.' || it == '!' || it == '?' || it == '…' }
    val sentence = if (end in 1..90) line.substring(0, end) else line
    return sentence.trim().take(90)
}

fun afterHeadline(title: String, body: String): String {
    if (title.isNotBlank()) return body
    val head = headline("", body)
    if (head == "Başlıksız") return ""
    val line = body.trim()
    val at = line.indexOf(head)
    if (at < 0) return body
    return line.substring(at + head.length).trimStart(' ', '.', '!', '?', '…', '\n')
}

fun windowAround(text: String, query: String, radius: Int = 42): String {
    val needle = query.trim()
    if (needle.isEmpty()) return text
    val at = text.lowercase(tr).indexOf(needle.lowercase(tr))
    if (at < 0) return text
    val from = (at - radius).coerceAtLeast(0)
    val to = (at + needle.length + radius).coerceAtMost(text.length)
    val prefix = if (from > 0) "…" else ""
    val suffix = if (to < text.length) "…" else ""
    return prefix + text.substring(from, to).trim() + suffix
}

fun linkify(source: CharSequence): CharSequence {
    val text = SpannableString(source)
    Linkify.addLinks(text, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
    return text
}

fun markHtml(source: CharSequence, query: String): CharSequence {
    val needle = query.trim()
    if (needle.isEmpty()) return source
    val text = SpannableString(source)
    val lower = text.toString().lowercase(tr)
    val look = needle.lowercase(tr)
    var from = 0
    while (from < lower.length) {
        val at = lower.indexOf(look, from)
        if (at < 0) break
        val end = at + look.length
        text.setSpan(BackgroundColorSpan(0xFFFFF3A0.toInt()), at, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(0xFF1C1408.toInt()), at, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        from = end
    }
    return text
}

@Composable
fun MarkedText(
    text: String,
    query: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = null,
) {
    val shown = if (query.isBlank()) AnnotatedString(text) else highlight(text, query)
    Text(
        text = shown,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
    )
}
