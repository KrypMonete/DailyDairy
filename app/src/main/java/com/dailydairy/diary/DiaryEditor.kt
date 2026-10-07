package com.dailydairy.diary

import android.text.method.LinkMovementMethod
import android.view.inputmethod.InputMethodManager
import android.text.InputType
import android.widget.EditText
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import coil3.compose.AsyncImage
import com.dailydairy.home.clickableCard
import com.dailydairy.ui.linkify
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditor(
    entry: DiaryEntry?,
    onBack: () -> Unit,
    onSave: (title: String, body: String, bodyHtml: String, images: String, place: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var place by rememberSaveable { mutableStateOf(entry?.place.orEmpty()) }
    var showPlace by remember { mutableStateOf(false) }
    var customPlace by rememberSaveable { mutableStateOf(entry?.place.orEmpty()) }
    var changedPlace by remember { mutableStateOf(false) }
    val writtenAt = remember(entry?.createdAt) {
        entry?.createdAt?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr")))
        }
    }
    val startingHtml = remember(entry?.id) {
        when {
            !entry?.bodyHtml.isNullOrBlank() -> entry.bodyHtml
            !entry?.body.isNullOrBlank() -> HtmlCompat.toHtml(
                android.text.SpannableStringBuilder(entry.body),
                HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE,
            )
            else -> ""
        }
    }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch {
                val found = findPlace(context).orEmpty()
                if (found.isNotBlank()) {
                    place = found
                    customPlace = found
                    changedPlace = true
                }
            }
        }
    }

    RichEditor(
        titleText = if (entry == null) "Yeni yazı" else "Yazı",
        initialTitle = entry?.title.orEmpty(),
        initialHtml = startingHtml,
        initialImages = entry?.imageNames().orEmpty(),
        imageFolder = "diary_images",
        bodyHint = "Bugün",
        rememberKey = entry?.id,
        allowVideo = true,
        allowGiphy = true,
        onBack = onBack,
        aboveTitle = {
            writtenAt?.let { stamp ->
                Text(
                    text = stamp,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (place.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = place,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        },
        extraTools = {
            IconButton(onClick = { showPlace = true }) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = "Konum",
                    tint = if (place.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                )
            }
        },
        changedExtra = changedPlace,
        onSave = { title, body, html, images ->
            onSave(title, body, html, images, place)
        },
    )

    if (showPlace) {
        AlertDialog(
            onDismissRequest = { showPlace = false },
            title = { Text("Konum") },
            text = {
                Column {
                    TextButton(onClick = {
                        showPlace = false
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            scope.launch {
                                val found = findPlace(context).orEmpty()
                                if (found.isNotBlank()) {
                                    place = found
                                    customPlace = found
                                    changedPlace = true
                                }
                            }
                        } else {
                            locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    }) { Text("Şu anki konum") }
                    OutlinedTextField(
                        value = customPlace,
                        onValueChange = { customPlace = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Başka bir yer") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    place = customPlace.trim()
                    changedPlace = true
                    showPlace = false
                }) { Text("Ekle") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (place.isNotBlank()) {
                        place = ""
                        customPlace = ""
                        changedPlace = true
                    }
                    showPlace = false
                }) { Text(if (place.isNotBlank()) "Kaldır" else "Vazgeç") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RichEditor(
    titleText: String,
    initialTitle: String,
    initialHtml: String,
    initialImages: List<String>,
    imageFolder: String,
    bodyHint: String,
    rememberKey: Long?,
    allowVideo: Boolean = false,
    allowGiphy: Boolean = false,
    onBack: () -> Unit,
    aboveTitle: @Composable () -> Unit = {},
    extraTools: @Composable () -> Unit = {},
    changedExtra: Boolean = false,
    onSave: (title: String, body: String, bodyHtml: String, images: String) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable(rememberKey) { mutableStateOf(initialTitle) }
    var bodyText by remember { mutableStateOf("") }
    val images = remember(rememberKey) { initialImages.toMutableStateList() }
    var openImage by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<EditText?>(null) }
    var boldOn by remember { mutableStateOf(false) }
    var italicOn by remember { mutableStateOf(false) }
    var underlineOn by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    var inkColor by remember { mutableStateOf<Int?>(null) }
    var askLeave by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var changed by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onSurface.toArgb()
    val hint = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        DiaryImages.copy(context, uri, imageFolder)?.let {
            images.add(it)
            changed = true
        }
    }
    var showGifs by remember { mutableStateOf(false) }
    val goBack = {
        if (changed || changedExtra) askLeave = true else onBack()
    }
    BackHandler(enabled = !askLeave && openImage == null, onBack = goBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(titleText, style = MaterialTheme.typography.headlineMedium)
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            aboveTitle()
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (started) changed = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Başlık") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.height(4.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                factory = { context ->
                    EditText(context).apply {
                        setTextColor(ink)
                        setHintTextColor(hint)
                        setHint(bodyHint)
                        inputType = InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        background = null
                        textSize = 16f
                        gravity = android.view.Gravity.TOP
                        if (initialHtml.isNotBlank()) {
                            setText(linkify(HtmlCompat.fromHtml(initialHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)))
                        }
                        movementMethod = LinkMovementMethod.getInstance()
                        linksClickable = true
                        if (initialTitle.isBlank()) {
                            post {
                                requestFocus()
                                val imm = context.getSystemService(InputMethodManager::class.java)
                                imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                        doAfterTextChanged { editable ->
                            bodyText = editable?.toString().orEmpty()
                            if (started) changed = true
                            val flags = tag as? FormatFlags ?: return@doAfterTextChanged
                            val text = editable ?: return@doAfterTextChanged
                            val end = selectionEnd
                            val start = (end - 1).coerceAtLeast(0)
                            if (end <= start) return@doAfterTextChanged
                            if (flags.bold) {
                                text.setSpan(
                                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                    start,
                                    end,
                                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                            if (flags.italic) {
                                text.setSpan(
                                    android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                                    start,
                                    end,
                                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                            if (flags.underline) {
                                text.setSpan(
                                    android.text.style.UnderlineSpan(),
                                    start,
                                    end,
                                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                            flags.color?.let { color ->
                                text.setSpan(
                                    android.text.style.ForegroundColorSpan(color),
                                    start,
                                    end,
                                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                        }
                        editor = this
                        post {
                            bodyText = text?.toString().orEmpty()
                            started = true
                        }
                    }
                },
                update = { view ->
                    view.setTextColor(ink)
                    view.setHintTextColor(hint)
                    view.tag = FormatFlags(boldOn, italicOn, underlineOn, inkColor)
                    editor = view
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormatButton(Icons.Outlined.FormatBold, "Kalın", boldOn) {
                    boldOn = !boldOn
                    editor.applyToSelection(bold = boldOn, italic = null, underline = null)
                    editor?.tag = FormatFlags(boldOn, italicOn, underlineOn, inkColor)
                }
                FormatButton(Icons.Outlined.FormatItalic, "İtalik", italicOn) {
                    italicOn = !italicOn
                    editor.applyToSelection(bold = null, italic = italicOn, underline = null)
                    editor?.tag = FormatFlags(boldOn, italicOn, underlineOn, inkColor)
                }
                FormatButton(Icons.Outlined.FormatUnderlined, "Altı çizili", underlineOn) {
                    underlineOn = !underlineOn
                    editor.applyToSelection(bold = null, italic = null, underline = underlineOn)
                    editor?.tag = FormatFlags(boldOn, italicOn, underlineOn, inkColor)
                }
                ColorBubble(open = showColors) { showColors = !showColors }
                IconButton(onClick = {
                    val kind = if (allowVideo) {
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    } else {
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    }
                    picker.launch(PickVisualMediaRequest(kind))
                }) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = "Galeriden ekle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
                if (allowGiphy && Giphy.ready) {
                    IconButton(onClick = { showGifs = true }) {
                        Icon(
                            Icons.Outlined.Gif,
                            contentDescription = "GIF ara",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
                extraTools()
            }
            if (showColors) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    inkChoices.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickableCard(onClick = {
                                    val next = if (inkColor == color) null else color
                                    inkColor = next
                                    editor.applyColor(next)
                                    editor?.tag = FormatFlags(boldOn, italicOn, underlineOn, next)
                                    showColors = false
                                }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (inkColor == color) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    images.forEach { name ->
                        ImageThumb(
                            name = name,
                            folder = imageFolder,
                            onOpen = { openImage = name },
                            onRemove = {
                                DiaryImages.delete(context, listOf(name), imageFolder)
                                images.remove(name)
                                changed = true
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val field = editor
                    val plain = field?.text?.toString().orEmpty()
                    val html = field?.let {
                        HtmlCompat.toHtml(it.text, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                    }.orEmpty()
                    onSave(title, plain, html, images.joinToString("|"))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() || bodyText.isNotBlank() || images.isNotEmpty(),
            ) { Text("Kaydet") }
        }
    }

    openImage?.let { name ->
        ImageViewer(name, imageFolder) { openImage = null }
    }

    if (showGifs) {
        GifPicker(
            folder = imageFolder,
            onPick = { name ->
                images.add(name)
                changed = true
                showGifs = false
            },
            onClose = { showGifs = false },
        )
    }

    if (askLeave) {
        AlertDialog(
            onDismissRequest = { askLeave = false },
            title = { Text("Kaydetmeden çıkılsın mı?") },
            text = { Text("Yaptığın değişiklikler kaybolur.") },
            confirmButton = {
                TextButton(onClick = {
                    askLeave = false
                    onBack()
                }) { Text("Çık") }
            },
            dismissButton = {
                TextButton(onClick = { askLeave = false }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun ImageThumb(name: String, folder: String, onOpen: () -> Unit, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(72.dp)) {
        MediaFill(
            name = name,
            folder = folder,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpen),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp),
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Görseli kaldır",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ImageViewer(name: String, folder: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            MediaFull(name, folder)
        }
    }
}

@Composable
private fun GifPicker(folder: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<GifHit>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        delay(300)
        busy = true
        failed = false
        val found = withContext(Dispatchers.IO) { runCatching { Giphy.search(query) }.getOrDefault(emptyList()) }
        hits = found
        busy = false
        failed = found.isEmpty()
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Kapat")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("GIF ara") },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Powered by GIPHY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            when {
                busy -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                failed -> Text(
                    text = "GIF bulunamadı.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(hits, key = { it.id }) { hit ->
                        AsyncImage(
                            model = hit.preview,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        val name = withContext(Dispatchers.IO) {
                                            DiaryImages.download(context, hit.file, folder)
                                        }
                                        if (name != null) onPick(name)
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

private suspend fun findPlace(context: android.content.Context): String? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    var location = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        ?: runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    if (location == null) {
        location = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(found: android.location.Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(found)
                }
            }
            val started = runCatching {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
                true
            }.getOrDefault(false) || runCatching {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
                true
            }.getOrDefault(false)
            if (!started) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }
    val found = location ?: return null
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.forLanguageTag("tr"))
        val address = runCatching { geocoder.getFromLocation(found.latitude, found.longitude, 1) }
            .getOrNull()
            ?.firstOrNull()
            ?: return@withContext null
        listOfNotNull(address.subLocality, address.locality, address.adminArea)
            .distinct()
            .joinToString(", ")
            .ifBlank { null }
    }
}

@Composable
private fun FormatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            },
        )
    }
}

private val inkChoices = listOf(
    0xFFE23B3B.toInt(),
    0xFFF08A24.toInt(),
    0xFFE2B123.toInt(),
    0xFF3C9A4A.toInt(),
    0xFF2F8FD4.toInt(),
    0xFF7A4AD4.toInt(),
)

@Composable
private fun ColorBubble(open: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        listOf(
                            Color(0xFFE23B3B),
                            Color(0xFFF08A24),
                            Color(0xFFE2B123),
                            Color(0xFF3C9A4A),
                            Color(0xFF2F8FD4),
                            Color(0xFF7A4AD4),
                            Color(0xFFE23B3B),
                        ),
                    ),
                )
                .border(
                    width = if (open) 1.5.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ),
        )
    }
}

private data class FormatFlags(
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val color: Int?,
)

private fun EditText?.applyToSelection(bold: Boolean?, italic: Boolean?, underline: Boolean?) {
    val field = this ?: return
    val start = minOf(field.selectionStart, field.selectionEnd)
    val end = maxOf(field.selectionStart, field.selectionEnd)
    if (start < 0 || start == end) return
    val text = field.text
    fun restyle(style: Int, on: Boolean) {
        text.getSpans(start, end, android.text.style.StyleSpan::class.java)
            .filter { it.style == style }
            .forEach { text.removeSpan(it) }
        if (on) {
            text.setSpan(
                android.text.style.StyleSpan(style),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    if (bold != null) restyle(android.graphics.Typeface.BOLD, bold)
    if (italic != null) restyle(android.graphics.Typeface.ITALIC, italic)
    if (underline != null) {
        text.getSpans(start, end, android.text.style.UnderlineSpan::class.java).forEach { text.removeSpan(it) }
        if (underline) {
            text.setSpan(
                android.text.style.UnderlineSpan(),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
}

private fun EditText?.applyColor(color: Int?) {
    val field = this ?: return
    val start = minOf(field.selectionStart, field.selectionEnd)
    val end = maxOf(field.selectionStart, field.selectionEnd)
    if (start < 0 || start == end) return
    val text = field.text
    text.getSpans(start, end, android.text.style.ForegroundColorSpan::class.java).forEach { text.removeSpan(it) }
    if (color != null) {
        text.setSpan(
            android.text.style.ForegroundColorSpan(color),
            start,
            end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
}
