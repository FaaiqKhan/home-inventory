package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.domain.ItemDraft
import com.localentertainmentgames.homeinventory.model.Categories
import com.localentertainmentgames.homeinventory.model.Conditions
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.ItemTag
import com.localentertainmentgames.homeinventory.ui.components.FieldLabel
import com.localentertainmentgames.homeinventory.ui.components.LabeledTextField
import com.localentertainmentgames.homeinventory.ui.components.LinkButton
import com.localentertainmentgames.homeinventory.ui.components.PhotoThumb
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.SelectField
import com.localentertainmentgames.homeinventory.ui.components.TagFilterChip
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Radius
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Name, location, category, quantity + price, photos; then optional details. Used by Add and Edit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemFormFields(
    draft: ItemDraft,
    onChange: (ItemDraft) -> Unit,
    showErrors: Boolean,
    rooms: List<String>,
    onAddPhoto: () -> Unit,
    nameHelper: String?,
    detailsInitiallyOpen: Boolean,
) {
    var locationOpen by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(detailsInitiallyOpen) }

    LabeledTextField(
        label = "Name",
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        placeholder = "What is it?",
        helper = nameHelper,
        error = if (showErrors) draft.nameError else null,
    )
    SelectField(
        label = "Location",
        value = Format.location(draft.location),
        placeholder = "Choose a location",
        onClick = { locationOpen = true },
        leading = AppIcons.Pin,
    )
    OptionField("Category", draft.category.ifBlank { null }, "Choose a category", Categories.all) { onChange(draft.copy(category = it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Quantity")
            QuantityStepper(draft.quantity, onDown = { onChange(draft.decrement()) }, onUp = { onChange(draft.increment()) })
        }
        LabeledTextField(
            label = "Price",
            value = draft.priceText,
            onValueChange = { onChange(draft.copy(priceText = it)) },
            placeholder = "$0.00",
            error = if (draft.priceError != null) draft.priceError else null,
            keyboardType = KeyboardType.Decimal,
            textStyle = AppTheme.text.monoMedium,
            modifier = Modifier.weight(1f),
        )
    }
    PhotoStrip(draft.photos, onAddPhoto, onRemove = { p -> onChange(draft.copy(photos = draft.photos - p)) })

    Surface(
        onClick = { detailsOpen = !detailsOpen },
        color = AppTheme.colors.bg,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("more-details"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("More details", style = AppTheme.text.bodyMdBold, color = AppTheme.colors.cta, modifier = Modifier.weight(1f))
            Text("Serial, dates, condition, tags", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
            Icon(AppIcons.ArrowDropDown, null, tint = AppTheme.colors.fg3)
        }
    }
    if (detailsOpen) {
        LabeledTextField(
            label = "Serial / IMEI",
            value = draft.serial,
            onValueChange = { onChange(draft.copy(serial = it)) },
            placeholder = "From the serial plate",
            textStyle = AppTheme.text.monoMedium,
            tag = "Serial",
        )
        LabeledTextField(
            label = "Barcode",
            value = draft.barcode,
            onValueChange = { onChange(draft.copy(barcode = it)) },
            placeholder = "Scanned or typed",
            textStyle = AppTheme.text.monoMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField("Purchase date", draft.purchaseDate, { onChange(draft.copy(purchaseDate = it)) }, Modifier.weight(1f))
            DateField("Expires", draft.expiresOn, { onChange(draft.copy(expiresOn = it)) }, Modifier.weight(1f))
        }
        OptionField("Condition", draft.condition.ifBlank { null }, "How is it?", Conditions.all) { onChange(draft.copy(condition = it)) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Tags")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemTag.entries.forEach { tag -> TagFilterChip(tag.label, tag in draft.tags, { onChange(draft.toggle(tag)) }) }
            }
        }
        LabeledTextField(
            label = "Notes",
            value = draft.notes,
            onValueChange = { onChange(draft.copy(notes = it)) },
            placeholder = "Model, warranty, where the receipt is…",
            singleLine = false,
        )
    }

    if (locationOpen) {
        LocationDialog(
            current = draft.location,
            rooms = rooms,
            onDone = { onChange(draft.copy(location = it)); locationOpen = false },
            onDismiss = { locationOpen = false },
        )
    }
}

@Composable
fun QuantityStepper(quantity: Int, onDown: () -> Unit, onUp: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = AppTheme.colors.surface,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDown, modifier = Modifier.size(44.dp)) { Icon(AppIcons.Minus, contentDescription = "Less", tint = AppTheme.colors.fg1) }
            Text(quantity.toString(), style = AppTheme.text.monoLarge, color = AppTheme.colors.fg1, modifier = Modifier.weight(1f).testTag("qty"), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = onUp, modifier = Modifier.size(44.dp)) { Icon(AppIcons.Add, contentDescription = "More", tint = AppTheme.colors.fg1) }
        }
    }
}

@Composable
private fun PhotoStrip(photos: List<String>, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        photos.forEach { p ->
            Box {
                PhotoThumb(p, 64.dp)
                Surface(
                    onClick = { onRemove(p) },
                    shape = RoundedCornerShape(50),
                    color = AppTheme.colors.fg1,
                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp),
                ) { Icon(AppIcons.Close, contentDescription = "Remove photo", tint = AppTheme.colors.bg, modifier = Modifier.padding(3.dp)) }
            }
        }
        val dash = AppTheme.colors.borderStrong
        Box(
            Modifier
                .size(64.dp)
                .drawBehind {
                    drawRoundRect(
                        color = dash,
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }
                .clickable(onClick = onAdd)
                .testTag("add-photo"),
            contentAlignment = Alignment.Center,
        ) { Icon(AppIcons.Camera, contentDescription = "Add photo", tint = AppTheme.colors.fg3, modifier = Modifier.size(26.dp)) }
        if (photos.isEmpty()) {
            Text("Add a photo of the item, serial plate or receipt", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.fg3, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun OptionField(label: String, value: String?, placeholder: String, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SelectField(label, value, placeholder, onClick = { open = true }, trailing = AppIcons.ArrowDropDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); open = false }, modifier = Modifier.testTag("option-$opt"))
            }
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
private fun Long.toUtcDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    SelectField(label, value?.let(Format::date), "Not set", onClick = { open = true }, modifier = modifier, trailing = AppIcons.Calendar)
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                LinkButton("OK", onClick = {
                    onChange(state.selectedDateMillis?.toUtcDate())
                    open = false
                })
            },
            dismissButton = {
                LinkButton("Clear", onClick = {
                    onChange(null)
                    open = false
                })
            },
        ) { DatePicker(state = state) }
    }
}

/** Picks Home → Room → Spot. Rooms already used are offered as chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationDialog(current: List<String>, rooms: List<String>, onDone: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var room by remember { mutableStateOf(current.getOrNull(1).orEmpty()) }
    var spot by remember { mutableStateOf(current.drop(2).joinToString(" ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.xl),
        containerColor = AppTheme.colors.bg,
        modifier = Modifier.testTag("location-dialog"),
        title = { Text("Location", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField("Room", room, { room = it }, placeholder = "Garage")
                if (rooms.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rooms.forEach { r -> TagFilterChip(r, r == room, { room = r }) }
                    }
                }
                LabeledTextField("Spot", spot, { spot = it }, placeholder = "Shelf B")
                Text(
                    Format.location(ItemDraft.locationOf(room, spot)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.fg3,
                )
            }
        },
        confirmButton = { PrimaryButton("Done", onClick = { onDone(ItemDraft.locationOf(room, spot)) }, height = 48.dp, textStyle = AppTheme.text.buttonSmall) },
        dismissButton = { LinkButton("Cancel", onClick = onDismiss) },
    )
}

