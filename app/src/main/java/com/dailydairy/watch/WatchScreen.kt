package com.dailydairy.watch

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Movie
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.dailydairy.diary.barColors
import com.dailydairy.home.clickableCard
import kotlinx.coroutines.launch

@Composable
fun WatchRoute(onBack: () -> Unit, viewModel: WatchViewModel = viewModel()) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val hits by viewModel.hits.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val imdb by viewModel.imdb.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(KIND_MOVIE) }
    var shelf by remember { mutableStateOf(SHELF_WANT) }
    var looking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val open = detail
    if (open != null) {
        val stored = saved.find { it.tmdbId == open.hit.tmdbId && it.kind == open.hit.kind }
        TitlePage(
            detail = open,
            imdb = imdb,
            stored = stored,
            onShelf = { viewModel.save(open.hit, it) },
            onRate = { score, note -> viewModel.rate(open.hit, score, note) },
            onRemove = { stored?.let { viewModel.remove(it.id) } },
            onBack = viewModel::close,
        )
        return
    }

    ShelfPage(
        saved = saved,
        hits = hits,
        searching = searching,
        kind = kind,
        shelf = shelf,
        looking = looking,
        query = query,
        onKind = { kind = it },
        onShelf = { shelf = it },
        onLooking = { looking = it },
        onQuery = { query = it },
        onBack = onBack,
        onSearch = viewModel::search,
        onClear = viewModel::clearSearch,
        onOpenHit = viewModel::open,
        onOpenSaved = { title ->
            viewModel.open(
                TitleHit(title.tmdbId, title.kind, title.title, title.year, title.poster, title.overview),
            )
        },
        onMove = { title, shelf ->
            viewModel.save(
                TitleHit(title.tmdbId, title.kind, title.title, title.year, title.poster, title.overview),
                shelf,
            )
        },
        onRemove = { viewModel.remove(it.id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfPage(
    saved: List<WatchTitle>,
    hits: List<TitleHit>,
    searching: Boolean,
    kind: String,
    shelf: String,
    looking: Boolean,
    query: String,
    onKind: (String) -> Unit,
    onShelf: (String) -> Unit,
    onLooking: (Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onOpenHit: (TitleHit) -> Unit,
    onOpenSaved: (WatchTitle) -> Unit,
    onMove: (WatchTitle, String) -> Unit,
    onRemove: (WatchTitle) -> Unit,
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
    LaunchedEffect(kind) {
        if (kind == KIND_MOVIE && shelf == SHELF_WATCHING) onShelf(SHELF_WANT)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (looking) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { onQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(if (kind == KIND_TV) "Dizi ara" else "Film ara") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        )
                    } else {
                        Text("İzleme listem", style = MaterialTheme.typography.headlineMedium)
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
            when {
                !Tmdb.ready -> MissingKey()
                browsing -> SearchList(hits, searching, query, kind, onKind, onOpenHit)
                else -> ShelfList(saved, kind, shelf, onKind, onShelf, onOpenSaved, onMove, onRemove)
            }
            if (Tmdb.ready && !looking) {
                FloatingActionButton(
                    onClick = { onLooking(true) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Film veya dizi ara")
                }
            }
        }
    }
}

@Composable
private fun MissingKey() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Film ve dizi için ücretsiz bir TMDB anahtarı gerekiyor. local.properties dosyasındaki TMDB_KEY satırına yazıp yeniden kur.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SearchList(
    hits: List<TitleHit>,
    searching: Boolean,
    query: String,
    kind: String,
    onKind: (String) -> Unit,
    onOpen: (TitleHit) -> Unit,
) {
    val shown = hits.filter { it.kind == kind }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item { KindSwitch(kind, onKind) }
        if (searching) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "Aranıyor",
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (shown.isEmpty() && query.isNotBlank()) {
            item {
                Text(
                    text = "“$query” için sonuç yok.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(shown, key = { "${it.kind}-${it.tmdbId}" }) { hit ->
            HitRow(hit) { onOpen(hit) }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun ShelfList(
    saved: List<WatchTitle>,
    kind: String,
    shelf: String,
    onKind: (String) -> Unit,
    onShelf: (String) -> Unit,
    onOpen: (WatchTitle) -> Unit,
    onMove: (WatchTitle, String) -> Unit,
    onRemove: (WatchTitle) -> Unit,
) {
    val groups = if (kind == KIND_TV) {
        listOf(SHELF_WATCHING to "İzliyorum", SHELF_WANT to "İzleyeceğim", SHELF_DONE to "İzledim")
    } else {
        listOf(SHELF_WANT to "İzleyeceğim", SHELF_DONE to "İzledim")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(4.dp))
            KindSwitch(kind, onKind)
            Spacer(Modifier.height(12.dp))
        }
        key(kind) {
            val start = groups.indexOfFirst { it.first == shelf }.coerceAtLeast(0)
            val pagerState = rememberPagerState(initialPage = start, pageCount = { groups.size })
            val scope = rememberCoroutineScope()
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    groups.getOrNull(page)?.first?.let(onShelf)
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEachIndexed { index, (_, label) ->
                    ShelfChip(label, pagerState.currentPage == index) {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
            ) { page ->
                val rows = saved.filter { it.kind == kind && it.shelf == groups[page].first }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (rows.isEmpty()) {
                        item {
                            Text(
                                text = "Bu listede bir şey yok.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    items(rows, key = { it.id }) { title ->
                        SavedRow(
                            title,
                            onOpen = { onOpen(title) },
                            onMove = { onMove(title, it) },
                            onRemove = { onRemove(title) },
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}

@Composable
private fun KindSwitch(kind: String, onKind: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShelfChip("Film", kind == KIND_MOVIE) { onKind(KIND_MOVIE) }
        ShelfChip("Dizi", kind == KIND_TV) { onKind(KIND_TV) }
    }
}

@Composable
private fun HitRow(hit: TitleHit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(hit.poster, Modifier.size(width = 54.dp, height = 80.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(hit.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOf(kindLabel(hit.kind), hit.year).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedRow(
    title: WatchTitle,
    onOpen: () -> Unit,
    onMove: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val others = buildList {
        if (title.shelf != SHELF_WANT) add(SHELF_WANT to "İzleyeceğim")
        if (title.kind == KIND_TV && title.shelf != SHELF_WATCHING) add(SHELF_WATCHING to "İzliyorum")
        if (title.shelf != SHELF_DONE) add(SHELF_DONE to "İzledim")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onLongClick = { menu = true }, onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(title.poster, Modifier.size(width = 54.dp, height = 80.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOf(kindLabel(title.kind), title.year).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (title.shelf == SHELF_DONE && (title.score in 1..10 || title.note.isNotBlank())) {
                val bits = buildList {
                    if (title.score in 1..10) add("${title.score}/10")
                    if (title.note.isNotBlank()) add(title.note)
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
                    Poster(title.poster, Modifier.size(width = 40.dp, height = 58.dp))
                    Text(
                        text = title.title,
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
private fun TitlePage(
    detail: TitleDetail,
    imdb: String,
    stored: WatchTitle?,
    onShelf: (String) -> Unit,
    onRate: (Int, String) -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
) {
    val hit = detail.hit
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
                title = { Text(hit.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                Poster(hit.poster, Modifier.size(width = 120.dp, height = 180.dp))
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(hit.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOf(kindLabel(hit.kind), hit.year).filter { it.isNotBlank() }.joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (imdb.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "IMDb $imdb",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShelfChip("İzleyeceğim", shelf == SHELF_WANT) {
                    hideNote()
                    onShelf(SHELF_WANT)
                }
                if (hit.kind == KIND_TV) {
                    ShelfChip("İzliyorum", shelf == SHELF_WATCHING) {
                        hideNote()
                        onShelf(SHELF_WATCHING)
                    }
                }
                ShelfChip("İzledim", shelf == SHELF_DONE) {
                    hideNote()
                    onShelf(SHELF_DONE)
                }
            }
            if (shelf == SHELF_DONE) {
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
            if (detail.cast.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Oyuncular",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                detail.cast.forEach { person ->
                    CastRow(person)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Kapak ve özet TMDB.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShelfChip(label: String, on: Boolean, onClick: () -> Unit) {
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
private fun CastRow(person: CastMember) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Poster(person.photo, Modifier.size(44.dp).clip(CircleShape))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(person.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (person.role.isNotBlank()) {
                Text(
                    person.role,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Poster(url: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isBlank()) {
            Icon(
                Icons.Outlined.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun kindLabel(kind: String): String = if (kind == KIND_TV) "Dizi" else "Film"
