package com.localentertainmentgames.homeinventory.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object Format {
    const val HIDDEN_MONEY = "$•••••"

    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun groupThousands(value: Long): String {
        val digits = kotlin.math.abs(value).toString()
        val sb = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
            sb.append(c)
        }
        return (if (value < 0) "-" else "") + sb
    }

    /** "$48,720" or "$12.50"; masked when [hide] is on. */
    fun money(cents: Long, hide: Boolean = false): String {
        if (hide) return HIDDEN_MONEY
        val dollars = cents / 100
        val rest = kotlin.math.abs(cents % 100)
        val whole = "$" + groupThousands(dollars)
        return if (rest == 0L) whole else whole + "." + rest.toString().padStart(2, '0')
    }

    /** Parses "$1,999.50", "1999.5", "12" into cents. Returns null for blank or invalid input. */
    fun parseMoney(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        if (!Regex("""\d+(\.\d{0,2})?""").matches(cleaned)) return null
        val parts = cleaned.split('.')
        val dollars = parts[0].toLongOrNull() ?: return null
        val cents = parts.getOrNull(1)?.padEnd(2, '0')?.toLongOrNull() ?: 0L
        return dollars * 100 + cents
    }

    /** Money as editable text, without the "$" or grouping. */
    fun moneyInput(cents: Long?): String {
        if (cents == null) return ""
        val rest = cents % 100
        return if (rest == 0L) (cents / 100).toString() else "${cents / 100}.${rest.toString().padStart(2, '0')}"
    }

    fun shortMonth(month: Int): String = months[month - 1]

    /** "Mar 12, 2024" */
    fun date(d: LocalDate): String = "${shortMonth(d.month.ordinal + 1)} ${d.day}, ${d.year}"

    /** "Mar 2024" */
    fun monthYear(d: LocalDate): String = "${shortMonth(d.month.ordinal + 1)} ${d.year}"

    /** "Nov 24, 2026" for an instant. */
    fun date(millis: Long, tz: TimeZone): String = date(Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date)

    /** "14:02:11" */
    fun time(millis: Long, tz: TimeZone = TimeZone.currentSystemDefault()): String {
        val t: LocalDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
        fun two(n: Int) = n.toString().padStart(2, '0')
        return "${two(t.hour)}:${two(t.minute)}:${two(t.second)}"
    }

    fun daysLabel(days: Int): String = when {
        days < 0 -> "Expired"
        days == 0 -> "Today"
        days == 1 -> "1 day"
        else -> "$days days"
    }

    fun lastBackup(lastMillis: Long?, nowMillis: Long): String {
        if (lastMillis == null) return "Never backed up"
        val days = ((nowMillis - lastMillis) / 86_400_000L).toInt()
        return when {
            days <= 0 -> "Last backup today"
            days == 1 -> "Last backup yesterday"
            else -> "Last backup $days days ago"
        }
    }

    fun location(path: List<String>): String = path.joinToString(" → ")

    fun plural(count: Int, one: String, many: String = one + "s") = if (count == 1) one else many
}
