package com.dailydairy.notes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.ImeAction
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
import com.dailydairy.diary.DiaryImages
import com.dailydairy.diary.MediaFill
import com.dailydairy.diary.MediaFull
import com.dailydairy.diary.MediaWide
import com.dailydairy.diary.RichEditor
import com.dailydairy.diary.barColors
import com.dailydairy.home.clickableCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr"))

@Composable
fun NotesRoute(onBack: () -> Unit, viewModel: NotesViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var reading by remember { mutableStateOf<Note?>(null) }
    var editing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var folderId by remember { mutableStateOf<Long?>(null) }
    val open = reading?.let { current -> notes.find { it.id == current.id } ?: current }
    val folder = folders.find { it.id == folderId }

    if (creating || (open != null && editing)) {
        NoteEditor(
            note = if (creating) null else open,
            onBack = {
                editing = false
                creating = false
            },
            onSave = { title, body, html, images ->
                val wasNew = creating
                val target = if (wasNew) folderId ?: 0L else open?.folderId ?: 0L
                viewModel.save(if (wasNew) null else open?.id, title, body, html, images, target)
                editing = false
                creating = false
                if (wasNew) reading = null
            },
        )
        return
    }

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<Long>()) }
    var askDelete by remember { mutableStateOf(false) }
    var askFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var askMove by remember { mutableStateOf(false) }
    var moveNeedsFolder by remember { mutableStateOf(false) }
    var askDropFolder by remember { mutableStateOf(false) }
    var pickedFolders by remember { mutableStateOf(setOf<Long>()) }
    var renameFolder by remember { mutableStateOf<NoteFolder?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    if (open != null) {
        NoteReader(
            note = open,
            query = query,
            onBack = { reading = null },
            onEdit = { editing = true },
        )
        return
    }

    NoteList(
        notes = notes,
        folders = folders,
        openFolder = folder,
        searching = searching,
        query = query,
        picked = picked,
        pickedFolders = pickedFolders,
        onQuery = { query = it },
        onToggleSearch = {
            if (searching) {
                searching = false
                query = ""
            } else {
                searching = true
                picked = emptySet()
                pickedFolders = emptySet()
            }
        },
        onBack = {
            when {
                picked.isNotEmpty() || pickedFolders.isNotEmpty() -> {
                    picked = emptySet()
                    pickedFolders = emptySet()
                }
                searching -> {
                    searching = false
                    query = ""
                }
                folderId != null -> folderId = null
                else -> onBack()
            }
        },
        onOpen = { note ->
            if (picked.isNotEmpty()) {
                picked = if (note.id in picked) picked - note.id else picked + note.id
            } else {
                reading = note
            }
        },
        onHold = { note ->
            pickedFolders = emptySet()
            picked = if (note.id in picked) picked - note.id else picked + note.id
        },
        onOpenFolder = {
            if (picked.isNotEmpty() || pickedFolders.isNotEmpty()) return@NoteList
            picked = emptySet()
            folderId = it.id
        },
        onHoldFolder = { item ->
            picked = emptySet()
            pickedFolders = if (item.id in pickedFolders) pickedFolders - item.id else setOf(item.id)
        },
        onCreate = { creating = true },
        onAskFolder = { askFolder = true },
        onAskDelete = { askDelete = true },
        onAskMove = {
            if (folders.isEmpty()) {
                moveNeedsFolder = true
                askFolder = true
            } else {
                askMove = true
            }
        },
        onAskDropFolder = { askDropFolder = true },
        onPin = {
            if (pickedFolders.isNotEmpty()) {
                val folder = folders.find { it.id in pickedFolders }
                if (folder != null) viewModel.toggleFolderPin(folder.id, folder.pinned)
                pickedFolders = emptySet()
            } else {
                viewModel.togglePin(picked)
                picked = emptySet()
            }
        },
        onSelectAll = { visible ->
            picked = if (visible.all { it in picked }) emptySet() else visible.toSet()
        },
        onEdit = {
            val note = notes.find { it.id in picked }
            if (note != null) {
                picked = emptySet()
                reading = note
                editing = true
            } else {
                val item = folders.find { it.id in pickedFolders } ?: return@NoteList
                renameDraft = item.name
                renameFolder = item
                pickedFolders = emptySet()
            }
        },
    )

    if (askDelete && picked.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text(if (picked.size == 1) "Not silinsin mi?" else "${picked.size} not silinsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll(picked)
                    picked = emptySet()
                    pickedFolders = emptySet()
                    askDelete = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("Vazgeç") }
            },
        )
    }

    if (askFolder) {
        AlertDialog(
            onDismissRequest = {
                moveNeedsFolder = false
                askFolder = false
            },
            title = { Text(if (moveNeedsFolder) "Önce klasör oluştur" else "Yeni klasör") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    label = { Text("Klasör adı") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createFolder(folderName, if (moveNeedsFolder) picked else emptyList())
                        if (moveNeedsFolder) picked = emptySet()
                        folderName = ""
                        moveNeedsFolder = false
                        askFolder = false
                    },
                    enabled = folderName.isNotBlank(),
                ) { Text("Oluştur") }
            },
            dismissButton = {
                TextButton(onClick = {
                    folderName = ""
                    moveNeedsFolder = false
                    askFolder = false
                }) { Text("Vazgeç") }
            },
        )
    }

    val renaming = renameFolder
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renameFolder = null },
            title = { Text("Klasör adı") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    label = { Text("Klasör adı") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameFolder(renaming.id, renameDraft)
                        renameFolder = null
                    },
                    enabled = renameDraft.isNotBlank(),
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { renameFolder = null }) { Text("Vazgeç") }
            },
        )
    }

    if (askMove && picked.isNotEmpty()) {
        Dialog(onDismissRequest = { askMove = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            ) {
                Text(
                    text = "Klasöre taşı",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (folder != null) {
                    MoveLine("Klasörden çıkar") {
                        viewModel.moveNotes(picked, 0)
                        picked = emptySet()
                        askMove = false
                    }
                }
                folders.forEach { item ->
                    MoveLine(item.name) {
                        viewModel.moveNotes(picked, item.id)
                        picked = emptySet()
                        askMove = false
                    }
                }
            }
        }
    }

    if (askDropFolder && folder != null) {
        AlertDialog(
            onDismissRequest = { askDropFolder = false },
            title = { Text("Klasör silinsin mi?") },
            text = { Text("İçindeki notlar klasörsüz kalır.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    folderId = null
                    askDropFolder = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { askDropFolder = false }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun MoveLine(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickableCard(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteList(
    notes: List<Note>,
    folders: List<NoteFolder>,
    openFolder: NoteFolder?,
    searching: Boolean,
    query: String,
    picked: Set<Long>,
    pickedFolders: Set<Long>,
    onQuery: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onBack: () -> Unit,
    onOpen: (Note) -> Unit,
    onHold: (Note) -> Unit,
    onOpenFolder: (NoteFolder) -> Unit,
    onHoldFolder: (NoteFolder) -> Unit,
    onCreate: () -> Unit,
    onAskFolder: () -> Unit,
    onAskDelete: () -> Unit,
    onAskMove: () -> Unit,
    onAskDropFolder: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onSelectAll: (Set<Long>) -> Unit,
) {
    val needle = query.trim()
    val inFolder = if (openFolder == null) notes else notes.filter { it.folderId == openFolder.id }
    val loose = if (openFolder == null && needle.isEmpty()) {
        notes.filter { it.folderId == 0L && !it.pinned }
    } else {
        inFolder
    }
    val pinnedTop = if (openFolder == null && needle.isEmpty()) {
        notes.filter { it.pinned }.sortedByDescending { it.updatedAt }
    } else {
        emptyList()
    }
    val shown = remember(loose, needle, openFolder) {
        val matched = if (needle.isEmpty()) loose
        else inFolder.filter {
            it.title.contains(needle, ignoreCase = true) ||
                it.body.contains(needle, ignoreCase = true)
        }
        matched.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }
    val visibleIds = (pinnedTop + shown).map { it.id }.toSet()
    val allOn = visibleIds.isNotEmpty() && visibleIds.all { it in picked }
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    when {
                        picked.isNotEmpty() -> Text(
                            "${picked.size} seçildi",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        pickedFolders.isNotEmpty() -> Text(
                            "1 seçildi",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        searching -> OutlinedTextField(
                            value = query,
                            onValueChange = onQuery,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Başlık veya not") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { }),
                        )
                        else -> Text(
                            openFolder?.name ?: "Notlar",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (picked.isNotEmpty()) {
                        IconButton(onClick = { onSelectAll(visibleIds) }) {
                            Icon(
                                Icons.Outlined.SelectAll,
                                contentDescription = "Hepsini seç",
                                tint = if (allOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else if (pickedFolders.isEmpty()) {
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = if (searching) "Aramayı kapat" else "Ara",
                            )
                        }
                        if (!searching && openFolder == null) {
                            IconButton(onClick = onAskFolder) {
                                Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Yeni klasör")
                            }
                        }
                        if (!searching && openFolder != null) {
                            IconButton(onClick = onAskDropFolder) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Klasörü sil")
                            }
                        }
                    }
                },
                colors = barColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                notes.isEmpty() && folders.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = if (openFolder == null) "Henüz not yok." else "Bu klasör boş.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                shown.isEmpty() && needle.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "“$needle” için sonuç yok.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                shown.isEmpty() && folders.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Bu klasör boş.",
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
                        if (pinnedTop.isNotEmpty()) {
                            items(pinnedTop, key = { "pin-${it.id}" }) { note ->
                                NoteRow(
                                    note,
                                    query,
                                    selected = note.id in picked,
                                    selecting = picked.isNotEmpty(),
                                    onOpen = { onOpen(note) },
                                    onHold = { onHold(note) },
                                )
                            }
                        }
                        if (openFolder == null && needle.isEmpty()) {
                            items(folders, key = { "folder-${it.id}" }) { item ->
                                val count = notes.count { it.folderId == item.id }
                                FolderRow(
                                    item,
                                    count,
                                    selected = item.id in pickedFolders,
                                    onOpen = { onOpenFolder(item) },
                                    onHold = { onHoldFolder(item) },
                                )
                            }
                        }
                        items(shown, key = { it.id }) { note ->
                            NoteRow(
                                note,
                                query,
                                selected = note.id in picked,
                                selecting = picked.isNotEmpty(),
                                onOpen = { onOpen(note) },
                                onHold = { onHold(note) },
                            )
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
            if (picked.isNotEmpty() || pickedFolders.isNotEmpty()) {
                val unpin = if (pickedFolders.isNotEmpty()) {
                    folders.any { it.id in pickedFolders && it.pinned }
                } else {
                    notes.any { it.id in picked && it.pinned }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp, vertical = 18.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (picked.size == 1 || pickedFolders.size == 1) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Düzenle")
                        }
                    }
                    if (picked.isNotEmpty()) {
                        IconButton(onClick = onAskMove) {
                            Icon(Icons.Outlined.DriveFileMove, contentDescription = "Klasöre taşı")
                        }
                        IconButton(onClick = onAskDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Seçilenleri sil")
                        }
                    }
                    IconButton(onClick = onPin) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = if (unpin) "Tutturmayı kaldır" else "Başa tuttur",
                            tint = if (unpin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else if (!searching) {
                FloatingActionButton(
                    onClick = onCreate,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Yeni not")
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: NoteFolder,
    count: Int,
    selected: Boolean,
    onOpen: () -> Unit,
    onHold: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickableCard(onLongClick = onHold, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (count == 0) "Boş" else "$count not",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (folder.pinned) {
            Icon(
                Icons.Outlined.PushPin,
                contentDescription = "Başa tutturuldu",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    query: String,
    selected: Boolean,
    selecting: Boolean,
    onOpen: () -> Unit,
    onHold: () -> Unit,
) {
    val context = LocalContext.current
    val date = remember(note.updatedAt) {
        Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).format(dayFormat)
    }
    val thumb = remember(note.images) {
        note.imageNames().firstOrNull()?.let { name ->
            if (DiaryImages.isGif(name)) null
            else DiaryImages.decode(DiaryImages.file(context, name, "note_images"), 240)?.asImageBitmap()
        }
    }
    val first = note.imageNames().firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickableCard(onLongClick = onHold, onClick = onOpen)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (first != null && DiaryImages.isGif(first)) {
            MediaFill(
                name = first,
                folder = "note_images",
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
            val preview = afterHeadline(note.title, note.body)
            MarkedText(
                text = headline(note.title, note.body),
                query = query,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        if (note.pinned && !selecting) {
            Icon(
                Icons.Outlined.PushPin,
                contentDescription = "Başa tutturuldu",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(18.dp),
            )
        }
        if (selecting) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteReader(note: Note, query: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    var openImage by remember { mutableStateOf<String?>(null) }
    val stamp = remember(note.createdAt) {
        Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).format(dayFormat)
    }
    val html = remember(note.id, note.bodyHtml, note.body) {
        when {
            note.bodyHtml.isNotBlank() -> note.bodyHtml
            note.body.isNotBlank() -> HtmlCompat.toHtml(
                android.text.SpannableStringBuilder(note.body),
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
                title = { Text("Not", style = MaterialTheme.typography.headlineMedium) },
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
            if (note.title.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                MarkedText(
                    text = note.title,
                    query = query,
                    style = MaterialTheme.typography.headlineSmall,
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
            if (note.imageNames().isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                note.imageNames().forEach { name ->
                    MediaWide(
                        name = name,
                        folder = "note_images",
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openImage = name },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    openImage?.let { name ->
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
                MediaFull(name, "note_images")
            }
        }
    }
}

@Composable
private fun NoteEditor(
    note: Note?,
    onBack: () -> Unit,
    onSave: (title: String, body: String, bodyHtml: String, images: String) -> Unit,
) {
    val html = remember(note?.id) {
        when {
            !note?.bodyHtml.isNullOrBlank() -> note.bodyHtml
            !note?.body.isNullOrBlank() -> HtmlCompat.toHtml(
                android.text.SpannableStringBuilder(note.body),
                HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE,
            )
            else -> ""
        }
    }
    RichEditor(
        titleText = if (note == null) "Yeni not" else "Not",
        initialTitle = note?.title.orEmpty(),
        initialHtml = html,
        initialImages = note?.imageNames().orEmpty(),
        imageFolder = "note_images",
        bodyHint = "Not",
        rememberKey = note?.id,
        onBack = onBack,
        onSave = onSave,
    )
}
