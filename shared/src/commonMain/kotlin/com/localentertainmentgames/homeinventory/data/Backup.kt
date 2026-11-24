package com.localentertainmentgames.homeinventory.data

import com.localentertainmentgames.homeinventory.model.InventoryData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlin.io.encoding.Base64

/**
 * The exported backup file. It carries the photos too (base64 JPEG keyed by the path the item
 * stored), so a restore on a new phone gets them back.
 */
@Serializable
data class BackupFile(
    val format: String = Backup.FORMAT,
    val version: Int = Backup.VERSION,
    val exportedAtMillis: Long = 0L,
    val data: InventoryData = InventoryData(),
    val photos: Map<String, String> = emptyMap(),
) {
    val itemCount: Int get() = data.items.size
    val photoCount: Int get() = photos.size
}

class BackupFormatException(message: String) : Exception(message)

object Backup {
    const val FORMAT = "home-inventory-backup"
    const val VERSION = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Serialises [data] with every photo [readPhoto] can read. Unreadable photos are left out. */
    fun create(data: InventoryData, exportedAtMillis: Long, readPhoto: (String) -> ByteArray?): String {
        val photos = data.items.asSequence()
            .flatMap { it.photos }
            .distinct()
            .mapNotNull { path -> runCatching { readPhoto(path) }.getOrNull()?.let { path to Base64.encode(it) } }
            .toMap()
        val file = BackupFile(exportedAtMillis = exportedAtMillis, data = data, photos = photos)
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /**
     * Reads a backup. Also accepts the first export format (the bare database document).
     * @throws BackupFormatException when the text is not a backup this version can read.
     */
    fun parse(text: String): BackupFile {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: throw BackupFormatException(NOT_A_BACKUP)
        val format = (root["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        return when {
            format == FORMAT -> {
                val version = (root["version"] as? JsonPrimitive)?.intOrNull ?: 0
                if (version > VERSION) throw BackupFormatException("This backup was made by a newer version of the app. Update the app, then try again.")
                runCatching { json.decodeFromJsonElement<BackupFile>(root) }.getOrElse { throw BackupFormatException(DAMAGED) }
            }
            format == null && "items" in root && "goBag" in root -> {
                val data = runCatching { json.decodeFromJsonElement<InventoryData>(root) }.getOrElse { throw BackupFormatException(DAMAGED) }
                BackupFile(exportedAtMillis = data.settings.lastBackupAtMillis ?: 0L, data = data)
            }
            else -> throw BackupFormatException(NOT_A_BACKUP)
        }
    }

    /**
     * Writes the backup's photos into [photoDir] under fresh names and returns the data to restore,
     * with photo paths pointing at the new files. A photo that is neither in the backup nor on this
     * phone is dropped from its item. "Last backup" becomes the time the backup was made.
     */
    fun restoredData(
        backup: BackupFile,
        photoDir: String,
        newName: () -> String,
        writeBytes: (String, ByteArray) -> Unit = Files::writeBytes,
        exists: (String) -> Boolean = Files::exists,
    ): InventoryData {
        val written = HashMap<String, String?>()
        fun relocate(path: String): String? = written.getOrPut(path) {
            val encoded = backup.photos[path]
            val bytes = encoded?.let { runCatching { Base64.decode(it) }.getOrNull() }
            when {
                bytes != null -> runCatching {
                    val target = "$photoDir/${newName()}.jpg"
                    writeBytes(target, bytes)
                    target
                }.getOrNull()
                encoded == null && exists(path) -> path
                else -> null
            }
        }
        val items = backup.data.items.map { item -> item.copy(photos = item.photos.mapNotNull(::relocate)) }
        val lastBackup = backup.exportedAtMillis.takeIf { it > 0 } ?: backup.data.settings.lastBackupAtMillis
        return backup.data.copy(items = items, settings = backup.data.settings.copy(lastBackupAtMillis = lastBackup))
    }

    const val NOT_A_BACKUP = "That file isn't a Home Inventory backup."
    const val DAMAGED = "That backup file is damaged and can't be restored."
}
