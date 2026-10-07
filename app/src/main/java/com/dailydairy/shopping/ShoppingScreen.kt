package com.dailydairy.shopping

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailydairy.diary.barColors
import com.dailydairy.home.clickableCard

@Composable
fun ShoppingRoute(onBack: () -> Unit, viewModel: ShoppingViewModel = viewModel()) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    var openId by remember { mutableStateOf<Long?>(null) }
    val open = lists.find { it.list.id == openId }
    if (open != null) {
        ShoppingDetail(
            card = open,
            items = viewModel.items(open.list.id).collectAsState(emptyList()).value,
            onBack = { openId = null },
            onAdd = { viewModel.addItem(open.list.id, it, open.list.kind) },
            onToggle = viewModel::toggle,
            onCount = viewModel::setCount,
            onUnit = viewModel::cycleUnit,
            onPrice = viewModel::setPrice,
            onRewrite = { item, raw -> viewModel.rewriteItem(item, raw, open.list.kind) },
            onDeleteItems = viewModel::deleteItems,
            onClearDone = { viewModel.clearDone(open.list.id) },
            onDeleteList = {
                viewModel.deleteList(open.list.id)
                openId = null
            },
            onIcon = { viewModel.setIcon(open.list.id, it) },
        )
        return
    }
    ShoppingLists(
        lists = lists,
        items = allItems,
        onBack = onBack,
        onOpen = { openId = it.list.id },
        onCreate = { name, kind -> viewModel.createList(name, -1, kind) },
        onDelete = viewModel::deleteLists,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingLists(
    lists: List<ShoppingListCard>,
    items: List<ShoppingItem>,
    onBack: () -> Unit,
    onOpen: (ShoppingListCard) -> Unit,
    onCreate: (String, String) -> Unit,
    onDelete: (Collection<Long>) -> Unit,
) {
    var askName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LIST_PLAIN) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<Long>()) }
    var askDelete by remember { mutableStateOf(false) }
    val needle = query.trim()
    val shown = remember(lists, items, needle) {
        if (needle.isEmpty()) lists
        else lists.filter { card ->
            card.list.kindLabel().contains(needle, ignoreCase = true) ||
                card.list.name.contains(needle, ignoreCase = true) ||
                items.any { it.listId == card.list.id && it.name.contains(needle, ignoreCase = true) }
        }
    }
    val goBack = {
        if (picked.isNotEmpty()) picked = emptySet() else onBack()
    }
    BackHandler(onBack = goBack)
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
                        searching -> OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Tür, liste veya ürün") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { }),
                        )
                        else -> Text("Listeler", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (picked.isNotEmpty()) {
                        val visible = shown.map { it.list.id }.toSet()
                        val allOn = visible.isNotEmpty() && visible.all { it in picked }
                        IconButton(onClick = {
                            picked = if (allOn) emptySet() else visible
                        }) {
                            Icon(
                                Icons.Outlined.SelectAll,
                                contentDescription = "Hepsini seç",
                                tint = if (allOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { askDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Seçilenleri sil")
                        }
                    } else {
                        IconButton(onClick = {
                            if (searching) {
                                searching = false
                                query = ""
                            } else {
                                searching = true
                                picked = emptySet()
                            }
                        }) {
                            Icon(
                                if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = if (searching) "Aramayı kapat" else "Ara",
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
                lists.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Henüz liste yok.",
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
                            text = "“$needle” için sonuç yok.",
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
                        items(shown, key = { it.list.id }) { card ->
                            val hit = if (needle.isEmpty()) {
                                null
                            } else {
                                items.firstOrNull {
                                    it.listId == card.list.id && it.name.contains(needle, ignoreCase = true)
                                }?.name
                            }
                            ListRow(
                                card,
                                hint = hit,
                                selected = card.list.id in picked,
                                selecting = picked.isNotEmpty(),
                                onOpen = {
                                    if (picked.isNotEmpty()) {
                                        picked = if (card.list.id in picked) picked - card.list.id else picked + card.list.id
                                    } else {
                                        onOpen(card)
                                    }
                                },
                                onHold = {
                                    picked = if (card.list.id in picked) picked - card.list.id else picked + card.list.id
                                },
                            )
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
            if (picked.isEmpty() && !searching) {
                FloatingActionButton(
                    onClick = { askName = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Yeni liste")
                }
            }
        }
    }
    if (askName) {
        AlertDialog(
            onDismissRequest = { askName = false },
            title = { Text("Yeni liste") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Liste adı") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KindChip("Liste", kind == LIST_PLAIN) { kind = LIST_PLAIN }
                        KindChip("Alışveriş", kind == LIST_SHOP) { kind = LIST_SHOP }
                        KindChip("Tarif", kind == LIST_RECIPE) { kind = LIST_RECIPE }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreate(name, kind)
                        name = ""
                        kind = LIST_PLAIN
                        askName = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Oluştur") }
            },
            dismissButton = {
                TextButton(onClick = {
                    name = ""
                    kind = LIST_PLAIN
                    askName = false
                }) { Text("Vazgeç") }
            },
        )
    }
    if (askDelete && picked.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text(if (picked.size == 1) "Liste silinsin mi?" else "${picked.size} liste silinsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(picked)
                    picked = emptySet()
                    askDelete = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun ListRow(
    card: ShoppingListCard,
    hint: String? = null,
    selected: Boolean = false,
    selecting: Boolean = false,
    onOpen: () -> Unit,
    onHold: () -> Unit = {},
) {
    val tint = MaterialTheme.colorScheme.primary
    val line = when {
        hint != null -> hint
        card.total == 0 -> "Henüz bir şey yok"
        card.list.kind == LIST_SHOP && card.remaining == 0 -> "Sepet doldu"
        card.remaining == 0 -> "Hepsi bitti"
        else -> "${card.remaining} kaldı"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickableCard(onLongClick = onHold, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BagRing(
            tint = tint,
            total = card.total,
            remaining = card.remaining,
            icon = listIcon(card.list.tint),
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = card.list.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    hint != null -> tint
                    card.total > 0 && card.remaining == 0 -> tint
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (hint != null || (card.remaining == 0 && card.total > 0)) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            )
        }
        if (selecting) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
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

@Composable
private fun BagRing(
    tint: Color,
    total: Int,
    remaining: Int,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
) {
    val done = (total - remaining).coerceAtLeast(0)
    val target = if (total == 0) 0f else done / total.toFloat()
    val sweep = remember { Animatable(target) }
    LaunchedEffect(target) { sweep.animateTo(target, spring(dampingRatio = 0.62f, stiffness = 280f)) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            if (sweep.value > 0f) {
                drawArc(tint, -90f, 360f * sweep.value, false, style = stroke)
            }
        }
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingDetail(
    card: ShoppingListCard,
    items: List<ShoppingItem>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onCount: (ShoppingItem, Int) -> Unit,
    onUnit: (ShoppingItem) -> Unit,
    onPrice: (ShoppingItem, Int) -> Unit,
    onRewrite: (ShoppingItem, String) -> Unit,
    onDeleteItems: (Collection<ShoppingItem>) -> Unit,
    onClearDone: () -> Unit,
    onDeleteList: () -> Unit,
    onIcon: (Int) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var pricing by remember { mutableStateOf<ShoppingItem?>(null) }
    var priceDraft by remember { mutableStateOf("") }
    var askDelete by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<Long>()) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var editDraft by remember { mutableStateOf("") }
    val open = items.filter { !it.done }
    val done = items.filter { it.done }
    val kind = card.list.kind
    val shop = kind == LIST_SHOP
    val recipe = kind == LIST_RECIPE
    val tint = MaterialTheme.colorScheme.primary
    val full = card.total > 0 && card.remaining == 0
    val goBack = {
        if (picked.isNotEmpty()) picked = emptySet() else onBack()
    }
    BackHandler(onBack = goBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (picked.isNotEmpty()) {
                        Text("${picked.size} seçildi", style = MaterialTheme.typography.headlineMedium)
                    } else {
                        Text(
                            card.list.name,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (picked.isNotEmpty()) {
                        val visible = items.map { it.id }.toSet()
                        val allOn = visible.isNotEmpty() && visible.all { it in picked }
                        IconButton(onClick = {
                            picked = if (allOn) emptySet() else visible
                        }) {
                            Icon(
                                Icons.Outlined.SelectAll,
                                contentDescription = "Hepsini seç",
                                tint = if (allOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (picked.size == 1) {
                            IconButton(onClick = {
                                val item = items.find { it.id in picked } ?: return@IconButton
                                editDraft = item.editText(recipe, shop)
                                editing = item
                            }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Düzenle")
                            }
                        }
                        IconButton(onClick = {
                            onDeleteItems(items.filter { it.id in picked })
                            picked = emptySet()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Seçilenleri sil")
                        }
                    } else {
                        IconButton(onClick = { askDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Listeyi sil")
                        }
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
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hover = remember { MutableInteractionSource() }
                val hovered by hover.collectIsHoveredAsState()
                LaunchedEffect(hovered) { if (hovered) picking = true }
                BagRing(
                    tint = tint,
                    total = card.total,
                    remaining = card.remaining,
                    icon = if (card.list.tint < 0) Icons.Outlined.Edit else listIcon(card.list.tint),
                    modifier = Modifier
                        .size(64.dp)
                        .hoverable(hover)
                        .clickableCard(onClick = { picking = !picking }),
                )
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    AnimatedContent(
                        targetState = full,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "bag",
                    ) { filled ->
                        Text(
                            text = when {
                                full && shop -> "Sepet doldu"
                                full -> "Hepsi bitti"
                                card.total == 0 -> "Liste boş"
                                else -> "${card.remaining} kaldı"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (filled) tint else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = when {
                            card.total == 0 -> if (recipe) "Alta bir malzeme yaz" else "Alta bir madde yaz"
                            shop -> "${card.total - card.remaining} / ${card.total}  ·  ${kurusText(items.openTotal().toInt())}"
                            else -> "${card.total - card.remaining} / ${card.total}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (picking) {
                IconChoices(card.list.tint) {
                    onIcon(it)
                    picking = false
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = if (recipe) "Liste boş. Alta bir malzeme yaz." else "Liste boş. Alta bir madde yaz.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(open, key = { it.id }) { item ->
                        ItemRow(
                            item,
                            tint,
                            selecting = picked.isNotEmpty(),
                            selected = item.id in picked,
                            onToggle = { onToggle(item) },
                            onCount = { onCount(item, it) },
                            onUnit = { onUnit(item) },
                            onPrice = {
                                pricing = item
                                priceDraft = if (item.price == 0) "" else item.moneyText().removeSuffix(" ₺")
                            },
                            measured = recipe,
                            shop = shop,
                            onHold = {
                                picked = if (item.id in picked) picked - item.id else picked + item.id
                            },
                            onPick = {
                                picked = if (item.id in picked) picked - item.id else picked + item.id
                            },
                        )
                    }
                    if (done.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (shop) "Alınanlar" else "Bitenler",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onClearDone) { Text("Temizle") }
                            }
                        }
                        items(done, key = { it.id }) { item ->
                            ItemRow(
                                item,
                                tint,
                                selecting = picked.isNotEmpty(),
                                selected = item.id in picked,
                                onToggle = { onToggle(item) },
                                onCount = { onCount(item, it) },
                                onUnit = { onUnit(item) },
                                onPrice = {
                                    pricing = item
                                    priceDraft = if (item.price == 0) "" else item.moneyText().removeSuffix(" ₺")
                                },
                                measured = recipe,
                                shop = shop,
                                onHold = {
                                    picked = if (item.id in picked) picked - item.id else picked + item.id
                                },
                                onPick = {
                                    picked = if (item.id in picked) picked - item.id else picked + item.id
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(if (recipe) "Malzeme" else "Madde") },
                )
                IconButton(
                    onClick = {
                        onAdd(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Ekle")
                }
            }
        }
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Liste silinsin mi?") },
            text = { Text(card.list.name) },
            confirmButton = {
                TextButton(onClick = onDeleteList) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("Vazgeç") }
            },
        )
    }
    editing?.let { item ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Düzenle") },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    singleLine = true,
                    label = { Text(if (recipe) "Malzeme" else "Madde") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRewrite(item, editDraft)
                        editing = null
                        picked = emptySet()
                    },
                    enabled = editDraft.isNotBlank(),
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Vazgeç") }
            },
        )
    }
    pricing?.let { item ->
        val parsed = parseKurus(priceDraft)
        AlertDialog(
            onDismissRequest = { pricing = null },
            title = { Text("Fiyat") },
            text = {
                OutlinedTextField(
                    value = priceDraft,
                    onValueChange = { priceDraft = it },
                    singleLine = true,
                    label = { Text("Tutar") },
                    suffix = { Text("₺") },
                    isError = parsed == null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        parsed?.let { onPrice(item, it) }
                        pricing = null
                    },
                    enabled = parsed != null,
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { pricing = null }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun ItemRow(
    item: ShoppingItem,
    tint: Color,
    selecting: Boolean,
    selected: Boolean,
    measured: Boolean,
    shop: Boolean,
    onToggle: () -> Unit,
    onCount: (Int) -> Unit,
    onUnit: () -> Unit,
    onPrice: () -> Unit,
    onHold: () -> Unit,
    onPick: () -> Unit,
) {
    var bounce by remember(item.id) { mutableIntStateOf(0) }
    val scale = remember(item.id) { Animatable(1f) }
    LaunchedEffect(bounce) {
        if (bounce == 0) return@LaunchedEffect
        scale.snapTo(0.86f)
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickableCard(onLongClick = onHold) { if (selecting) onPick() else onToggle() }
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(RoundedCornerShape(7.dp))
                .background(
                    when {
                        selected -> tint
                        item.done -> tint
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.done || selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        val nameColor = if (item.done) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        val note = item.name.substringAfterLast(" (", "").substringBeforeLast(")")
        val hasNote = !measured && item.name.endsWith(")") && note.isNotEmpty() && item.name.contains(" (")
        Text(
            text = buildAnnotatedString {
                if (hasNote) {
                    append(item.name.substringBeforeLast(" ("))
                    withStyle(SpanStyle(color = tint)) {
                        append(" ($note)")
                    }
                } else {
                    append(item.name)
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (item.done) FontWeight.Normal else FontWeight.Medium,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            color = nameColor,
        )
        if (measured) {
            CountButton(Icons.Outlined.Remove, "Azalt", !selecting && item.count > 1) {
                onCount(item.count - item.measureStep())
            }
            Text(
                text = item.amountText(),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableCard(onClick = { if (!selecting && !shop) onUnit() })
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CountButton(Icons.Outlined.Add, "Arttır", !selecting && item.count < item.measureMax()) {
                onCount(item.count + item.measureStep())
            }
        }
        if (shop) {
            Text(
                text = if (item.price == 0) "Fiyat" else item.moneyText(),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableCard(onClick = { if (!selecting) onPrice() })
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (item.price == 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun KindChip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickableCard(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun CountButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

private val listIcons = listOf(
    Icons.Outlined.ShoppingBag,
    Icons.Outlined.LocalGroceryStore,
    Icons.Outlined.Restaurant,
    Icons.Outlined.LunchDining,
    Icons.Outlined.BakeryDining,
    Icons.Outlined.Cake,
    Icons.Outlined.Icecream,
    Icons.Outlined.Coffee,
    Icons.Outlined.LocalFlorist,
    Icons.Outlined.Spa,
    Icons.Outlined.Home,
    Icons.Outlined.Bedtime,
    Icons.Outlined.Pets,
    Icons.Outlined.Favorite,
    Icons.Outlined.Checkroom,
    Icons.Outlined.FitnessCenter,
    Icons.Outlined.LocalPharmacy,
    Icons.Outlined.School,
    Icons.Outlined.Book,
    Icons.Outlined.Work,
    Icons.Outlined.Flight,
    Icons.Outlined.DirectionsCar,
    Icons.Outlined.Movie,
    Icons.Outlined.MusicNote,
    Icons.Outlined.Brush,
)

private fun listIcon(index: Int): ImageVector? =
    if (index < 0) null else listIcons.getOrNull(index)

@Composable
private fun IconChoices(selected: Int, onPick: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(listIcons.size) { index ->
            val icon = listIcons[index]
            val on = index == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (on) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                    )
                    .clickableCard(onClick = { onPick(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
