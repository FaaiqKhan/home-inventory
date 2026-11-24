package com.localentertainmentgames.homeinventory.domain

import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import kotlinx.datetime.LocalDate

/** Editable form state for adding or editing an item. */
data class ItemDraft(
    val name: String = "",
    val location: List<String> = listOf(Item.HOME),
    val category: String = "",
    val quantity: Int = 1,
    val priceText: String = "",
    val purchaseDate: LocalDate? = null,
    val condition: String = "",
    val serial: String = "",
    val barcode: String = "",
    val expiresOn: LocalDate? = null,
    val tags: Set<ItemTag> = emptySet(),
    val photos: List<String> = emptyList(),
    val notes: String = "",
) {
    val nameError: String? get() = if (name.isBlank()) "Give the item a name" else null
    val priceError: String? get() = if (priceText.isNotBlank() && Format.parseMoney(priceText) == null) "Enter an amount like 12.50" else null
    val isValid: Boolean get() = nameError == null && priceError == null

    fun increment() = copy(quantity = quantity + 1)
    fun decrement() = copy(quantity = maxOf(1, quantity - 1))
    fun toggle(tag: ItemTag) = copy(tags = if (tag in tags) tags - tag else tags + tag)

    fun toItem(id: String = "", createdAtMillis: Long = 0L) = Item(
        id = id,
        name = name.trim(),
        location = location.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf(Item.HOME) },
        category = category,
        quantity = quantity,
        priceCents = Format.parseMoney(priceText),
        purchaseDate = purchaseDate,
        condition = condition,
        serial = serial.trim(),
        barcode = barcode.trim(),
        expiresOn = expiresOn,
        tags = tags,
        photos = photos,
        notes = notes.trim(),
        createdAtMillis = createdAtMillis,
    )

    companion object {
        fun from(item: Item) = ItemDraft(
            name = item.name,
            location = item.location,
            category = item.category,
            quantity = item.quantity,
            priceText = Format.moneyInput(item.priceCents),
            purchaseDate = item.purchaseDate,
            condition = item.condition,
            serial = item.serial,
            barcode = item.barcode,
            expiresOn = item.expiresOn,
            tags = item.tags,
            photos = item.photos,
            notes = item.notes,
        )

        /** Builds a location path from the picker's room and spot fields. */
        fun locationOf(room: String, spot: String): List<String> =
            listOf(Item.HOME) + listOf(room.trim(), spot.trim()).filter { it.isNotEmpty() }
    }
}
