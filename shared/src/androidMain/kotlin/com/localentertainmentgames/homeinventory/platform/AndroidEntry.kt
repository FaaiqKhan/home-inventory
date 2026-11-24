package com.localentertainmentgames.homeinventory.platform

import android.content.Context
import com.localentertainmentgames.homeinventory.data.FileStorage
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** One repository per process, so rotation and activity recreation keep in-memory state and pending writes. */
object AndroidRepository {
    @Volatile
    private var instance: InventoryRepository? = null

    fun get(context: Context): InventoryRepository = instance ?: synchronized(this) {
        instance ?: InventoryRepository(
            storage = FileStorage("${context.filesDir.absolutePath}/inventory.json"),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ).also { instance = it }
    }
}
