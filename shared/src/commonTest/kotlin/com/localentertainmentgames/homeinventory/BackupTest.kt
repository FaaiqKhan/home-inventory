package com.localentertainmentgames.homeinventory

import com.localentertainmentgames.homeinventory.data.Backup
import com.localentertainmentgames.homeinventory.data.BackupCodec
import com.localentertainmentgames.homeinventory.data.BackupFile
import com.localentertainmentgames.homeinventory.data.BackupFormatException
import com.localentertainmentgames.homeinventory.data.Files
import com.localentertainmentgames.homeinventory.data.InMemoryStorage
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import com.localentertainmentgames.homeinventory.model.AppSettings
import com.localentertainmentgames.homeinventory.model.InventoryData
import com.localentertainmentgames.homeinventory.model.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupTest {
    private val photoA = byteArrayOf(-1, -40, -1, 1, 2, 3)
    private val photoB = byteArrayOf(-1, -40, -1, 9, 8)

    private fun dataWithPhotos() = InventoryData(
        items = listOf(
            Item(id = "a", name = "Camera", photos = listOf("/old/a.jpg", "/old/gone.jpg")),
            Item(id = "b", name = "Lamp", photos = listOf("/old/b.jpg")),
            Item(id = "c", name = "Chair"),
        ),
    )

    private val photoFiles = mapOf("/old/a.jpg" to photoA, "/old/b.jpg" to photoB)

    @Test
    fun createEmbedsReadablePhotosAndRoundTrips() {
        val text = Backup.create(dataWithPhotos(), 1_000L) { photoFiles[it] }
        val parsed = Backup.parse(text)
        assertEquals(Backup.FORMAT, parsed.format)
        assertEquals(Backup.VERSION, parsed.version)
        assertEquals(1_000L, parsed.exportedAtMillis)
        assertEquals(dataWithPhotos(), parsed.data)
        assertEquals(setOf("/old/a.jpg", "/old/b.jpg"), parsed.photos.keys)
        assertEquals(3, parsed.itemCount)
        assertEquals(2, parsed.photoCount)
    }

    @Test
    fun createSkipsPhotosThatThrow() {
        val text = Backup.create(dataWithPhotos(), 1L) { if (it == "/old/a.jpg") error("io") else photoFiles[it] }
        assertEquals(setOf("/old/b.jpg"), Backup.parse(text).photos.keys)
    }

    @Test
    fun parseReadsTheFirstExportFormat() {
        val legacy = sampleData().let { it.copy(settings = it.settings.copy(lastBackupAtMillis = 42L)) }
        val parsed = Backup.parse(BackupCodec.encode(legacy))
        assertEquals(legacy, parsed.data)
        assertEquals(42L, parsed.exportedAtMillis)
        assertTrue(parsed.photos.isEmpty())
    }

    @Test
    fun parseRejectsFilesThatAreNotBackups() {
        listOf("", "not json", "[1,2,3]", "{}", """{"name":"something else"}""", """{"format":"other-app","data":{}}""").forEach { text ->
            val e = assertFailsWith<BackupFormatException>(text) { Backup.parse(text) }
            assertEquals(Backup.NOT_A_BACKUP, e.message)
        }
    }

    @Test
    fun parseRejectsDamagedAndNewerBackups() {
        val damaged = assertFailsWith<BackupFormatException> {
            Backup.parse("""{"format":"home-inventory-backup","version":2,"data":{"items":"oops"}}""")
        }
        assertEquals(Backup.DAMAGED, damaged.message)
        val newer = assertFailsWith<BackupFormatException> {
            Backup.parse("""{"format":"home-inventory-backup","version":99,"data":{}}""")
        }
        assertTrue(newer.message!!.contains("newer version"))
    }

    @Test
    fun restoredDataWritesPhotosUnderNewNamesAndDropsMissingOnes() {
        val backup = Backup.parse(Backup.create(dataWithPhotos(), 5_000L) { photoFiles[it] })
        val written = mutableMapOf<String, ByteArray>()
        val ids = IdSequence()
        val restored = Backup.restoredData(backup, "/new/photos", ids::next, writeBytes = { p, b -> written[p] = b }, exists = { false })

        assertEquals(listOf("/new/photos/id-0.jpg"), restored.items[0].photos)
        assertEquals(listOf("/new/photos/id-1.jpg"), restored.items[1].photos)
        assertTrue(restored.items[2].photos.isEmpty())
        assertContentEquals(photoA, written["/new/photos/id-0.jpg"])
        assertContentEquals(photoB, written["/new/photos/id-1.jpg"])
        assertEquals(5_000L, restored.settings.lastBackupAtMillis)
    }

    @Test
    fun restoredDataKeepsLocalPhotosForOldBackupsAndWritesSharedPhotosOnce() {
        val shared = InventoryData(
            items = listOf(
                Item(id = "a", name = "A", photos = listOf("/old/a.jpg")),
                Item(id = "b", name = "B", photos = listOf("/old/a.jpg", "/here.jpg")),
            ),
        )
        val withPhoto = BackupFile(data = shared, photos = mapOf("/old/a.jpg" to "/9j/AQID"))
        var writes = 0
        val restored = Backup.restoredData(withPhoto, "/p", IdSequence()::next, writeBytes = { _, _ -> writes++ }, exists = { it == "/here.jpg" })
        assertEquals(1, writes)
        assertEquals(listOf("/p/id-0.jpg"), restored.items[0].photos)
        assertEquals(listOf("/p/id-0.jpg", "/here.jpg"), restored.items[1].photos)
    }

    @Test
    fun restoredDataDropsPhotosWithBadBase64() {
        val backup = BackupFile(data = InventoryData(items = listOf(Item(id = "a", name = "A", photos = listOf("/x.jpg")))), photos = mapOf("/x.jpg" to "%%%"))
        val restored = Backup.restoredData(backup, "/p", IdSequence()::next, writeBytes = { _, _ -> error("unexpected") }, exists = { true })
        assertTrue(restored.items.single().photos.isEmpty())
    }

    @Test
    fun restoredDataWritesRealFiles() {
        val dir = "$SystemTemporaryDirectory/hi-backup-${Random.nextLong().toULong()}"
        val backup = Backup.parse(Backup.create(dataWithPhotos(), 1L) { photoFiles[it] })
        val restored = Backup.restoredData(backup, dir, IdSequence()::next)
        val path = restored.items[0].photos.single()
        assertContentEquals(photoA, Files.readBytes(path))
        restored.items.flatMap { it.photos }.forEach(Files::delete)
    }

    @Test
    fun repositoryRestoreReplacesDataAndDeletesUnusedPhotos() = runTest {
        val deleted = mutableListOf<String>()
        val storage = InMemoryStorage(
            BackupCodec.encode(
                InventoryData(
                    items = listOf(
                        Item(id = "x", name = "Old", photos = listOf("/keep.jpg", "/drop.jpg")),
                    ),
                    settings = AppSettings(hideValues = true),
                ),
            ),
        )
        val repo = InventoryRepository(storage, CoroutineScope(Dispatchers.Unconfined), FixedClock(), deleteFile = { deleted += it })
        repo.load()
        val incoming = InventoryData(items = listOf(Item(id = "y", name = "New", photos = listOf("/keep.jpg", "/new.jpg"))))
        repo.restore(incoming)
        repo.flush()

        assertEquals(incoming, repo.data.value)
        assertEquals(listOf("/drop.jpg"), deleted)
        assertEquals(incoming, BackupCodec.decode(storage.content!!))
    }
}
