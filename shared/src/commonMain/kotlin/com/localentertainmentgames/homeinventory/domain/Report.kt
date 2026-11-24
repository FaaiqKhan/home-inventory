package com.localentertainmentgames.homeinventory.domain

import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.Item
import kotlinx.datetime.LocalDate

data class ReportRow(
    val itemId: String,
    val name: String,
    val serial: String,
    val bought: String,
    val valueCents: Long,
    val quantity: Int,
    val category: String,
    val location: String,
    val photoPath: String?,
)

data class InsuranceReport(
    val date: LocalDate,
    val totalValueCents: Long,
    val itemCount: Int,
    val photoCount: Int,
    val missingPhotoCount: Int,
    val rows: List<ReportRow>,
) {
    val dateLabel: String get() = Format.date(date)

    companion object {
        const val PREVIEW_ROWS = 6

        fun build(items: List<Item>, today: LocalDate) = InsuranceReport(
            date = today,
            totalValueCents = items.sumOf { it.totalValueCents },
            itemCount = items.size,
            photoCount = items.count { it.hasPhoto },
            missingPhotoCount = items.count { !it.hasPhoto },
            rows = items
                .sortedWith(compareByDescending<Item> { it.totalValueCents }.thenBy { it.name })
                .map {
                    ReportRow(
                        itemId = it.id,
                        name = it.name,
                        serial = it.serial.ifBlank { "No serial" },
                        bought = it.purchaseDate?.let(Format::monthYear) ?: "—",
                        valueCents = it.totalValueCents,
                        quantity = it.quantity,
                        category = it.category,
                        location = Format.location(it.location),
                        photoPath = it.photos.firstOrNull(),
                    )
                },
        )
    }
}
