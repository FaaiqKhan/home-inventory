package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.localentertainmentgames.homeinventory.domain.ItemDraft
import com.localentertainmentgames.homeinventory.model.Item

/** State machine for screen 1g: camera (scan) -> form -> save & add another / save & close. */
class AddItemFlow(startManual: Boolean = false, initialLocation: List<String> = listOf(Item.HOME)) {
    /** True once a barcode was read, a photo taken, or "Enter Manually" tapped. */
    var scanned by mutableStateOf(startManual)
        private set
    var draft by mutableStateOf(ItemDraft(location = initialLocation))
    var flashOn by mutableStateOf(false)
        private set
    var savedCount by mutableStateOf(0)
        private set
    var showErrors by mutableStateOf(false)
        private set

    /** The camera explainer opens by itself once, the first time permission is undecided. */
    var explainerOpen by mutableStateOf(false)
    var explainerDismissed by mutableStateOf(false)

    val title: String get() = if (scanned) "New Item" else "Scan Barcode"
    val hasBarcode: Boolean get() = draft.barcode.isNotBlank()

    fun onBarcode(code: String) {
        if (scanned) return
        draft = draft.copy(barcode = code)
        scanned = true
    }

    fun enterManually() {
        draft = draft.copy(barcode = "")
        scanned = true
    }

    fun onPhotoTaken(path: String) {
        draft = draft.copy(photos = draft.photos + path)
        scanned = true
    }

    fun rescan() {
        draft = draft.copy(barcode = "", quantity = 1)
        scanned = false
        showErrors = false
    }

    fun qtyUp() { draft = draft.increment() }
    fun qtyDown() { draft = draft.decrement() }
    fun toggleFlash() { flashOn = !flashOn }

    /** Returns the item to save, or null (and shows errors) when the form is invalid. */
    fun validated(): Item? {
        if (!draft.isValid) {
            showErrors = true
            return null
        }
        return draft.toItem()
    }

    /** After "Save & Add Another": count it, keep the location, go back to the camera. */
    fun resetForNext() {
        savedCount++
        draft = ItemDraft(location = draft.location)
        scanned = false
        showErrors = false
        flashOn = false
    }

    fun openExplainer() { explainerOpen = true }

    fun dismissExplainer() {
        explainerOpen = false
        explainerDismissed = true
    }
}
