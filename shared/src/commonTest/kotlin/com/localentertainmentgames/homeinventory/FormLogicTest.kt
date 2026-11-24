package com.localentertainmentgames.homeinventory

import com.localentertainmentgames.homeinventory.domain.ItemDraft
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import com.localentertainmentgames.homeinventory.platform.CameraAccess
import com.localentertainmentgames.homeinventory.platform.CameraPermission
import com.localentertainmentgames.homeinventory.ui.navigation.Navigator
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.navigation.Tab
import com.localentertainmentgames.homeinventory.ui.screens.AddItemFlow
import com.localentertainmentgames.homeinventory.ui.screens.continueCameraRequest
import com.localentertainmentgames.homeinventory.ui.screens.photoLabel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormLogicTest {
    @Test
    fun draftValidation() {
        val d = ItemDraft()
        assertFalse(d.isValid)
        assertNotNull(d.nameError)
        val named = d.copy(name = "Lamp")
        assertTrue(named.isValid)
        assertFalse(named.copy(priceText = "twelve").isValid)
        assertTrue(named.copy(priceText = "12.50").isValid)
    }

    @Test
    fun draftQuantityNeverBelowOne() {
        val d = ItemDraft()
        assertEquals(1, d.decrement().quantity)
        assertEquals(3, d.increment().increment().quantity)
    }

    @Test
    fun draftConvertsBothWays() {
        val item = Item(
            id = "a", name = "Camera", location = listOf("Home", "Office", "Desk"), category = "Electronics", quantity = 2,
            priceCents = 249_950, purchaseDate = LocalDate(2023, 8, 2), condition = "Good", serial = "S1", barcode = "B1",
            expiresOn = LocalDate(2030, 1, 1), tags = setOf(ItemTag.Insured), photos = listOf("/p.jpg"), notes = "n", createdAtMillis = 5,
        )
        val draft = ItemDraft.from(item)
        assertEquals("2499.50", draft.priceText)
        assertEquals(item, draft.toItem(id = "a", createdAtMillis = 5))
        assertEquals(setOf(ItemTag.Insured, ItemTag.Lost), draft.toggle(ItemTag.Lost).tags)
    }

    @Test
    fun locationBuilder() {
        assertEquals(listOf("Home", "Garage", "Shelf B"), ItemDraft.locationOf(" Garage ", "Shelf B"))
        assertEquals(listOf("Home", "Garage"), ItemDraft.locationOf("Garage", ""))
        assertEquals(listOf("Home"), ItemDraft.locationOf("", ""))
        assertEquals(listOf("Home"), ItemDraft(name = "x", location = listOf(" ")).toItem().location)
    }

    @Test
    fun addFlowScanToSave() {
        val flow = AddItemFlow(initialLocation = listOf("Home", "Garage", "Shelf B"))
        assertFalse(flow.scanned)
        assertEquals("Scan Barcode", flow.title)
        flow.onBarcode("123")
        assertTrue(flow.scanned)
        assertEquals("New Item", flow.title)
        assertTrue(flow.hasBarcode)
        flow.onBarcode("456") // later reads are ignored once on the form
        assertEquals("123", flow.draft.barcode)

        flow.qtyUp(); flow.qtyUp(); flow.qtyDown()
        assertEquals(2, flow.draft.quantity)
        assertNull(flow.validated())
        assertTrue(flow.showErrors)

        flow.draft = flow.draft.copy(name = "Drill")
        val item = assertNotNull(flow.validated())
        assertEquals("123", item.barcode)
        assertEquals(listOf("Home", "Garage", "Shelf B"), item.location)

        flow.resetForNext()
        assertEquals(1, flow.savedCount)
        assertFalse(flow.scanned)
        assertFalse(flow.showErrors)
        assertEquals("", flow.draft.name)
        assertEquals(listOf("Home", "Garage", "Shelf B"), flow.draft.location)
    }

    @Test
    fun addFlowManualPhotoRescanFlash() {
        val flow = AddItemFlow(startManual = true)
        assertTrue(flow.scanned)
        assertFalse(flow.hasBarcode)
        flow.rescan()
        assertFalse(flow.scanned)
        flow.onPhotoTaken("/p.jpg")
        assertTrue(flow.scanned)
        assertEquals(listOf("/p.jpg"), flow.draft.photos)
        flow.toggleFlash()
        assertTrue(flow.flashOn)
        flow.openExplainer()
        assertTrue(flow.explainerOpen)
        flow.dismissExplainer()
        assertFalse(flow.explainerOpen)
        assertTrue(flow.explainerDismissed)
    }

    @Test
    fun navigatorStackAndTabs() {
        val nav = Navigator()
        assertFalse(nav.canPop)
        assertFalse(nav.pop())
        nav.push(Screen.Items())
        nav.push(Screen.Detail("a"))
        nav.push(Screen.Detail("a"))
        assertEquals(3, nav.stack.size)
        nav.push(Screen.Edit("a"))
        nav.removeItemScreens("a")
        assertEquals(Screen.Items(), nav.current)
        nav.replace(Screen.Report)
        assertEquals(Tab.Report, nav.currentTab)
        nav.selectTab(Tab.Add)
        assertEquals(listOf(Screen.Home, Screen.Add()), nav.stack.toList())
        nav.selectTab(Tab.Home)
        assertEquals(listOf<Screen>(Screen.Home), nav.stack.toList())
        assertEquals(Tab.Home, nav.currentTab)
        nav.push(Screen.Evacuation)
        assertNull(nav.currentTab)
        assertTrue(nav.pop())
    }

    @Test
    fun photoLabels() {
        assertEquals(listOf("Item", "Serial plate", "Receipt", "Photo 4"), (0..3).map(::photoLabel))
    }

    private class Access(override var status: CameraPermission, override val canRequest: Boolean, val grant: Boolean) : CameraAccess {
        var asked = 0
        override fun request(onResult: (Boolean) -> Unit) {
            asked++
            status = if (grant) CameraPermission.Granted else CameraPermission.Denied
            onResult(grant)
        }
    }

    @Test
    fun cameraContinueRequestsOrOpensSettings() {
        var settings = 0
        var granted = 0
        val askable = Access(CameraPermission.NotDetermined, canRequest = true, grant = true)
        continueCameraRequest(askable, { settings++ }) { granted++ }
        assertEquals(1, askable.asked)
        assertEquals(1, granted)

        val refused = Access(CameraPermission.NotDetermined, canRequest = true, grant = false)
        continueCameraRequest(refused, { settings++ }) { granted++ }
        assertEquals(1, granted)
        assertEquals(0, settings)

        val blocked = Access(CameraPermission.Denied, canRequest = false, grant = false)
        continueCameraRequest(blocked, { settings++ }) { granted++ }
        assertEquals(0, blocked.asked)
        assertEquals(1, settings)
    }
}
