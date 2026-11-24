package com.localentertainmentgames.homeinventory

import com.localentertainmentgames.homeinventory.domain.DashboardSummary
import com.localentertainmentgames.homeinventory.domain.Expiry
import com.localentertainmentgames.homeinventory.domain.InsuranceReport
import com.localentertainmentgames.homeinventory.domain.ItemFilter
import com.localentertainmentgames.homeinventory.domain.categories
import com.localentertainmentgames.homeinventory.domain.filterBy
import com.localentertainmentgames.homeinventory.domain.rooms
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainTest {
    private val today = LocalDate(2026, 11, 24)
    private val data = sampleData()
    private val items = data.items

    private fun names(f: ItemFilter) = items.filterBy(f, today).map { it.name }.toSet()

    @Test
    fun searchMatchesNameSerialRoomAndTag() {
        assertEquals(setOf("MacBook Pro 14″"), names(ItemFilter(query = "macbook")))
        assertEquals(setOf("Cordless drill"), names(ItemFilter(query = "DW-55012")))
        assertTrue("Cordless drill" in names(ItemFilter(query = "garage")))
        assertTrue(names(ItemFilter(query = "irreplaceable")).containsAll(setOf("Passport · Maya", "Family photo albums")))
        assertTrue(names(ItemFilter(query = "zzz-nothing")).isEmpty())
    }

    @Test
    fun tagsMustAllMatch() {
        assertEquals(setOf("Canon EOS R6"), names(ItemFilter(tags = setOf(ItemTag.Insured, ItemTag.GoBag))))
        val goBag = names(ItemFilter(tags = setOf(ItemTag.GoBag)))
        assertEquals(setOf("Passport · Maya", "First-aid kit", "Canon EOS R6", "Emergency radio"), goBag)
    }

    @Test
    fun filtersByRoomCategoryAndExpiry() {
        assertEquals(setOf("Emergency radio", "Cordless drill"), names(ItemFilter(room = "Garage")))
        assertEquals(setOf("Canned soup", "Granola bars"), names(ItemFilter(category = "Food")))
        assertEquals(setOf("AA batteries", "Prescription meds", "Canned soup", "Granola bars"), names(ItemFilter(expiringOnly = true)))
    }

    @Test
    fun filterToggleAndActive() {
        val f = ItemFilter()
        assertFalse(f.isActive)
        val on = f.toggle(ItemTag.Insured)
        assertTrue(on.isActive)
        assertEquals(emptySet(), on.toggle(ItemTag.Insured).tags)
    }

    @Test
    fun listsRoomsAndCategories() {
        assertTrue(items.rooms().containsAll(listOf("Garage", "Kitchen", "Office")))
        assertEquals(items.rooms().sorted(), items.rooms())
        assertTrue("" !in items.categories())
        assertTrue("Electronics" in items.categories())
    }

    @Test
    fun expiryOrdersAndGroups() {
        val up = Expiry.upcoming(items, today)
        assertEquals(listOf("Prescription meds", "Canned soup", "AA batteries", "Granola bars"), up.map { it.item.name })
        assertEquals(6, up.first().daysLeft)
        assertTrue(up.first().isUrgent)
        assertEquals("Bathroom cabinet", up.first().where)
        val groups = Expiry.groups(up)
        assertEquals(listOf("Meds · 6 days", "Food · 2", "Batteries · 19 days"), groups.map { it.label })
        assertEquals("+ 2 food, 1 batteries in the next 30 days", Expiry.restSummary(up))
        assertNull(Expiry.restSummary(up.take(1)))
    }

    @Test
    fun expiredAndFarItems() {
        val expired = Item("x", "Old milk", expiresOn = LocalDate(2026, 11, 20), category = "Food")
        val far = Item("y", "Rice", expiresOn = LocalDate(2027, 6, 1))
        val up = Expiry.upcoming(listOf(expired, far), today)
        assertEquals(listOf("Old milk"), up.map { it.item.name })
        assertEquals("Expired", up.single().label)
    }

    @Test
    fun dashboardSummary() {
        val s = DashboardSummary.from(data, today)
        assertEquals(items.size, s.itemCount)
        assertEquals(items.sumOf { it.totalValueCents }, s.totalValueCents)
        assertEquals(9, s.packed)
        assertEquals(12, s.bagSize)
        assertEquals(3, s.missingCount)
        assertEquals(listOf("Medications (7 days)", "Flashlight", "Spare glasses"), s.missing)
        assertEquals(0.75f, s.packedFraction)
        assertEquals(0, s.withPhotos)
    }

    @Test
    fun reportSortsByValueAndCountsPhotos() {
        val withPhoto = items.map { if (it.name == "Cordless drill") it.copy(photos = listOf("/a.jpg", "/b.jpg")) else it }
        val r = InsuranceReport.build(withPhoto, today)
        assertEquals("Canon EOS R6", r.rows.first().name)
        assertEquals(r.rows.sortedByDescending { it.valueCents }.map { it.valueCents }, r.rows.map { it.valueCents })
        assertEquals(1, r.photoCount)
        assertEquals(items.size - 1, r.missingPhotoCount)
        assertEquals("Nov 24, 2026", r.dateLabel)
        val passport = r.rows.first { it.name.startsWith("Passport") }
        assertEquals("—", passport.bought)
        val drill = r.rows.first { it.name == "Cordless drill" }
        assertEquals("May 2021", drill.bought)
        assertEquals("/a.jpg", drill.photoPath)
        assertEquals("No serial", r.rows.first { it.name == "Espresso machine" }.serial)
    }
}
