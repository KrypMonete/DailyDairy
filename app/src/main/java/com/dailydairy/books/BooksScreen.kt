package com.dailydairy.books

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.dailydairy.diary.DiaryImages
import com.dailydairy.diary.barColors
import com.dailydairy.home.clickableCard
import kotlinx.coroutines.launch

@Composable
fun BooksRoute(onBack: () -> Unit, viewModel: BooksViewModel = viewModel()) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val hits by viewModel.hits.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val unreachable by viewModel.unreachable.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(BOOK_READING) }
    var looking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }

    val open = detail
    if (open != null) {
        val stored = saved.find { it.workKey == open.workKey }
        BookPage(
            hit = open.copy(overview = open.overview.ifBlank { stored?.overview.orEmpty() }),
            stored = stored,
            onShelf = { viewModel.save(open, it) },
            onRate = { score, note -> viewModel.rate(open, score, note) },
            onRemove = { stored?.let { viewModel.remove(it.id) } },
            onAuthor = {
                query = "author:${open.author}"
                looking = true
                viewModel.close()
            },
            onBack = viewModel::close,
        )
        return
    }

    Shelf(
        saved = saved,
        hits = hits,
        searching = searching,
        unreachable = unreachable,
        tab = tab,
        looking = looking,
        query = query,
        onTab = { tab = it },
        onLooking = { looking = it },
        onQuery = { query = it },
        onBack = onBack,
        onSearch = viewModel::search,
        onClear = viewModel::clearSearch,
        onOpenHit = viewModel::open,
        onOpenSaved = { book ->
            viewModel.open(BookHit(book.workKey, book.title, book.author, book.year, book.cover, book.overview))
        },
        onMove = { book, shelf ->
            viewModel.save(BookHit(book.workKey, book.title, book.author, book.year, book.cover, book.overview), shelf)
        },
        onFocus = { book ->
            viewModel.save(BookHit(book.workKey, book.title, book.author, book.year, book.cover, book.overview), BOOK_READING)
        },
        onRemove = { viewModel.remove(it.id) },
        onAdd = { title, author, year, overview, cover ->
            viewModel.addOwn(title, author, year, overview, cover)
            adding = false
            looking = false
            query = ""
            viewModel.clearSearch()
        },
        adding = adding,
        onAdding = { adding = it },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Shelf(
    saved: List<Book>,
    hits: List<BookHit>,
    searching: Boolean,
    unreachable: Boolean,
    tab: String,
    looking: Boolean,
    query: String,
    onTab: (String) -> Unit,
    onLooking: (Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onOpenHit: (BookHit) -> Unit,
    onOpenSaved: (Book) -> Unit,
    onMove: (Book, String) -> Unit,
    onFocus: (Book) -> Unit,
    onRemove: (Book) -> Unit,
    onAdd: (String, String, String, String, String) -> Unit,
    adding: Boolean,
    onAdding: (Boolean) -> Unit,
) {
    val browsing = looking || query.isNotBlank()
    val goBack = {
        if (looking) {
            onLooking(false)
            onQuery("")
            onClear()
        } else {
            onBack()
        }
    }
    BackHandler(onBack = goBack)
    LaunchedEffect(query) { onSearch(query) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (looking) {
                        OutlinedTextField(
                            value = query.removePrefix("author:"),
                            onValueChange = { onQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Kitap ara") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        )
                    } else {
                        Text("Kitaplar", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(
                            if (looking) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = if (looking) "Aramayı kapat" else "Geri",
                        )
                    }
                },
                actions = {
                    if (!looking) {
                        IconButton(onClick = { onLooking(true) }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Ara")
                        }
                    }
                },
                colors = barColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (browsing) {
                SearchList(hits, searching, unreachable, query, onOpenHit) { onAdding(true) }
            } else {
                Library(saved, tab, onTab, onOpenSaved, onMove, onFocus, onRemove)
            }
            if (!looking) {
                FloatingActionButton(
                    onClick = { onLooking(true) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Kitap ara")
                }
            }
        }
    }
    if (adding) {
        OwnBook(onAdd, onDismiss = { onAdding(false) })
    }
}

@Composable
private fun Library(
    saved: List<Book>,
    tab: String,
    onTab: (String) -> Unit,
    onOpen: (Book) -> Unit,
    onMove: (Book, String) -> Unit,
    onFocus: (Book) -> Unit,
    onRemove: (Book) -> Unit,
) {
    val groups = listOf(
        BOOK_READING to "Şu an",
        BOOK_WANT to "Okuyacaklarım",
        BOOK_DONE to "Okudum",
    )
    val start = groups.indexOfFirst { it.first == tab }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = start, pageCount = { groups.size })
    val scope = rememberCoroutineScope()
    val reading = saved.filter { it.shelf == BOOK_READING }.sortedByDescending { it.addedAt }
    val want = saved.filter { it.shelf == BOOK_WANT }
    val done = saved.filter { it.shelf == BOOK_DONE }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            groups.getOrNull(page)?.first?.let(onTab)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEachIndexed { index, (_, label) ->
                Chip(label, pagerState.currentPage == index) {
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
        ) { page ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (groups[page].first) {
                    BOOK_READING -> {
                        if (reading.isEmpty()) {
                            item { EmptyLine("Şu an okuduğun kitap yok.") }
                        } else {
                            val front = reading.first().id
                            items(reading, key = { it.id }) { book ->
                                BookRow(
                                    book,
                                    large = book.id == front,
                                    modifier = Modifier.animateItem(),
                                    onOpen = { if (book.id == front) onOpen(book) else onFocus(book) },
                                    onMove = { onMove(book, it) },
                                    onRemove = { onRemove(book) },
                                )
                            }
                        }
                    }
                    BOOK_WANT -> {
                        if (want.isEmpty()) {
                            item { EmptyLine("Okumak istediğin kitap yok.") }
                        } else {
                            items(want, key = { it.id }) { book ->
                                BookRow(book, onOpen = { onOpen(book) }, onMove = { onMove(book, it) }, onRemove = { onRemove(book) })
                            }
                        }
                    }
                    else -> {
                        if (done.isEmpty()) {
                            item { EmptyLine("Bitirdiğin kitap yok.") }
                        } else {
                            items(done, key = { it.id }) { book ->
                                BookRow(book, onOpen = { onOpen(book) }, onMove = { onMove(book, it) }, onRemove = { onRemove(book) })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun SearchList(
    hits: List<BookHit>,
    searching: Boolean,
    unreachable: Boolean,
    query: String,
    onOpen: (BookHit) -> Unit,
    onAdd: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        if (searching) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Aranıyor",
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (unreachable) {
            item { EmptyLine("Bağlantı yok.") }
        } else if (hits.isEmpty() && query.isNotBlank()) {
            item { EmptyLine("“${query.removePrefix("author:")}” için sonuç yok.") }
        }
        items(hits, key = { it.workKey }) { hit ->
            HitRow(hit) { onOpen(hit) }
        }
        item {
            TextButton(onClick = onAdd) { Text("Listede yok, kendim ekleyeyim") }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HitRow(hit: BookHit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(hit.cover, Modifier.size(width = 54.dp, height = 80.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(hit.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOf(hit.author, hit.year).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BookRow(
    book: Book,
    large: Boolean = false,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onMove: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val coverWidth by animateDpAsState(if (large) 92.dp else 54.dp, label = "coverWidth")
    val coverHeight by animateDpAsState(if (large) 138.dp else 80.dp, label = "coverHeight")
    var menu by remember { mutableStateOf(false) }
    val others = buildList {
        if (book.shelf != BOOK_READING) add(BOOK_READING to "Şu an okuyorum")
        if (book.shelf != BOOK_WANT) add(BOOK_WANT to "Okuyacaklarım")
        if (book.shelf != BOOK_DONE) add(BOOK_DONE to "Okudum")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onLongClick = { menu = true }, onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = if (large) Alignment.Top else Alignment.CenterVertically,
    ) {
        Cover(book.cover, Modifier.size(width = coverWidth, height = coverHeight))
        Column(modifier = Modifier.padding(start = 14.dp, top = if (large) 2.dp else 0.dp)) {
            Text(
                book.title,
                style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                maxLines = if (large) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (large) {
                if (book.author.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (book.year.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        book.year,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = listOf(book.author, book.year).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (book.shelf == BOOK_DONE && (book.score in 1..10 || book.note.isNotBlank())) {
                val bits = buildList {
                    if (book.score in 1..10) add("${book.score}/10")
                    if (book.note.isNotBlank()) add(book.note)
                }
                Text(
                    text = bits.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (menu) {
        Dialog(onDismissRequest = { menu = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(book.cover, Modifier.size(width = 40.dp, height = 58.dp))
                    Text(
                        text = book.title,
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                others.forEach { (shelf, label) ->
                    MenuLine(label) {
                        onMove(shelf)
                        menu = false
                    }
                }
                MenuLine("Listeden çıkar", danger = true) {
                    onRemove()
                    menu = false
                }
            }
        }
    }
}

@Composable
private fun MenuLine(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickableCard(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPage(
    hit: BookHit,
    stored: Book?,
    onShelf: (String) -> Unit,
    onRate: (Int, String) -> Unit,
    onRemove: () -> Unit,
    onAuthor: () -> Unit,
    onBack: () -> Unit,
) {
    val shelf = stored?.shelf
    var score by remember(stored?.id, stored?.score) { mutableStateOf(stored?.score ?: 0) }
    var note by remember(stored?.id, stored?.note) { mutableStateOf(stored?.note.orEmpty()) }
    val focusManager = LocalFocusManager.current
    val hideNote = { focusManager.clearFocus() }
    BackHandler(onBack = {
        hideNote()
        onBack()
    })
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        hit.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        hideNote()
                        onBack()
                    }) {
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
                .pointerInput(Unit) {
                    detectTapGestures { hideNote() }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row {
                Cover(hit.cover, Modifier.size(width = 120.dp, height = 180.dp))
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(hit.title, style = MaterialTheme.typography.titleLarge)
                    if (hit.author.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            hit.author,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickableCard(onClick = onAuthor)
                                .padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (hit.year.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            hit.year,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Şu an", shelf == BOOK_READING) {
                    hideNote()
                    onShelf(BOOK_READING)
                }
                Chip("Okuyacaklarım", shelf == BOOK_WANT) {
                    hideNote()
                    onShelf(BOOK_WANT)
                }
                Chip("Okudum", shelf == BOOK_DONE) {
                    hideNote()
                    onShelf(BOOK_DONE)
                }
            }
            if (shelf == BOOK_DONE) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Puanın",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..10).forEach { value ->
                        Text(
                            text = value.toString(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (score == value) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .clickableCard(onClick = {
                                    hideNote()
                                    score = if (score == value) 0 else value
                                    onRate(score, note)
                                })
                                .padding(horizontal = 7.dp, vertical = 6.dp),
                            color = if (score == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .onFocusChanged { if (!it.isFocused) onRate(score, note) },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 8,
                    label = { Text("Yorum") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { hideNote() }),
                )
            }
            if (shelf != null) {
                TextButton(onClick = {
                    hideNote()
                    onRemove()
                    onBack()
                }) { Text("Listeden çıkar") }
            }
            if (hit.overview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(hit.overview, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OwnBook(
    onSave: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var overview by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cover = DiaryImages.copy(context, uri, "book_covers").orEmpty()
        }
    }
    BackHandler(onBack = onDismiss)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
        ) {
            Text("Kendi kitabın", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(cover, Modifier.size(width = 72.dp, height = 108.dp))
                TextButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(if (cover.isBlank()) "Kapak seç" else "Kapağı değiştir") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Ad") },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Yazar") },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter { digit -> digit.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Yıl") },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = overview,
                onValueChange = { overview = it },
                modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
                minLines = 2,
                maxLines = 6,
                label = { Text("Açıklama") },
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
                TextButton(
                    onClick = { onSave(title, author, year, overview, cover) },
                    enabled = title.isNotBlank(),
                ) { Text("Ekle") }
            }
        }
    }
}

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickableCard(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun Cover(url: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isBlank()) {
            Icon(
                Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val model = if (url.startsWith("http")) {
                url
            } else {
                DiaryImages.file(LocalContext.current, url, "book_covers")
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
