package com.localentertainmentgames.homeinventory.domain

import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.Item
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

data class ExpiringEntry(val item: Item, val daysLeft: Int) {
    val label: String get() = Format.daysLabel(daysLeft)
    val isUrgent: Boolean get() = daysLeft <= Expiry.URGENT_DAYS
    val where: String get() = item.location.lastOrNull() ?: ""
}

data class ExpiringGroup(val category: String, val count: Int, val minDays: Int) {
    val isUrgent: Boolean get() = minDays <= Expiry.URGENT_DAYS

    /** "Meds · 6 days" for one item, "Food · 2" for several. */
    val label: String get() = if (count == 1) "$category · ${Format.daysLabel(minDays)}" else "$category · $count"
}

object Expiry {
    const val WINDOW_DAYS = 30
    const val URGENT_DAYS = 7

    fun daysLeft(item: Item, today: LocalDate): Int? = item.expiresOn?.let { today.daysUntil(it) }

    fun isExpiringSoon(item: Item, today: LocalDate): Boolean {
        val d = daysLeft(item, today) ?: return false
        return d <= WINDOW_DAYS
    }

    fun upcoming(items: List<Item>, today: LocalDate): List<ExpiringEntry> = items
        .mapNotNull { item -> daysLeft(item, today)?.takeIf { it <= WINDOW_DAYS }?.let { ExpiringEntry(item, it) } }
        .sortedWith(compareBy<ExpiringEntry> { it.daysLeft }.thenBy { it.item.name })

    fun groups(entries: List<ExpiringEntry>): List<ExpiringGroup> = entries
        .groupBy { it.item.category.ifBlank { "Other" } }
        .map { (cat, list) -> ExpiringGroup(cat, list.size, list.minOf { it.daysLeft }) }
        .sortedBy { it.minDays }

    /** "+ 2 food, 1 batteries in the next 30 days" for everything after the first entry. */
    fun restSummary(entries: List<ExpiringEntry>): String? {
        val rest = entries.drop(1)
        if (rest.isEmpty()) return null
        val parts = groups(rest).joinToString(", ") { "${it.count} ${it.category.lowercase()}" }
        return "+ $parts in the next $WINDOW_DAYS days"
    }
}
