package com.localentertainmentgames.homeinventory

import com.localentertainmentgames.homeinventory.model.Format
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatTest {
    @Test
    fun groupsThousands() {
        assertEquals("0", Format.groupThousands(0))
        assertEquals("999", Format.groupThousands(999))
        assertEquals("48,720", Format.groupThousands(48720))
        assertEquals("1,234,567", Format.groupThousands(1234567))
        assertEquals("-1,000", Format.groupThousands(-1000))
    }

    @Test
    fun formatsMoney() {
        assertEquals("$48,720", Format.money(4_872_000))
        assertEquals("$1,999.50", Format.money(199_950))
        assertEquals("$0.05", Format.money(5))
        assertEquals("$•••••", Format.money(4_872_000, hide = true))
    }

    @Test
    fun parsesMoney() {
        assertEquals(199_950L, Format.parseMoney("$1,999.50"))
        assertEquals(1200L, Format.parseMoney("12"))
        assertEquals(1250L, Format.parseMoney("12.5"))
        assertEquals(1205L, Format.parseMoney(" 12.05 "))
        assertNull(Format.parseMoney(""))
        assertNull(Format.parseMoney("abc"))
        assertNull(Format.parseMoney("1.234"))
    }

    @Test
    fun moneyInputRoundTrips() {
        assertEquals("", Format.moneyInput(null))
        assertEquals("1999", Format.moneyInput(199_900))
        assertEquals("12.05", Format.moneyInput(1205))
        assertEquals(1205L, Format.parseMoney(Format.moneyInput(1205)))
    }

    @Test
    fun formatsDates() {
        assertEquals("Mar 12, 2024", Format.date(LocalDate(2024, 3, 12)))
        assertEquals("Oct 26, 2026", Format.date(LocalDate(2026, 10, 26)))
        assertEquals("Aug 2023", Format.monthYear(LocalDate(2023, 8, 2)))
    }

    @Test
    fun formatsTime() {
        assertEquals("10:00:00", Format.time(FIXED_NOW.toEpochMilliseconds(), TimeZone.UTC))
    }

    @Test
    fun labelsDays() {
        assertEquals("Expired", Format.daysLabel(-1))
        assertEquals("Today", Format.daysLabel(0))
        assertEquals("1 day", Format.daysLabel(1))
        assertEquals("6 days", Format.daysLabel(6))
    }

    @Test
    fun describesLastBackup() {
        val now = FIXED_NOW.toEpochMilliseconds()
        val day = 86_400_000L
        assertEquals("Never backed up", Format.lastBackup(null, now))
        assertEquals("Last backup today", Format.lastBackup(now - 1000, now))
        assertEquals("Last backup yesterday", Format.lastBackup(now - day, now))
        assertEquals("Last backup 3 days ago", Format.lastBackup(now - 3 * day, now))
    }

    @Test
    fun joinsLocationAndPlurals() {
        assertEquals("Home → Garage → Shelf B", Format.location(listOf("Home", "Garage", "Shelf B")))
        assertEquals("item", Format.plural(1, "item"))
        assertEquals("items", Format.plural(2, "item"))
    }
}
