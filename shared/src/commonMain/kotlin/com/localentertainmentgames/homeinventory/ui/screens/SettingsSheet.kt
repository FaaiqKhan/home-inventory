package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.model.AppSettings
import com.localentertainmentgames.homeinventory.model.DashboardLayout
import com.localentertainmentgames.homeinventory.ui.components.ConfirmDialog
import com.localentertainmentgames.homeinventory.ui.components.Overline
import com.localentertainmentgames.homeinventory.ui.components.SecondaryButton
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Radius

@Composable
fun SettingsSheet(c: AppController, settings: AppSettings, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmErase by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl),
        containerColor = AppTheme.colors.bg,
        modifier = Modifier.testTag("settings-sheet"),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.fg1)
            Overline("Home layout", modifier = Modifier.padding(top = 8.dp))
            DashboardLayout.entries.forEach { layout ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = settings.dashboardLayout == layout, role = Role.RadioButton) { c.repo.setLayout(layout) }
                        .testTag("layout-${layout.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.dashboardLayout == layout,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = AppTheme.colors.cta),
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(layout.label, style = AppTheme.text.bodyLgBold, color = AppTheme.colors.fg1)
                        Text(layout.description, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
                    }
                }
            }
            HorizontalDivider(color = AppTheme.colors.border)
            Overline("Privacy", modifier = Modifier.padding(top = 8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(value = settings.hideValues, role = Role.Switch) { c.repo.setHideValues(it) }
                    .testTag("hide-values"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Hide values", style = AppTheme.text.bodyLgBold, color = AppTheme.colors.fg1)
                    Text("Masks money on screen. Exported reports still show values.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
                }
                Switch(
                    checked = settings.hideValues,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = AppTheme.colors.cta),
                )
            }
            HorizontalDivider(color = AppTheme.colors.border)
            Overline("Data", modifier = Modifier.padding(top = 8.dp))
            SecondaryButton("Export Backup", onClick = { c.exportBackup() }, modifier = Modifier.fillMaxWidth())
            SecondaryButton(
                "Load Sample Inventory",
                onClick = {
                    c.repo.loadSample()
                    c.showToast("Sample inventory loaded")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { confirmErase = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Erase All Data", style = AppTheme.text.buttonSmall, color = AppTheme.colors.error)
            }
        }
    }
    if (confirmErase) {
        ConfirmDialog(
            title = "Erase everything?",
            message = "This deletes every item, the go-bag checklist and your evacuation plan from this phone. Export a backup first if you might need it.",
            confirmLabel = "Erase",
            destructive = true,
            onConfirm = {
                confirmErase = false
                c.repo.eraseAll()
                c.showToast("All data erased")
                onDismiss()
            },
            onDismiss = { confirmErase = false },
        )
    }
}
