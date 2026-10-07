package com.dailydairy.diary

import android.text.method.LinkMovementMethod
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailydairy.ui.MarkedText
import com.dailydairy.ui.afterHeadline
import com.dailydairy.ui.headline
import com.dailydairy.ui.linkify
import com.dailydairy.ui.markHtml
import com.dailydairy.ui.windowAround
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailydairy.home.clickableCard
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr"))

@Composable
fun DiaryRoute(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = viewModel(),
) {
    val lock by viewModel.lock.collectAsStateWithLifecycle()
    val ready by viewModel.lockReady.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val fingerprint by viewModel.fingerprintEnabled.collectAsStateWithLifecycle()

    if (!ready) return

    when {
        lock == null -> DiarySetupScreen(
            onBack = onBack,
            onCreate = { password, hint -> viewModel.createLock(password, hint) },
        )
        !unlocked -> DiaryUnlockScreen(
            hint = lock?.hint.orEmpty(),
            fingerprint = fingerprint,
            onBack = onBack,
            matches = viewModel::matches,
            onUnlock = viewModel::unlock,
        )
        else -> DiaryListScreen(onBack = onBack, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryListScreen(onBack: () -> Unit, viewModel: DiaryViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var reading by remember { mutableStateOf<DiaryEntry?>(null) }
    var editing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    val open = reading?.let { current -> entries.find { it.id == current.id } ?: current }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var askDate by remember { mutableStateOf(false) }
    var dayText by remember { mutableStateOf("") }
    var monthText by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf<DiaryDateFilter?>(null) }

    if (creating || (open != null && editing)) {
        DiaryEditor(
            entry = if (creating) null else open,
            onBack = {
                editing = false
                creating = false
            },
            onSave = { title, body, html, images, place ->
                val wasNew = creating
                viewModel.save(if (wasNew) null else open?.id, title, body, html, images, place)
                editing = false
                creating = false
                if (wasNew) reading = null
            },
        )
        return
    }

    if (open != null) {
        DiaryReader(
            entry = open,
            query = query,
            onBack = { reading = null },
            onEdit = { editing = true },
        )
        return
    }

    val needle = query.trim()
    val shown = remember(entries, needle, dateFilter) {
        entries.filter { entry ->
            val zone = ZoneId.systemDefault()
            val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            val dateOk = when (val filter = dateFilter) {
                null -> true
                is DiaryDateFilter.Day -> date == filter.date
                is DiaryDateFilter.Month -> date.year == filter.year && date.monthValue == filter.month
                is DiaryDateFilter.Year -> date.year == filter.year
            }
            val textOk = needle.isEmpty() ||
                entry.title.contains(needle, ignoreCase = true) ||
                entry.body.contains(needle, ignoreCase = true) ||
                entry.place.contains(needle, ignoreCase = true)
            dateOk && textOk
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Başlık, yazı veya yer") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { }),
                        )
                    } else {
                        Column {
                            Text("Günlük", style = MaterialTheme.typography.headlineMedium)
                            dateFilter?.let { filter ->
                                Text(
                                    text = filter.label(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searching) {
                            searching = false
                            query = ""
                        } else {
                            searching = true
                        }
                    }) {
                        Icon(
                            if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                            contentDescription = if (searching) "Aramayı kapat" else "Ara",
                        )
                    }
                    if (!searching) {
                        IconButton(onClick = { askDate = true }) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Tarih",
                                tint = if (dateFilter == null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                },
                colors = barColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                entries.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Henüz yazı yok.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                shown.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Bu aramaya uyan yazı yok.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(shown, key = { it.id }) { entry ->
                            DiaryRow(
                                entry = entry,
                                query = needle,
                                onOpen = { reading = entry },
                                onDelete = { pendingDelete = entry },
                            )
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
            if (!searching) {
                FloatingActionButton(
                    onClick = { creating = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Yeni yazı")
                }
            }
        }
    }

    if (askDate) {
        val day = dayText.toIntOrNull()
        val month = monthText.toIntOrNull()
        val year = yearText.toIntOrNull()
        val valid = diaryFilterOf(day, month, year) != null
        AlertDialog(
            onDismissRequest = { askDate = false },
            title = { Text("Tarihe göre") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gün boşsa ay, ay da boşsa yıl. Hepsi doluysa o gün.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dayText,
                            onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Gün") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = monthText,
                            onValueChange = { monthText = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Ay") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = yearText,
                            onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true,
                            label = { Text("Yıl") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateFilter = diaryFilterOf(day, month, year)
                        askDate = false
                    },
                    enabled = valid,
                ) { Text("Göster") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dayText = ""
                    monthText = ""
                    yearText = ""
                    dateFilter = null
                    askDate = false
                }) { Text("Temizle") }
            },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Yazı silinsin mi?") },
            text = { Text(headline(entry.title, entry.body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry.id)
                    pendingDelete = null
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Vazgeç") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryReader(entry: DiaryEntry, query: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    var openImage by remember { mutableStateOf<String?>(null) }
    val stamp = remember(entry.createdAt) {
        Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(dayFormat)
    }
    val html = remember(entry.id, entry.bodyHtml, entry.body) {
        when {
            entry.bodyHtml.isNotBlank() -> entry.bodyHtml
            entry.body.isNotBlank() -> HtmlCompat.toHtml(
                android.text.SpannableStringBuilder(entry.body),
                HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE,
            )
            else -> ""
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface
    BackHandler(enabled = openImage == null, onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Yazı", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Düzenle")
                    }
                },
                colors = barColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stamp,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.place.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    MarkedText(
                        text = entry.place,
                        query = query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (entry.title.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                MarkedText(
                    text = entry.title,
                    query = query,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
            if (html.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { viewContext ->
                        android.widget.TextView(viewContext).apply {
                            setTextColor(ink.toArgb())
                            textSize = 16f
                            background = null
                        }
                    },
                    update = { view ->
                        view.setTextColor(ink.toArgb())
                        view.text = linkify(
                            markHtml(
                                HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT),
                                query,
                            ),
                        )
                        view.movementMethod = LinkMovementMethod.getInstance()
                        view.linksClickable = true
                    },
                )
            }
            if (entry.imageNames().isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                entry.imageNames().forEach { name ->
                    val bitmap = remember(name) {
                        DiaryImages.decode(DiaryImages.file(context, name), 1200)?.asImageBitmap()
                    }
                    if (DiaryImages.isGif(name) || DiaryImages.isVideo(name) || bitmap != null) {
                        if (DiaryImages.isGif(name) || DiaryImages.isVideo(name)) {
                            MediaWide(
                                name = name,
                                folder = "diary_images",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { openImage = name },
                            )
                        } else {
                            Image(
                                bitmap = bitmap!!,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { openImage = name },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    openImage?.let { name ->
        val bitmap = remember(name) {
            DiaryImages.decode(DiaryImages.file(context, name), 1600)?.asImageBitmap()
        }
        if (bitmap != null || DiaryImages.isGif(name) || DiaryImages.isVideo(name)) {
            Dialog(
                onDismissRequest = { openImage = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { openImage = null },
                    contentAlignment = Alignment.Center,
                ) {
                    if (DiaryImages.isGif(name) || DiaryImages.isVideo(name)) {
                        MediaFull(name, "diary_images")
                    } else if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryRow(entry: DiaryEntry, query: String, onOpen: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val date = remember(entry.updatedAt) {
        Instant.ofEpochMilli(entry.updatedAt).atZone(ZoneId.systemDefault()).format(dayFormat)
    }
    val thumb = remember(entry.images) {
        entry.imageNames().firstOrNull()?.let { name ->
            if (DiaryImages.isGif(name) || DiaryImages.isVideo(name)) null
            else DiaryImages.decode(DiaryImages.file(context, name), 240)?.asImageBitmap()
        }
    }
    val first = entry.imageNames().firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onClick = onOpen)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (first != null && (DiaryImages.isGif(first) || DiaryImages.isVideo(first))) {
            MediaFill(
                name = first,
                folder = "diary_images",
                loops = 3,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            val preview = afterHeadline(entry.title, entry.body)
            MarkedText(
                text = headline(entry.title, entry.body),
                query = query,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            MarkedText(
                text = if (entry.place.isBlank()) date else "$date  ·  ${entry.place}",
                query = query,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                MarkedText(
                    text = windowAround(preview, query),
                    query = query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun barColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
)

private sealed interface DiaryDateFilter {
    data class Day(val date: LocalDate) : DiaryDateFilter
    data class Month(val year: Int, val month: Int) : DiaryDateFilter
    data class Year(val year: Int) : DiaryDateFilter

    fun label(): String = when (this) {
        is Day -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr")))
        is Month -> YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("tr")))
        is Year -> year.toString()
    }
}

private fun diaryFilterOf(day: Int?, month: Int?, year: Int?): DiaryDateFilter? {
    val y = year?.takeIf { it in 1970..2100 } ?: return null
    val m = month?.takeIf { it in 1..12 }
    val d = day?.takeIf { it in 1..31 }
    return when {
        m != null && d != null -> runCatching { DiaryDateFilter.Day(LocalDate.of(y, m, d)) }.getOrNull()
        m != null -> DiaryDateFilter.Month(y, m)
        d == null && month == null -> DiaryDateFilter.Year(y)
        else -> null
    }
}
