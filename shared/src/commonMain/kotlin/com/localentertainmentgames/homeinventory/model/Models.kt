package com.localentertainmentgames.homeinventory.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** Tags shown as filter chips and statuses shown in the Mark Status sheet share one set. */
@Serializable
enum class ItemTag(val label: String, val hint: String) {
    GoBag("go-bag", "Packed to leave"),
    Irreplaceable("irreplaceable", "Can't be replaced"),
    Insured("insured", "Covered by policy"),
    Lost("lost", "Cannot be found"),
    Claimed("claimed", "Claim filed");

    companion object {
        /** Chips on the item list. */
        val filterTags = listOf(GoBag, Irreplaceable, Insured)

        /** Rows in the Mark Status sheet, in design order. */
        val statusTags = listOf(Insured, Lost, Claimed, GoBag)
    }
}

@Serializable
data class Item(
    val id: String,
    val name: String,
    /** Location path, e.g. ["Home", "Garage", "Shelf B"]. Index 1 is the room. */
    val location: List<String> = listOf(HOME),
    val category: String = "",
    val quantity: Int = 1,
    /** Unit price in cents. */
    val priceCents: Long? = null,
    val purchaseDate: LocalDate? = null,
    val condition: String = "",
    val serial: String = "",
    val barcode: String = "",
    val expiresOn: LocalDate? = null,
    val tags: Set<ItemTag> = emptySet(),
    /** Absolute file paths of photos stored on the device. */
    val photos: List<String> = emptyList(),
    val notes: String = "",
    val createdAtMillis: Long = 0,
) {
    val room: String? get() = location.getOrNull(1)
    val totalValueCents: Long get() = (priceCents ?: 0L) * quantity
    val hasPhoto: Boolean get() = photos.isNotEmpty()

    companion object {
        const val HOME = "Home"
    }
}

@Serializable
data class GoBagEntry(val id: String, val name: String, val packed: Boolean = false)

@Serializable
data class Contact(val name: String, val role: String = "", val phone: String = "")

@Serializable
data class EvacuationPlan(
    val meetingPoint: String = "",
    val contacts: List<Contact> = emptyList(),
)

@Serializable
enum class DashboardLayout(val label: String, val description: String) {
    ValueFirst("Value first", "Total value up top"),
    ReadinessFirst("Readiness first", "Go-bag status up top"),
    ActionFirst("Action first", "Big shortcuts up top"),
}

@Serializable
data class AppSettings(
    val dashboardLayout: DashboardLayout = DashboardLayout.ValueFirst,
    val hideValues: Boolean = false,
    val lastBackupAtMillis: Long? = null,
)

@Serializable
data class InventoryData(
    val version: Int = 1,
    val items: List<Item> = emptyList(),
    val goBag: List<GoBagEntry> = DefaultGoBag.entries(),
    val plan: EvacuationPlan = EvacuationPlan(),
    val evacuatingSinceMillis: Long? = null,
    val settings: AppSettings = AppSettings(),
)

object DefaultGoBag {
    val names = listOf(
        "Passport & IDs",
        "Cash envelope",
        "Medications (7 days)",
        "First-aid kit",
        "Water, 3 litres",
        "Phone charger + power bank",
        "Flashlight",
        "Emergency radio",
        "Documents USB drive",
        "Spare glasses",
        "Pet food & leash",
        "Change of clothes",
    )

    fun entries(): List<GoBagEntry> = names.mapIndexed { i, n -> GoBagEntry(id = "bag-$i", name = n) }
}

object Categories {
    val all = listOf(
        "Electronics",
        "Appliances",
        "Furniture",
        "Jewelry",
        "Documents",
        "Tools",
        "Clothing",
        "Kitchen",
        "Sports",
        "Collectibles",
        "Meds",
        "Food",
        "Batteries",
        "Emergency",
        "Other",
    )
}

object Conditions {
    val all = listOf("New", "Excellent", "Good", "Fair", "Poor")
}
