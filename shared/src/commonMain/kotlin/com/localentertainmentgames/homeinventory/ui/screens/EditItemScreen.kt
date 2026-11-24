package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.domain.ItemDraft
import com.localentertainmentgames.homeinventory.domain.rooms
import com.localentertainmentgames.homeinventory.platform.LocalAppPlatform
import com.localentertainmentgames.homeinventory.ui.components.AppTopBar
import com.localentertainmentgames.homeinventory.ui.components.ConfirmDialog
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme

@Composable
fun EditItemScreen(c: AppController, itemId: String) {
    val data by c.repo.data.collectAsState()
    val item = data.items.firstOrNull { it.id == itemId }
    if (item == null) {
        LaunchedEffect(Unit) { c.nav.removeItemScreens(itemId) }
        return
    }
    var draft by remember(itemId) { mutableStateOf(ItemDraft.from(item)) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val platform = LocalAppPlatform.current
    val capture = platform.rememberPhotoCapture { path -> if (path != null) draft = draft.copy(photos = draft.photos + path) }
    val addPhoto = rememberCameraGate(onGranted = capture)

    fun save() {
        if (!draft.isValid) {
            showErrors = true
            return
        }
        c.repo.updateItem(draft.toItem(id = item.id, createdAtMillis = item.createdAtMillis))
        c.nav.pop()
        c.showToast("Changes saved")
    }

    ScreenScaffold(topBar = { AppTopBar("Edit item", onNavigate = { c.nav.pop() }, navIcon = AppIcons.Close, navLabel = "Cancel") }) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ItemFormFields(
                    draft = draft,
                    onChange = { draft = it },
                    showErrors = showErrors,
                    rooms = data.items.rooms(),
                    onAddPhoto = addPhoto,
                    nameHelper = null,
                    detailsInitiallyOpen = true,
                )
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Delete Item", style = AppTheme.text.buttonSmall, color = AppTheme.colors.error)
                }
            }
            HorizontalDivider(color = AppTheme.colors.border)
            PrimaryButton("Save Changes", onClick = ::save, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this item?",
            message = "\"${item.name}\" and its photos are removed from this phone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                c.repo.deleteItem(item.id)
                c.nav.removeItemScreens(item.id)
                c.showToast("Item deleted")
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
