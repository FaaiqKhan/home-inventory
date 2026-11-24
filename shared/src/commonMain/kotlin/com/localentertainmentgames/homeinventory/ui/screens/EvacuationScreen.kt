package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.model.Contact
import com.localentertainmentgames.homeinventory.model.EvacuationPlan
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.GoBagEntry
import com.localentertainmentgames.homeinventory.platform.LocalAppPlatform
import com.localentertainmentgames.homeinventory.ui.components.CheckSquare
import com.localentertainmentgames.homeinventory.ui.components.ConfirmDialog
import com.localentertainmentgames.homeinventory.ui.components.IconTile
import com.localentertainmentgames.homeinventory.ui.components.LabeledTextField
import com.localentertainmentgames.homeinventory.ui.components.LinkButton
import com.localentertainmentgames.homeinventory.ui.components.Overline
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Elevation
import com.localentertainmentgames.homeinventory.ui.theme.EvacuationTheme
import com.localentertainmentgames.homeinventory.ui.theme.LeTokens
import com.localentertainmentgames.homeinventory.ui.theme.PillShape
import com.localentertainmentgames.homeinventory.ui.theme.Radius

/** Screen 1f: always dark, high contrast, works offline. */
@Composable
fun EvacuationScreen(c: AppController) = EvacuationTheme {
    val data by c.repo.data.collectAsState()
    val platform = LocalAppPlatform.current
    var editPlan by remember { mutableStateOf(false) }
    var addEntry by remember { mutableStateOf(false) }
    var removeEntry by remember { mutableStateOf<GoBagEntry?>(null) }
    val packed = data.goBag.count { it.packed }
    val since = data.evacuatingSinceMillis

    ScreenScaffold(
        topBar = {
            Row(Modifier.height(56.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline("Evacuation mode", color = AppTheme.colors.accent)
                    Text("Works offline · all data on this phone", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, modifier = Modifier.padding(top = 4.dp))
                }
                IconButton(onClick = { if (!c.nav.pop()) c.nav.selectTab(com.localentertainmentgames.homeinventory.ui.navigation.Tab.Home) }, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcons.Close, contentDescription = "Close evacuation mode", tint = AppTheme.colors.fg1, modifier = Modifier.size(28.dp))
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(horizontal = 16.dp).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanCard(data.plan, onEdit = { editPlan = true }, onCall = { platform.dial(it.phone) })
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Go-bag", style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.fg1, modifier = Modifier.weight(1f))
                    Text(
                        buildAnnotatedString {
                            append(packed.toString())
                            withStyle(SpanStyle(color = AppTheme.colors.fg3)) { append(" / ${data.goBag.size}") }
                        },
                        style = AppTheme.text.monoLarge.copy(fontSize = 18.sp),
                        color = AppTheme.colors.fg1,
                        modifier = Modifier.testTag("packed-count"),
                    )
                }
                LazyColumn(Modifier.weight(1f).testTag("gobag-list"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)) {
                    items(data.goBag, key = { it.id }) { entry ->
                        BagRow(entry, onToggle = { c.repo.toggleGoBag(entry.id) }, onLongPress = { removeEntry = entry })
                    }
                    item { LinkButton("+ Add to go-bag", onClick = { addEntry = true }) }
                }
            }
            HorizontalDivider(color = AppTheme.colors.borderStrong)
            val evacuating = since != null
            Button(
                onClick = { c.repo.toggleEvacuating() },
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (evacuating) AppTheme.colors.success else AppTheme.colors.cta,
                    contentColor = if (evacuating) LeTokens.Dark1 else androidx.compose.ui.graphics.Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = Elevation.cta),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp).height(76.dp).testTag("evacuate"),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (evacuating) "Evacuating" else "I'm Evacuating", style = AppTheme.text.button.copy(fontSize = 24.sp, lineHeight = 26.sp))
                    Text(
                        if (since != null) "Since ${Format.time(since, c.timeZone)} · tap to undo" else "Tap to log time and status",
                        style = AppTheme.text.monoSmall.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    )
                }
            }
        }
    }

    if (editPlan) PlanDialog(data.plan, onSave = { mp, contacts -> c.repo.setPlan(mp, contacts); editPlan = false }, onDismiss = { editPlan = false })
    if (addEntry) AddEntryDialog(onAdd = { c.repo.addGoBagEntry(it); addEntry = false }, onDismiss = { addEntry = false })
    removeEntry?.let { e ->
        ConfirmDialog(
            title = "Remove from go-bag?",
            message = "\"${e.name}\" leaves the checklist.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { c.repo.removeGoBagEntry(e.id); removeEntry = null },
            onDismiss = { removeEntry = null },
        )
    }
}

@Composable
private fun PlanCard(plan: EvacuationPlan, onEdit: () -> Unit, onCall: (Contact) -> Unit) {
    Surface(
        shape = RoundedCornerShape(Radius.lg),
        color = AppTheme.colors.surface,
        border = BorderStroke(2.dp, AppTheme.colors.borderStrong),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                IconTile(AppIcons.Pin, AppTheme.colors.accent, LeTokens.Fg1, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("MEETING POINT", style = AppTheme.text.overline.copy(fontSize = 11.sp), color = AppTheme.colors.fg3)
                    Text(
                        plan.meetingPoint.ifBlank { "Not set yet" },
                        style = AppTheme.text.bodyLgBold.copy(fontSize = 18.sp, lineHeight = 23.sp),
                        color = if (plan.meetingPoint.isBlank()) AppTheme.colors.fg3 else AppTheme.colors.fg1,
                        modifier = Modifier.padding(top = 4.dp).testTag("meeting-point"),
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(AppIcons.Edit, contentDescription = "Edit plan", tint = AppTheme.colors.fg3, modifier = Modifier.size(20.dp))
                }
            }
            if (plan.contacts.isEmpty()) {
                LinkButton("Add emergency contacts", onClick = onEdit)
            } else {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    plan.contacts.take(2).forEach { contact ->
                        Surface(
                            onClick = { onCall(contact) },
                            enabled = contact.phone.isNotBlank(),
                            shape = RoundedCornerShape(Radius.md),
                            color = AppTheme.colors.surface2,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("call-${contact.name}"),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(AppTheme.colors.success), contentAlignment = Alignment.Center) {
                                    Icon(AppIcons.Phone, contentDescription = "Call ${contact.name}", tint = LeTokens.Dark1, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(contact.name, style = AppTheme.text.bodyLgBold.copy(fontSize = 15.sp), color = AppTheme.colors.fg1, maxLines = 1)
                                    if (contact.role.isNotBlank()) Text(contact.role, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BagRow(entry: GoBagEntry, onToggle: () -> Unit, onLongPress: () -> Unit) {
    val d = entry.packed
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = AppTheme.colors.surface,
        border = BorderStroke(2.dp, if (d) AppTheme.colors.border else AppTheme.colors.borderStrong),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(Radius.md))
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress, role = Role.Checkbox)
            .semantics {
                toggleableState = ToggleableState(d)
                stateDescription = if (d) "Packed" else "Not packed"
            }
            .testTag("bag-${entry.name}"),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CheckSquare(d, AppTheme.colors.success, AppTheme.colors.fg3, size = 34.dp, borderWidth = 3.dp, tickColor = LeTokens.Dark1)
            Text(
                entry.name,
                style = AppTheme.text.bodyLgBold.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = if (d) AppTheme.colors.fg3 else AppTheme.colors.fg1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlanDialog(plan: EvacuationPlan, onSave: (String, List<Contact>) -> Unit, onDismiss: () -> Unit) {
    var meeting by remember { mutableStateOf(plan.meetingPoint) }
    val contacts = remember { mutableStateListOf<Contact>().apply { addAll(plan.contacts); while (size < 2) add(Contact("")) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.xl),
        containerColor = AppTheme.colors.bg,
        modifier = Modifier.testTag("plan-dialog"),
        title = { Text("Evacuation plan", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField("Meeting point", meeting, { meeting = it }, placeholder = "Oak Street Library, north car park", singleLine = false)
                contacts.forEachIndexed { i, ct ->
                    Overline("Contact ${i + 1}", modifier = Modifier.padding(top = 4.dp))
                    LabeledTextField("Name", ct.name, { contacts[i] = ct.copy(name = it) }, tag = "contact-name-$i")
                    LabeledTextField("Relationship or address", ct.role, { contacts[i] = ct.copy(role = it) }, tag = "contact-role-$i")
                    LabeledTextField("Phone", ct.phone, { contacts[i] = ct.copy(phone = it) }, keyboardType = KeyboardType.Phone, tag = "contact-phone-$i")
                }
            }
        },
        confirmButton = { PrimaryButton("Save", onClick = { onSave(meeting, contacts.toList()) }, height = 48.dp, textStyle = AppTheme.text.buttonSmall) },
        dismissButton = { LinkButton("Cancel", onClick = onDismiss) },
    )
}

@Composable
private fun AddEntryDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.xl),
        containerColor = AppTheme.colors.bg,
        title = { Text("Add to go-bag", style = MaterialTheme.typography.headlineSmall) },
        text = { LabeledTextField("Item", name, { name = it }, placeholder = "Spare keys", tag = "bag-entry") },
        confirmButton = { PrimaryButton("Add", onClick = { onAdd(name) }, enabled = name.isNotBlank(), height = 48.dp, textStyle = AppTheme.text.buttonSmall) },
        dismissButton = { LinkButton("Cancel", onClick = onDismiss) },
    )
}
