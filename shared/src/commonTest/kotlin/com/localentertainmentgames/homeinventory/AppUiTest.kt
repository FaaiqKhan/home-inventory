package com.localentertainmentgames.homeinventory

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.localentertainmentgames.homeinventory.data.BackupCodec
import com.localentertainmentgames.homeinventory.data.Files
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.InventoryData
import com.localentertainmentgames.homeinventory.model.ItemTag
import com.localentertainmentgames.homeinventory.platform.CameraPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppUiTest {
    private class Harness(val repo: InventoryRepository, val platform: FakePlatform)

    private fun uiTest(
        data: InventoryData? = sampleData(),
        platform: FakePlatform = FakePlatform(),
        block: ComposeUiTest.(Harness) -> Unit,
    ) = runComposeUiTest {
        val repo = testRepository(data)
        setContent { App(platform = platform, repository = repo, clock = FixedClock(), darkTheme = false) }
        waitUntil(timeoutMillis = 5_000) { repo.loaded.value }
        waitForIdle()
        block(Harness(repo, platform))
    }

    private fun ComposeUiTest.tab(name: String) = onNodeWithTag("nav-$name").performClick()

    private fun ComposeUiTest.search(text: String) {
        onNodeWithTag("search").performTextClearance()
        onNodeWithTag("search").performTextInput(text)
        waitForIdle()
    }

    // Home

    @Test
    fun homeValueFirstShowsSummary() = uiTest { h ->
        val total = h.repo.data.value.items.sumOf { it.totalValueCents }
        onNodeWithTag("total-value").assertTextEquals(Format.money(total))
        onNodeWithText("13 items").assertExists()
        onNodeWithText("0 with photos").assertExists()
        onNodeWithText("9 of 12", substring = true).assertExists()
        onNodeWithText("3 missing", substring = true).assertExists()
        onNodeWithText("Meds · 6 days").assertExists()
        onNodeWithText("Food · 2").assertExists()
        onNodeWithTag("last-backup").assertTextEquals("Never backed up")
        onNodeWithText("Offline · on device").assertExists()
    }

    @Test
    fun goBagCardOpensEvacuationAndCloseReturnsHome() = uiTest {
        onNodeWithTag("gobag-card").performClick()
        onNodeWithText("EVACUATION MODE").assertIsDisplayed()
        onNodeWithContentDescription("Close evacuation mode").performClick()
        onNodeWithText("Home Inventory").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationReachesEveryTab() = uiTest {
        tab("Items")
        onNodeWithTag("result-count").assertTextEquals("13 items")
        tab("Report")
        onNodeWithText("Insurance Report").assertIsDisplayed()
        tab("Add")
        onNodeWithText("Scan Barcode").assertIsDisplayed()
        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("Home Inventory").assertIsDisplayed()
        tab("Items")
        tab("Home")
        onNodeWithTag("total-value").assertExists()
    }

    @Test
    fun settingsSwitchLayoutsAndHideValues() = uiTest { h ->
        onNodeWithContentDescription("Settings").performClick()
        onNodeWithTag("layout-ReadinessFirst").performClick()
        waitForIdle()
        onNodeWithText("Still to pack").assertExists()
        onNodeWithText("Open Evacuation Mode").assertExists()
        onNodeWithText("Prescription meds").assertExists()

        onNodeWithTag("layout-ActionFirst").performClick()
        waitForIdle()
        onNodeWithText("9 of 12 go-bag items packed").assertExists()
        onNodeWithText("Prescription meds expires in 6 days").assertExists()
        onNodeWithText("+ 2 food, 1 batteries in the next 30 days").assertExists()

        onNodeWithTag("hide-values").performClick()
        waitForIdle()
        onNodeWithTag("hide-values").assertIsOn()
        onAllNodesWithText(Format.HIDDEN_MONEY).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
        assertTrue(h.repo.data.value.settings.hideValues)
    }

    @Test
    fun settingsLoadSampleAndEraseAll() = uiTest(data = InventoryData()) { h ->
        onNodeWithContentDescription("Settings").performClick()
        onNodeWithText("Load Sample Inventory").performClick()
        waitForIdle()
        assertEquals(13, h.repo.data.value.items.size)
        onNodeWithContentDescription("Settings").performClick()
        onNodeWithText("Erase All Data").performClick()
        onNodeWithText("Erase").performClick()
        waitForIdle()
        assertTrue(h.repo.data.value.items.isEmpty())
    }

    @Test
    fun exportBackupWritesJsonAndUpdatesLastBackup() = uiTest { h ->
        onNodeWithText("Export Backup").performClick()
        waitUntil(timeoutMillis = 5_000) { h.platform.saved.isNotEmpty() }
        val path = h.platform.saved.single()
        assertTrue(path.endsWith(".json"))
        val decoded = BackupCodec.decode(assertNotNull(Files.readText(path)))
        assertEquals(h.repo.data.value.items, decoded.items)
        waitForIdle()
        onNodeWithTag("last-backup").assertTextEquals("Last backup today")
    }

    @Test
    fun expiringCardOpensFilteredList() = uiTest {
        onNodeWithTag("expiring-card").performClick()
        onNodeWithTag("result-count").assertTextEquals("4 items")
        onNodeWithText("Prescription meds").assertExists()
    }

    // Item list

    @Test
    fun searchTagFiltersAndClear() = uiTest {
        tab("Items")
        search("drill")
        onNodeWithTag("result-count").assertTextEquals("1 item")
        onNodeWithText("Cordless drill").assertIsDisplayed()

        search("")
        onNodeWithTag("chip-insured").performClick()
        onNodeWithTag("result-count").assertTextEquals("4 items")
        onNodeWithTag("chip-go-bag").performClick()
        onNodeWithTag("result-count").assertTextEquals("1 item")
        onNodeWithText("Canon EOS R6").assertIsDisplayed()

        search("zzz")
        onNodeWithTag("empty-no-matches").assertIsDisplayed()
        onNodeWithText("Clear Filters").performClick()
        onNodeWithTag("result-count").assertTextEquals("13 items")
    }

    @Test
    fun roomAndCategoryDropdowns() = uiTest {
        tab("Items")
        onNodeWithTag("dropdown-Room").performClick()
        onNodeWithText("Garage").performClick()
        onNodeWithTag("result-count").assertTextEquals("2 items")
        onNodeWithTag("dropdown-Room").performClick()
        onNodeWithText("All rooms").performClick()
        onNodeWithTag("dropdown-Category").performClick()
        onNodeWithText("Food").performClick()
        onNodeWithTag("result-count").assertTextEquals("2 items")
        onNodeWithText("Granola bars").assertIsDisplayed()
    }

    @Test
    fun firstRunEmptyStateLeadsToAdd() = uiTest(data = InventoryData()) {
        tab("Items")
        onNodeWithTag("empty-first-run").assertIsDisplayed()
        onNodeWithText("Nothing here yet").assertIsDisplayed()
        onNodeWithText("Add Manually").performClick()
        onNodeWithText("New Item").assertIsDisplayed()
    }

    @Test
    fun firstRunScanButtonOpensScanner() = uiTest(data = InventoryData()) {
        tab("Items")
        onNodeWithText("Scan First Item").performClick()
        onNodeWithText("Scan Barcode").assertIsDisplayed()
        onNodeWithTag("fake-scanner").assertExists()
    }

    // Detail, status, photos, edit

    private fun ComposeUiTest.openMacBook() {
        tab("Items")
        search("macbook")
        onNodeWithText("MacBook Pro 14″").performClick()
        onNodeWithTag("item-name").assertTextEquals("MacBook Pro 14″")
    }

    @Test
    fun detailShowsFieldsAndMarksStatus() = uiTest { h ->
        openMacBook()
        onNodeWithText("C02XK3Y9MD6T").assertExists()
        onNodeWithText("$1,999").assertExists()
        onNodeWithText("Mar 12, 2024").assertExists()
        onNodeWithText("Home → Office → Desk").assertExists()
        onNodeWithText("NO PHOTO YET").assertExists()

        onNodeWithText("Mark Status").performClick()
        onNodeWithTag("status-lost").performClick()
        onNodeWithTag("status-insured").performClick()
        onNodeWithText("Done").performClick()
        waitForIdle()
        val tags = h.repo.data.value.items.first { it.name.startsWith("MacBook") }.tags
        assertEquals(setOf(ItemTag.Lost), tags)
        onNodeWithTag("status-badges").assertExists()
        onNodeWithText("lost").assertExists()
    }

    @Test
    fun detailAddPhotoWithPermission() = uiTest { h ->
        openMacBook()
        onNodeWithText("Add Photo").performClick()
        waitForIdle()
        onNodeWithTag("photo-pos").assertTextEquals("1 / 1")
        onNodeWithTag("thumb-0").assertExists()
        assertEquals(listOf("/photos/p1.jpg"), h.repo.data.value.items.first { it.name.startsWith("MacBook") }.photos)
    }

    @Test
    fun detailAddPhotoAsksForCameraFirst() = uiTest(platform = FakePlatform(CameraPermission.NotDetermined)) { h ->
        openMacBook()
        onNodeWithText("Add Photo").performClick()
        onNodeWithText("Allow camera access?").assertIsDisplayed()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        assertEquals(1, h.platform.permissionRequests)
        assertEquals(1, h.platform.photoCaptures)
        onNodeWithTag("photo-pos").assertTextEquals("1 / 1")
    }

    @Test
    fun detailAddPhotoNotNowDoesNothing() = uiTest(platform = FakePlatform(CameraPermission.NotDetermined)) { h ->
        openMacBook()
        onNodeWithText("Add Photo").performClick()
        onNodeWithText("Not now").performClick()
        waitForIdle()
        assertEquals(0, h.platform.photoCaptures)
        onNodeWithText("NO PHOTO YET").assertExists()
    }

    @Test
    fun editRenamesAndDeletes() = uiTest { h ->
        openMacBook()
        onNodeWithText("Edit").performClick()
        onNodeWithText("Edit item").assertIsDisplayed()
        onNodeWithTag("field-Name").performTextClearance()
        onNodeWithText("Save Changes").performClick()
        onNodeWithText("Give the item a name").assertExists()
        onNodeWithTag("field-Name").performTextInput("MacBook Air")
        onNodeWithText("Save Changes").performClick()
        onNodeWithTag("item-name").assertTextEquals("MacBook Air")

        onNodeWithText("Edit").performClick()
        onNodeWithText("Delete Item").performScrollTo().performClick()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertTrue(h.repo.data.value.items.none { it.name == "MacBook Air" })
        onNodeWithTag("result-count").assertExists()
    }

    // Evacuation

    @Test
    fun evacuationChecklistAndLog() = uiTest { h ->
        onNodeWithTag("gobag-card").performClick()
        onNodeWithTag("packed-count").assertTextEquals("9 / 12")
        onNodeWithTag("bag-Flashlight").performClick()
        onNodeWithTag("packed-count").assertTextEquals("10 / 12")
        onNodeWithTag("bag-Flashlight").performClick()
        onNodeWithTag("packed-count").assertTextEquals("9 / 12")

        onNodeWithText("I'm Evacuating").performClick()
        onNodeWithText("Evacuating").assertIsDisplayed()
        onNodeWithTag("evacuate").assertTextContains("Since ", substring = true)
        assertEquals(FIXED_NOW.toEpochMilliseconds(), h.repo.data.value.evacuatingSinceMillis)
        onNodeWithText("Evacuating").performClick()
        onNodeWithText("I'm Evacuating").assertIsDisplayed()

        onNodeWithText("Oak Street Library, north car park").assertIsDisplayed()
        onNodeWithTag("call-Sam").performClick()
        assertEquals(listOf("+15555550101"), h.platform.dialed)
    }

    @Test
    fun evacuationPlanCanBeEdited() = uiTest(data = InventoryData()) { h ->
        tab("Home")
        onNodeWithText("Evacuation Mode").performClick()
        onNodeWithTag("meeting-point").assertTextEquals("Not set yet")
        onNodeWithContentDescription("Edit plan").performClick()
        onNodeWithTag("field-Meeting point").performTextInput("Town hall steps")
        onNodeWithTag("field-contact-name-0").performTextInput("Ana")
        onNodeWithTag("field-contact-phone-0").performTextInput("555")
        onNodeWithText("Save").performClick()
        onNodeWithTag("meeting-point").assertTextEquals("Town hall steps")
        onNodeWithTag("call-Ana").performClick()
        assertEquals(listOf("555"), h.platform.dialed)
    }

    @Test
    fun evacuationAddsGoBagEntry() = uiTest { h ->
        onNodeWithTag("gobag-card").performClick()
        onNodeWithTag("gobag-list").assertExists()
        h.repo.addGoBagEntry("Spare keys")
        waitForIdle()
        onNodeWithTag("packed-count").assertTextEquals("9 / 13")
    }

    // Add / scan

    @Test
    fun scanThenSaveAndAddAnother() = uiTest { h ->
        tab("Add")
        onNodeWithText("Scan Barcode").assertIsDisplayed()
        onNodeWithTag("fake-scanner").performClick()
        onNodeWithText("New Item").assertIsDisplayed()
        onNodeWithTag("barcode").assertTextEquals(SAMPLE_BARCODE)
        onNodeWithText("Save & Add Another").performClick()
        onNodeWithText("Give the item a name").assertExists()

        onNodeWithTag("field-Name").performTextInput("Cordless sander")
        onNodeWithContentDescription("More").performClick()
        onNodeWithContentDescription("More").performClick()
        onNodeWithTag("qty").assertTextEquals("3")
        onNodeWithTag("field-Price").performTextInput("89.99")
        onNodeWithText("Save & Add Another").performClick()
        waitForIdle()
        onNodeWithTag("saved-count").assertTextEquals("1 saved")
        onNodeWithText("Scan Barcode").assertIsDisplayed()
        val saved = h.repo.data.value.items.first { it.name == "Cordless sander" }
        assertEquals(SAMPLE_BARCODE, saved.barcode)
        assertEquals(3, saved.quantity)
        assertEquals(8999L, saved.priceCents)
    }

    @Test
    fun rescanReturnsToCamera() = uiTest {
        tab("Add")
        onNodeWithTag("fake-scanner").performClick()
        onNodeWithText("Rescan").performClick()
        onNodeWithText("Scan Barcode").assertIsDisplayed()
    }

    @Test
    fun manualEntrySaveAndCloseShowsList() = uiTest(data = InventoryData()) { h ->
        tab("Add")
        onNodeWithText("Enter Manually").performClick()
        onNodeWithTag("field-Name").performTextInput("Toaster")
        onNodeWithTag("select-Category").performClick()
        onNodeWithTag("option-Kitchen").performClick()
        onNodeWithTag("select-Location").performClick()
        onNodeWithTag("field-Room").performTextInput("Kitchen")
        onNodeWithTag("field-Spot").performTextInput("Counter")
        onNodeWithText("Done").performClick()
        onNodeWithText("Home → Kitchen → Counter").assertExists()
        onNodeWithText("Save & Close").performClick()
        waitForIdle()
        onNodeWithTag("result-count").assertTextEquals("1 item")
        onNodeWithText("Toaster").assertIsDisplayed()
        val item = h.repo.data.value.items.single()
        assertEquals("Kitchen", item.category)
        assertEquals(listOf("Home", "Kitchen", "Counter"), item.location)
    }

    @Test
    fun takePhotoFromScannerAndToggleFlash() = uiTest { h ->
        tab("Add")
        onNodeWithTag("flash").performClick()
        waitForIdle()
        assertEquals(true, h.platform.lastTorch)
        onNodeWithText("Take Photo").performClick()
        onNodeWithText("New Item").assertIsDisplayed()
        onNodeWithContentDescription("Remove photo").assertExists()
    }

    @Test
    fun cameraExplainerThenGrant() = uiTest(platform = FakePlatform(CameraPermission.NotDetermined)) { h ->
        tab("Add")
        onNodeWithText("Allow camera access?").assertIsDisplayed()
        onNodeWithText("Not now").performClick()
        onNodeWithTag("camera-blocked").assertIsDisplayed()
        onNodeWithText("Camera access is off").assertIsDisplayed()
        onNodeWithText("Why does it need the camera?").performClick()
        onNodeWithText("Allow camera access?").assertIsDisplayed()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        assertEquals(1, h.platform.permissionRequests)
        onNodeWithTag("fake-scanner").assertExists()
    }

    @Test
    fun cameraBlockedOpensSettingsAndAllowsManual() = uiTest(platform = FakePlatform(CameraPermission.Denied, canRequestPermission = false)) { h ->
        tab("Add")
        onNodeWithTag("camera-blocked").assertIsDisplayed()
        onNode(hasText("Settings → Apps → Home Inventory → Permissions → Camera", substring = true)).assertExists()
        onNodeWithText("Open Settings").performClick()
        assertEquals(1, h.platform.settingsOpened)
        onNodeWithText("Why does it need the camera?").performClick()
        onNodeWithText("Continue").performClick()
        assertEquals(2, h.platform.settingsOpened)
        assertEquals(0, h.platform.permissionRequests)
        onNodeWithText("Enter Manually").performClick()
        onNodeWithText("New Item").assertIsDisplayed()
    }

    // Report

    @Test
    fun reportPreviewAndExports() = uiTest { h ->
        tab("Report")
        val total = h.repo.data.value.items.sumOf { it.totalValueCents }
        onNodeWithText(Format.money(total)).assertIsDisplayed()
        onNodeWithTag("missing-photos").assertTextEquals("13 items have no photo. Insurers may ask for one.")
        onNodeWithTag("report-more").assertTextEquals("+ 7 more items in the full PDF")
        onNodeWithText("Canon EOS R6").assertIsDisplayed()

        onNodeWithText("Export PDF").performClick()
        waitUntil(timeoutMillis = 5_000) { h.platform.saved.isNotEmpty() }
        val pdf = assertNotNull(Files.readBytes(h.platform.saved.single()))
        assertEquals("%PDF", pdf.copyOf(4).decodeToString())

        onNodeWithText("Share").performClick()
        waitUntil(timeoutMillis = 5_000) { h.platform.shared.isNotEmpty() }
        assertTrue(h.platform.shared.single().endsWith(".pdf"))
    }

    @Test
    fun reportRowOpensDetail() = uiTest {
        tab("Report")
        onNodeWithText("Canon EOS R6").performClick()
        onNodeWithTag("item-name").assertTextEquals("Canon EOS R6")
    }

    @Test
    fun filtersSurviveOpeningAnItem() = uiTest {
        tab("Items")
        onNodeWithTag("chip-insured").performClick()
        search("canon")
        onNodeWithText("Canon EOS R6").performClick()
        onNodeWithTag("item-name").assertTextEquals("Canon EOS R6")
        onNodeWithContentDescription("Back").performClick()
        onNodeWithTag("result-count").assertTextEquals("1 item")
        onNodeWithTag("search").assertTextContains("canon")
    }

    @Test
    fun evacuationIsReachableFromHomeButton() = uiTest {
        onNode(hasText("Evacuation Mode") and !hasTestTag("gobag-card")).performClick()
        onNodeWithText("EVACUATION MODE").assertIsDisplayed()
    }
}
