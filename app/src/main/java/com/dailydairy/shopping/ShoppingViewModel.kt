package com.dailydairy.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ShoppingDatabase.get(application).shoppingDao()

    val lists: StateFlow<List<ShoppingListCard>> = dao.observeLists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val allItems: StateFlow<List<ShoppingItem>> = dao.observeAllItems().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun items(listId: Long) = dao.observeItems(listId)

    fun createList(name: String, icon: Int, kind: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.insertList(
                ShoppingList(
                    name = trimmed,
                    tint = icon,
                    kind = kind,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun setIcon(id: Long, icon: Int) {
        viewModelScope.launch { dao.setIcon(id, icon) }
    }

    fun deleteList(id: Long) = deleteLists(listOf(id))

    fun deleteLists(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val list = ids.toList()
            dao.deleteItemsOf(list)
            dao.deleteLists(list)
        }
    }

    fun addItem(listId: Long, raw: String, kind: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val priced = if (kind == LIST_SHOP) parsePrice(trimmed) else null
        val parsed = when {
            priced != null -> Measure(priced.name, 1, "")
            kind == LIST_PLAIN -> Measure(trimmed, 1, "")
            kind == LIST_SHOP -> Measure(shopName(trimmed), 1, "")
            else -> parseMeasure(trimmed)
        }
        viewModelScope.launch {
            val position = dao.maxPosition(listId) + 1
            dao.insertItem(
                ShoppingItem(
                    listId = listId,
                    name = parsed.name,
                    count = if (kind == LIST_PLAIN || kind == LIST_SHOP) 1 else parsed.count,
                    unit = if (kind == LIST_RECIPE) parsed.unit else "",
                    price = priced?.kurus ?: 0,
                    position = position,
                ),
            )
            dao.touch(listId, System.currentTimeMillis())
        }
    }

    fun rewriteItem(item: ShoppingItem, raw: String, kind: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val priced = if (kind == LIST_SHOP) parsePrice(trimmed) else null
        val parsed = when {
            priced != null -> Measure(priced.name, 1, "")
            kind == LIST_PLAIN -> Measure(trimmed, 1, "")
            kind == LIST_SHOP -> Measure(shopName(trimmed), 1, "")
            else -> parseMeasure(trimmed)
        }
        val next = item.copy(
            name = parsed.name,
            count = when (kind) {
                LIST_RECIPE -> parsed.count
                LIST_SHOP -> item.count
                else -> 1
            },
            unit = if (kind == LIST_RECIPE) parsed.unit else item.unit,
            price = priced?.kurus ?: item.price,
        )
        viewModelScope.launch {
            dao.updateItem(next)
            dao.touch(item.listId, System.currentTimeMillis())
        }
    }

    fun setPrice(item: ShoppingItem, price: Int) {
        viewModelScope.launch {
            dao.updateItem(item.copy(price = price.coerceAtLeast(0)))
            dao.touch(item.listId, System.currentTimeMillis())
        }
    }

    fun setCount(item: ShoppingItem, count: Int) {
        viewModelScope.launch {
            dao.updateItem(item.copy(count = count.coerceIn(1, item.measureMax())))
            dao.touch(item.listId, System.currentTimeMillis())
        }
    }

    fun cycleUnit(item: ShoppingItem) {
        val (count, unit) = item.cycled()
        viewModelScope.launch {
            dao.updateItem(item.copy(count = count, unit = unit))
            dao.touch(item.listId, System.currentTimeMillis())
        }
    }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch {
            dao.updateItem(item.copy(done = !item.done))
            dao.touch(item.listId, System.currentTimeMillis())
        }
    }

    fun deleteItem(item: ShoppingItem) = deleteItems(listOf(item))

    fun deleteItems(items: Collection<ShoppingItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            dao.deleteItemsById(items.map { it.id })
            val now = System.currentTimeMillis()
            items.map { it.listId }.distinct().forEach { dao.touch(it, now) }
        }
    }

    fun clearDone(listId: Long) {
        viewModelScope.launch {
            dao.clearDone(listId)
            dao.touch(listId, System.currentTimeMillis())
        }
    }
}

private data class Measure(val name: String, val count: Int, val unit: String)

private data class Priced(val name: String, val kurus: Int)

private val moneyWord = """(?:₺|tl|ytl|lira|try)"""

private fun parsePrice(raw: String): Priced? {
    val text = raw.trim()
    val number = """(\d+(?:[.,]\d{1,2})?)"""
    val tail = Regex(
        """^(.+?)\s+$number\s*$moneyWord\.?$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(text)
    if (tail != null) {
        val name = tail.groupValues[1].trim()
        val kurus = parseKurus(tail.groupValues[2])
        if (name.isNotEmpty() && kurus != null && kurus > 0) return Priced(name, kurus)
    }
    val lead = Regex(
        """^$number\s*$moneyWord\.?\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(text)
    if (lead != null) {
        val name = lead.groupValues[2].trim()
        val kurus = parseKurus(lead.groupValues[1])
        if (name.isNotEmpty() && kurus != null && kurus > 0) return Priced(name, kurus)
    }
    return null
}

private val unitWord = """(?:kilogram|kilolar|kiloları|kilo|kg|gram|gr|mililitre|milim|ml|litre|lt|adet|tane|ad|g|l)"""

private fun shopName(raw: String): String {
    val text = raw.trim()
    val note = measureNote(text) ?: return text
    val name = parseMeasure(text).name
    if (name.isEmpty()) return text
    return "$name ($note)"
}

private fun measureNote(text: String): String? {
    val number = """(\d+(?:[.,]\d+)?)"""
    val lead = Regex(
        """^($number\s*$unitWord\.?)\s+.+$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(text)
    if (lead != null) return lead.groupValues[1].replace(Regex("\\s+"), " ").trim().trimEnd('.')
    val tail = Regex(
        """^.+?\s+($number\s*$unitWord\.?)$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(text)
    if (tail != null) return tail.groupValues[1].replace(Regex("\\s+"), " ").trim().trimEnd('.')
    val times = Regex("""^.+?\s*([x×]\s*\d+)$""", RegexOption.IGNORE_CASE).matchEntire(text)
    if (times != null) return times.groupValues[1].replace("×", "x").replace(" ", "")
    val plain = Regex("""^($number)\s+.+$""").matchEntire(text)
    if (plain != null) return plain.groupValues[1]
    return null
}

private fun parseMeasure(raw: String): Measure {
    val text = raw.trim()
    val number = """(\d+(?:[.,]\d+)?)"""
    val lead = Regex(
        """^$number\s*($unitWord)\.?\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(text)
    if (lead != null) {
        val name = lead.groupValues[3].trim()
        if (name.isNotEmpty()) return measure(name, lead.groupValues[1], lead.groupValues[2])
    }
    val tail = Regex(
        """^(.+?)\s+$number\s*($unitWord)\.?$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(text)
    if (tail != null) {
        val name = tail.groupValues[1].trim()
        if (name.isNotEmpty()) return measure(name, tail.groupValues[2], tail.groupValues[3])
    }
    val times = Regex("""^(.+?)\s*[x×]\s*(\d+)$""", RegexOption.IGNORE_CASE).matchEntire(text)
    if (times != null) {
        val name = times.groupValues[1].trim()
        val count = times.groupValues[2].toIntOrNull()?.coerceIn(1, 99) ?: 1
        if (name.isNotEmpty()) return Measure(name, count, "")
    }
    val plain = Regex("""^$number\s+(.+)$""").matchEntire(text)
    if (plain != null) {
        val name = plain.groupValues[2].trim()
        val count = plain.groupValues[1].replace(',', '.').toDoubleOrNull()?.toInt()?.coerceIn(1, 99) ?: 1
        if (name.isNotEmpty()) return Measure(name, count, "")
    }
    return Measure(text, 1, "")
}

private fun measure(name: String, number: String, rawUnit: String): Measure {
    val value = number.replace(',', '.').toDoubleOrNull() ?: 1.0
    val unit = when (rawUnit.lowercase().trimEnd('.')) {
        "kg", "kilo", "kilogram", "kilolar", "kiloları" -> "kg"
        "g", "gr", "gram" -> "g"
        "ml", "mililitre", "milim" -> "ml"
        "l", "lt", "litre" -> "L"
        else -> ""
    }
    val count = when (unit) {
        "kg", "L" -> (value * 10).toInt().coerceIn(1, 200)
        "g", "ml" -> value.toInt().coerceIn(1, 9_999)
        else -> value.toInt().coerceIn(1, 99)
    }
    return Measure(name, count, unit)
}
