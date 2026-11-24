package com.localentertainmentgames.homeinventory.data

import com.localentertainmentgames.homeinventory.model.AppSettings
import com.localentertainmentgames.homeinventory.model.Contact
import com.localentertainmentgames.homeinventory.model.DashboardLayout
import com.localentertainmentgames.homeinventory.model.GoBagEntry
import com.localentertainmentgames.homeinventory.model.InventoryData
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Single source of truth. Every change updates [data] at once and is written to [storage]
 * in the background, in order. Everything stays on the device.
 */
class InventoryRepository(
    private val storage: DataStorage,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val newId: () -> String = { Uuid.random().toString() },
    private val deleteFile: (String) -> Unit = { path -> runCatching { Files.delete(path) } },
) {
    private val _data = MutableStateFlow(InventoryData())
    val data: StateFlow<InventoryData> = _data.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val writeLock = Mutex()
    private var lastWrite: Job? = null

    suspend fun load() {
        val text = runCatching { storage.read() }.getOrNull()
        if (text != null) {
            runCatching { BackupCodec.decode(text) }.onSuccess { _data.value = it }
        }
        _loaded.value = true
    }

    /** Waits for pending writes; used by tests and before exporting. */
    suspend fun flush() {
        lastWrite?.join()
    }

    fun update(transform: (InventoryData) -> InventoryData) {
        _data.update(transform)
        val snapshot = _data.value
        lastWrite = scope.launch {
            writeLock.withLock { storage.write(BackupCodec.encode(snapshot)) }
        }
    }

    fun now(): Long = clock.now().toEpochMilliseconds()

    fun item(id: String): Item? = _data.value.items.firstOrNull { it.id == id }

    // Items

    fun addItem(item: Item): Item {
        val saved = item.copy(
            id = item.id.ifBlank { newId() },
            name = item.name.trim(),
            createdAtMillis = if (item.createdAtMillis == 0L) now() else item.createdAtMillis,
        )
        update { it.copy(items = it.items + saved) }
        return saved
    }

    fun updateItem(item: Item) = update { d -> d.copy(items = d.items.map { if (it.id == item.id) item.copy(name = item.name.trim()) else it }) }

    /** Removes the item and its photo files. */
    fun deleteItem(id: String) {
        val photos = item(id)?.photos.orEmpty()
        update { d -> d.copy(items = d.items.filterNot { it.id == id }) }
        photos.forEach(deleteFile)
    }

    fun toggleTag(id: String, tag: ItemTag) = editItem(id) { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) }

    fun addPhoto(id: String, path: String) = editItem(id) { it.copy(photos = it.photos + path) }

    fun removePhoto(id: String, path: String) {
        editItem(id) { it.copy(photos = it.photos - path) }
        deleteFile(path)
    }

    private fun editItem(id: String, f: (Item) -> Item) =
        update { d -> d.copy(items = d.items.map { if (it.id == id) f(it) else it }) }

    // Go-bag and evacuation

    fun toggleGoBag(entryId: String) = update { d ->
        d.copy(goBag = d.goBag.map { if (it.id == entryId) it.copy(packed = !it.packed) else it })
    }

    fun addGoBagEntry(name: String) {
        if (name.isBlank()) return
        update { d -> d.copy(goBag = d.goBag + GoBagEntry(id = newId(), name = name.trim())) }
    }

    fun removeGoBagEntry(entryId: String) = update { d -> d.copy(goBag = d.goBag.filterNot { it.id == entryId }) }

    /** "I'm Evacuating" logs the time; tapping again undoes it. */
    fun toggleEvacuating() = update { d ->
        d.copy(evacuatingSinceMillis = if (d.evacuatingSinceMillis == null) now() else null)
    }

    fun setPlan(meetingPoint: String, contacts: List<Contact>) = update { d ->
        d.copy(plan = d.plan.copy(meetingPoint = meetingPoint.trim(), contacts = contacts.filter { it.name.isNotBlank() }))
    }

    // Settings

    fun setLayout(layout: DashboardLayout) = updateSettings { it.copy(dashboardLayout = layout) }

    fun setHideValues(hide: Boolean) = updateSettings { it.copy(hideValues = hide) }

    fun markBackedUp() = updateSettings { it.copy(lastBackupAtMillis = now()) }

    private fun updateSettings(f: (AppSettings) -> AppSettings) = update { it.copy(settings = f(it.settings)) }

    // Bulk

    fun replaceAll(newData: InventoryData) = update { newData }

    /** Replaces everything with restored [newData] and deletes photo files nothing points to any more. */
    fun restore(newData: InventoryData) {
        val before = _data.value.items.flatMapTo(HashSet()) { it.photos }
        val after = newData.items.flatMapTo(HashSet()) { it.photos }
        update { newData }
        (before - after).forEach(deleteFile)
    }

    fun loadSample() = update { SampleData.build(now(), clock).copy(settings = it.settings) }

    fun eraseAll() = update { InventoryData(settings = it.settings) }
}

object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(data: InventoryData): String = json.encodeToString(InventoryData.serializer(), data)

    fun decode(text: String): InventoryData = json.decodeFromString(InventoryData.serializer(), text)
}
