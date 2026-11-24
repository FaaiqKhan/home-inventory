package com.localentertainmentgames.homeinventory

import com.localentertainmentgames.homeinventory.data.BackupCodec
import com.localentertainmentgames.homeinventory.data.InMemoryStorage
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.localentertainmentgames.homeinventory.model.Contact
import com.localentertainmentgames.homeinventory.model.DashboardLayout
import com.localentertainmentgames.homeinventory.model.DefaultGoBag
import com.localentertainmentgames.homeinventory.model.InventoryData
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class RepositoryTest {
    @Test
    fun startsEmptyWithDefaultGoBag() = runTest {
        val repo = testRepository()
        repo.load()
        assertTrue(repo.loaded.value)
        assertTrue(repo.data.value.items.isEmpty())
        assertEquals(DefaultGoBag.names, repo.data.value.goBag.map { it.name })
        assertTrue(repo.data.value.goBag.none { it.packed })
    }

    @Test
    fun loadsPersistedDataAndIgnoresCorruptFiles() = runTest {
        val data = sampleData()
        val repo = testRepository(data)
        repo.load()
        assertEquals(data, repo.data.value)

        val broken = testRepository(storage = InMemoryStorage("{not json"))
        broken.load()
        assertTrue(broken.loaded.value)
        assertTrue(broken.data.value.items.isEmpty())
    }

    @Test
    fun everyChangeIsPersisted() = runTest {
        val storage = InMemoryStorage()
        val repo = testRepository(storage = storage)
        repo.load()
        val saved = repo.addItem(Item(id = "", name = "  Kettle  ", priceCents = 2500))
        repo.flush()
        assertEquals("id-0", saved.id)
        assertEquals("Kettle", saved.name)
        assertEquals(FIXED_NOW.toEpochMilliseconds(), saved.createdAtMillis)
        val reloaded = testRepository(storage = InMemoryStorage(storage.content))
        reloaded.load()
        assertEquals(listOf(saved), reloaded.data.value.items)
        assertEquals(1, storage.writes)
    }

    @Test
    fun updatesTagsPhotosAndDeletes() = runTest {
        val repo = testRepository()
        repo.load()
        val item = repo.addItem(Item(id = "", name = "Drill"))
        repo.toggleTag(item.id, ItemTag.Lost)
        assertEquals(setOf(ItemTag.Lost), repo.item(item.id)!!.tags)
        repo.toggleTag(item.id, ItemTag.Lost)
        assertTrue(repo.item(item.id)!!.tags.isEmpty())

        repo.addPhoto(item.id, "/p/1.jpg")
        repo.addPhoto(item.id, "/p/2.jpg")
        assertEquals(listOf("/p/1.jpg", "/p/2.jpg"), repo.item(item.id)!!.photos)
        repo.removePhoto(item.id, "/p/1.jpg")
        assertEquals(listOf("/p/2.jpg"), repo.item(item.id)!!.photos)

        repo.updateItem(repo.item(item.id)!!.copy(name = " Hammer drill ", quantity = 2))
        assertEquals("Hammer drill", repo.item(item.id)!!.name)
        assertEquals(2, repo.item(item.id)!!.quantity)

        repo.deleteItem(item.id)
        assertNull(repo.item(item.id))
    }

    @Test
    fun deletingRemovesPhotoFiles() = runTest {
        val deleted = mutableListOf<String>()
        val repo = InventoryRepository(InMemoryStorage(), CoroutineScope(Dispatchers.Unconfined), FixedClock(), IdSequence()::next) { deleted += it }
        repo.load()
        val item = repo.addItem(Item(id = "", name = "Lamp", photos = listOf("/p/a.jpg", "/p/b.jpg")))
        repo.removePhoto(item.id, "/p/a.jpg")
        assertEquals(listOf("/p/a.jpg"), deleted)
        repo.deleteItem(item.id)
        assertEquals(listOf("/p/a.jpg", "/p/b.jpg"), deleted)
    }

    @Test
    fun goBagAndEvacuation() = runTest {
        val clock = FixedClock()
        val repo = testRepository(clock = clock)
        repo.load()
        val first = repo.data.value.goBag.first()
        repo.toggleGoBag(first.id)
        assertTrue(repo.data.value.goBag.first().packed)
        repo.toggleGoBag(first.id)
        assertFalse(repo.data.value.goBag.first().packed)

        repo.addGoBagEntry("  Spare keys ")
        assertEquals("Spare keys", repo.data.value.goBag.last().name)
        repo.addGoBagEntry("   ")
        assertEquals(13, repo.data.value.goBag.size)
        repo.removeGoBagEntry(repo.data.value.goBag.last().id)
        assertEquals(12, repo.data.value.goBag.size)

        repo.toggleEvacuating()
        assertEquals(FIXED_NOW.toEpochMilliseconds(), repo.data.value.evacuatingSinceMillis)
        clock.instant = FIXED_NOW + 5.minutes
        repo.toggleEvacuating()
        assertNull(repo.data.value.evacuatingSinceMillis)
    }

    @Test
    fun planDropsBlankContacts() = runTest {
        val repo = testRepository()
        repo.load()
        repo.setPlan(" Library ", listOf(Contact("Sam", "Partner", "123"), Contact("", "", "")))
        assertEquals("Library", repo.data.value.plan.meetingPoint)
        assertEquals(listOf(Contact("Sam", "Partner", "123")), repo.data.value.plan.contacts)
    }

    @Test
    fun settingsSampleAndErase() = runTest {
        val repo = testRepository()
        repo.load()
        repo.setLayout(DashboardLayout.ActionFirst)
        repo.setHideValues(true)
        repo.markBackedUp()
        assertEquals(FIXED_NOW.toEpochMilliseconds(), repo.data.value.settings.lastBackupAtMillis)

        repo.loadSample()
        assertTrue(repo.data.value.items.isNotEmpty())
        assertEquals(DashboardLayout.ActionFirst, repo.data.value.settings.dashboardLayout)
        assertTrue(repo.data.value.settings.hideValues)
        assertNotNull(repo.data.value.plan.meetingPoint)

        repo.eraseAll()
        assertTrue(repo.data.value.items.isEmpty())
        assertEquals("", repo.data.value.plan.meetingPoint)
        assertTrue(repo.data.value.settings.hideValues)
    }

    @Test
    fun backupCodecRoundTripsAndToleratesUnknownKeys() {
        val data = sampleData()
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
        val withExtra = BackupCodec.encode(InventoryData()).replaceFirst("{", "{\"futureField\": 42,")
        assertEquals(InventoryData(), BackupCodec.decode(withExtra))
    }
}
