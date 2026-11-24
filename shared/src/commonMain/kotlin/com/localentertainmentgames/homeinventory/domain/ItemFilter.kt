package com.localentertainmentgames.homeinventory.domain

import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import kotlinx.datetime.LocalDate

data class ItemFilter(
    val query: String = "",
    val tags: Set<ItemTag> = emptySet(),
    val room: String? = null,
    val category: String? = null,
    val expiringOnly: Boolean = false,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || tags.isNotEmpty() || room != null || category != null || expiringOnly

    fun toggle(tag: ItemTag) = copy(tags = if (tag in tags) tags - tag else tags + tag)
}

/** Searches name, serial, barcode, location, category and tags; all chosen tags must match. */
fun List<Item>.filterBy(filter: ItemFilter, today: LocalDate): List<Item> {
    val q = filter.query.trim().lowercase()
    return filter { item ->
        filter.tags.all { it in item.tags } &&
            (filter.room == null || item.room == filter.room) &&
            (filter.category == null || item.category == filter.category) &&
            (!filter.expiringOnly || Expiry.isExpiringSoon(item, today)) &&
            (q.isEmpty() || item.searchText().contains(q))
    }
}

private fun Item.searchText(): String =
    (listOf(name, serial, barcode, category, condition, notes) + location + tags.map { it.label })
        .joinToString(" ")
        .lowercase()

fun List<Item>.rooms(): List<String> = mapNotNull { it.room }.distinct().sorted()

fun List<Item>.categories(): List<String> = map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
