package com.localentertainmentgames.homeinventory.data

import com.localentertainmentgames.homeinventory.model.Contact
import com.localentertainmentgames.homeinventory.model.DefaultGoBag
import com.localentertainmentgames.homeinventory.model.EvacuationPlan
import com.localentertainmentgames.homeinventory.model.InventoryData
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** The demo household from the design, so people can try every screen. */
object SampleData {
    fun build(nowMillis: Long, clock: Clock = Clock.System, tz: TimeZone = TimeZone.currentSystemDefault()): InventoryData {
        val today = clock.todayIn(tz)
        fun inDays(n: Int) = today.plus(DatePeriod(days = n))
        var n = 0
        fun item(
            name: String,
            path: List<String>,
            category: String,
            qty: Int = 1,
            price: Long? = null,
            bought: LocalDate? = null,
            serial: String = "",
            tags: Set<ItemTag> = emptySet(),
            expires: LocalDate? = null,
            condition: String = "Good",
        ) = Item(
            id = "sample-${n++}",
            name = name,
            location = listOf(Item.HOME) + path,
            category = category,
            quantity = qty,
            priceCents = price?.times(100),
            purchaseDate = bought,
            serial = serial,
            tags = tags,
            expiresOn = expires,
            condition = condition,
            createdAtMillis = nowMillis,
        )
        val items = listOf(
            item("Passport · Maya", listOf("Bedroom", "Fireproof safe"), "Documents", serial = "P4821", tags = setOf(ItemTag.GoBag, ItemTag.Irreplaceable)),
            item("MacBook Pro 14″", listOf("Office", "Desk"), "Electronics", price = 1999, bought = LocalDate(2024, 3, 12), serial = "C02XK3Y9MD6T", tags = setOf(ItemTag.Insured)),
            item("First-aid kit", listOf("Hall closet", "Go-bag"), "Emergency", price = 35, tags = setOf(ItemTag.GoBag)),
            item("Family photo albums", listOf("Living room", "Bookshelf"), "Collectibles", qty = 3, tags = setOf(ItemTag.Irreplaceable)),
            item("Canon EOS R6", listOf("Office", "Camera case"), "Electronics", price = 2499, bought = LocalDate(2023, 8, 2), serial = "0820517", tags = setOf(ItemTag.Insured, ItemTag.GoBag)),
            item("AA batteries", listOf("Kitchen", "Junk drawer"), "Batteries", qty = 24, price = 1, expires = inDays(19)),
            item("Emergency radio", listOf("Garage", "Go-bag"), "Emergency", price = 59, bought = LocalDate(2022, 9, 4), serial = "ER-77120", tags = setOf(ItemTag.GoBag)),
            item("Cordless drill", listOf("Garage", "Toolbox"), "Tools", price = 189, bought = LocalDate(2021, 5, 20), serial = "DW-55012", tags = setOf(ItemTag.Insured)),
            item("Samsung TV 65″", listOf("Living room", "Wall"), "Electronics", price = 1199, bought = LocalDate(2022, 11, 25), serial = "0AKT3HCN70", tags = setOf(ItemTag.Insured)),
            item("Espresso machine", listOf("Kitchen", "Counter"), "Appliances", price = 649, bought = LocalDate(2024, 1, 8)),
            item("Prescription meds", listOf("Bathroom", "Bathroom cabinet"), "Meds", expires = inDays(6)),
            item("Canned soup", listOf("Kitchen", "Pantry"), "Food", qty = 6, price = 3, expires = inDays(12)),
            item("Granola bars", listOf("Kitchen", "Pantry"), "Food", qty = 12, price = 1, expires = inDays(24)),
        )
        val packed = setOf(0, 1, 3, 4, 5, 7, 8, 10, 11)
        return InventoryData(
            items = items,
            goBag = DefaultGoBag.entries().mapIndexed { i, e -> e.copy(packed = i in packed) },
            plan = EvacuationPlan(
                meetingPoint = "Oak Street Library, north car park",
                contacts = listOf(Contact("Sam", "Partner", "+15555550101"), Contact("Rosa", "Aunt · 14 Pine Ct", "+15555550102")),
            ),
        )
    }
}
