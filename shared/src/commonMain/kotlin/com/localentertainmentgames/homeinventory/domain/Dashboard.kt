package com.localentertainmentgames.homeinventory.domain

import com.localentertainmentgames.homeinventory.model.InventoryData
import kotlinx.datetime.LocalDate

data class DashboardSummary(
    val totalValueCents: Long,
    val itemCount: Int,
    val withPhotos: Int,
    val packed: Int,
    val bagSize: Int,
    val missing: List<String>,
    val expiring: List<ExpiringEntry>,
) {
    val missingCount: Int get() = bagSize - packed
    val packedFraction: Float get() = if (bagSize == 0) 0f else packed.toFloat() / bagSize
    val expiringGroups: List<ExpiringGroup> get() = Expiry.groups(expiring)

    companion object {
        fun from(data: InventoryData, today: LocalDate) = DashboardSummary(
            totalValueCents = data.items.sumOf { it.totalValueCents },
            itemCount = data.items.size,
            withPhotos = data.items.count { it.hasPhoto },
            packed = data.goBag.count { it.packed },
            bagSize = data.goBag.size,
            missing = data.goBag.filterNot { it.packed }.map { it.name },
            expiring = Expiry.upcoming(data.items, today),
        )
    }
}
