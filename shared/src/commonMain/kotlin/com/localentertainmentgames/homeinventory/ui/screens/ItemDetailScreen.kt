package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import com.localentertainmentgames.homeinventory.platform.LocalAppPlatform
import com.localentertainmentgames.homeinventory.ui.components.AppTopBar
import com.localentertainmentgames.homeinventory.ui.components.CheckSquare
import com.localentertainmentgames.homeinventory.ui.components.LeCard
import com.localentertainmentgames.homeinventory.ui.components.Pill
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.components.SecondaryButton
import com.localentertainmentgames.homeinventory.ui.components.TagPill
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Radius

fun photoLabel(index: Int): String = when (index) {
    0 -> "Item"
    1 -> "Serial plate"
    2 -> "Receipt"
    else -> "Photo ${index + 1}"
}

@Composable
fun ItemDetailScreen(c: AppController, itemId: String) {
    val data by c.repo.data.collectAsState()
    val item = data.items.firstOrNull { it.id == itemId }
    if (item == null) {
        // Deleted elsewhere: leave the screen.
        androidx.compose.runtime.LaunchedEffect(Unit) { c.nav.removeItemScreens(itemId) }
        return
    }
    val hide = data.settings.hideValues
    var selected by remember(itemId) { mutableIntStateOf(0) }
    var sheetOpen by remember { mutableStateOf(false) }
    val platform = LocalAppPlatform.current
    val capture = platform.rememberPhotoCapture { path ->
        if (path != null) {
            c.repo.addPhoto(itemId, path)
            selected = item.photos.size
        }
    }
    val addPhoto = rememberCameraGate(onGranted = capture)

    ScreenScaffold(topBar = { AppTopBar("Item", onNavigate = { c.nav.pop() }) }) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val sel = selected.coerceIn(0, maxOf(0, item.photos.size - 1))
                PhotoHero(item, sel)
                if (item.photos.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.photos.forEachIndexed { i, path ->
                            Surface(
                                onClick = { selected = i },
                                shape = RoundedCornerShape(Radius.sm),
                                color = AppTheme.colors.surface2,
                                border = BorderStroke(2.dp, if (i == sel) AppTheme.colors.cta else Color.Transparent),
                                modifier = Modifier.size(76.dp, 60.dp).testTag("thumb-$i"),
                            ) {
                                Box(contentAlignment = Alignment.BottomCenter) {
                                    AsyncImage(model = path, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    Text(
                                        photoLabel(i),
                                        style = AppTheme.text.chipSmall,
                                        color = AppTheme.colors.fg1,
                                        modifier = Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(Radius.xs)).background(AppTheme.colors.bg).padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Text(item.name, style = MaterialTheme.typography.headlineMedium, color = AppTheme.colors.fg1, modifier = Modifier.testTag("item-name"))
                StatusBadges(item)
                FieldsCard(item, hide)
            }
            HorizontalDivider(color = AppTheme.colors.border)
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Edit", onClick = { c.nav.push(Screen.Edit(itemId)) }, icon = AppIcons.Edit, modifier = Modifier.weight(1f))
                SecondaryButton("Add Photo", onClick = addPhoto, icon = AppIcons.Camera, modifier = Modifier.weight(1.2f))
                PrimaryButton("Mark Status", onClick = { sheetOpen = true }, height = 48.dp, textStyle = AppTheme.text.buttonSmall, modifier = Modifier.weight(1.3f))
            }
        }
    }
    if (sheetOpen) StatusSheet(item, onToggle = { c.repo.toggleTag(itemId, it) }, onDone = { sheetOpen = false })
}

@Composable
private fun PhotoHero(item: Item, selected: Int) {
    Box(
        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(Radius.lg)).background(AppTheme.colors.surface2)
            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(Radius.lg)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.Image, null, tint = AppTheme.colors.fg4, modifier = Modifier.size(40.dp))
            Text(
                if (item.photos.isEmpty()) "NO PHOTO YET" else "PHOTO · ${photoLabel(selected).uppercase()}",
                style = AppTheme.text.monoTiny.copy(fontSize = 12.sp),
                color = AppTheme.colors.fg3,
            )
        }
        item.photos.getOrNull(selected)?.let { path ->
            AsyncImage(model = path, contentDescription = photoLabel(selected), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (item.photos.isNotEmpty()) {
            Text(
                "${selected + 1} / ${item.photos.size}",
                style = AppTheme.text.monoSmall.copy(fontSize = 12.sp),
                color = AppTheme.colors.bg,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(RoundedCornerShape(50)).background(AppTheme.colors.fg1)
                    .padding(horizontal = 10.dp, vertical = 6.dp).testTag("photo-pos"),
            )
        }
    }
}

private val badgeOrder = listOf(ItemTag.Insured, ItemTag.Lost, ItemTag.Claimed, ItemTag.GoBag, ItemTag.Irreplaceable)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBadges(item: Item) {
    FlowRow(Modifier.heightIn(min = 30.dp).testTag("status-badges"), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val tags = badgeOrder.filter { it in item.tags }
        if (tags.isEmpty()) {
            Pill("no status", background = AppTheme.colors.surface2, foreground = AppTheme.colors.fg3)
        } else {
            tags.forEach { TagPill(it, large = true) }
        }
    }
}

private data class Field(val label: String, val value: String, val full: Boolean, val mono: Boolean)

@Composable
private fun FieldsCard(item: Item, hide: Boolean) {
    val fields = buildList {
        add(Field("Category", item.category.ifBlank { "—" }, true, false))
        add(Field("Location", Format.location(item.location), true, false))
        add(Field("Quantity", item.quantity.toString(), false, true))
        add(Field("Condition", item.condition.ifBlank { "—" }, false, false))
        add(Field("Purchase date", item.purchaseDate?.let(Format::date) ?: "—", false, false))
        add(Field("Price", item.priceCents?.let { Format.money(it, hide) } ?: "—", false, true))
        add(Field("Serial / IMEI", item.serial.ifBlank { "—" }, true, true))
        if (item.barcode.isNotBlank()) add(Field("Barcode", item.barcode, true, true))
        if (item.expiresOn != null) add(Field("Expires", Format.date(item.expiresOn), true, false))
        if (item.notes.isNotBlank()) add(Field("Notes", item.notes, true, false))
    }
    // Pair half-width fields into rows of two.
    val rows = mutableListOf<List<Field>>()
    var pending: Field? = null
    for (f in fields) {
        if (f.full) {
            pending?.let { rows += listOf(it); pending = null }
            rows += listOf(f)
        } else if (pending == null) {
            pending = f
        } else {
            rows += listOf(pending!!, f)
            pending = null
        }
    }
    pending?.let { rows += listOf(it) }

    LeCard(modifier = Modifier.fillMaxWidth().testTag("fields-card"), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        rows.forEachIndexed { i, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { f ->
                    Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text(f.label.uppercase(), style = AppTheme.text.overline.copy(fontSize = 11.sp), color = AppTheme.colors.fg3)
                        Text(
                            f.value,
                            style = if (f.mono) AppTheme.text.monoMedium.copy(fontSize = 15.sp) else TextStyle(fontFamily = AppTheme.fonts.ui, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
                            color = AppTheme.colors.fg1,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
            if (i < rows.lastIndex) HorizontalDivider(color = AppTheme.colors.border)
        }
    }
}

/** ModalBottomSheet, 32 dp top corners: toggles insured / lost / claimed / go-bag. */
@Composable
private fun StatusSheet(item: Item, onToggle: (ItemTag) -> Unit, onDone: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = state,
        shape = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl),
        containerColor = AppTheme.colors.bg,
        modifier = Modifier.testTag("status-sheet"),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            Text("Mark status", style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.fg1, modifier = Modifier.padding(bottom = 8.dp))
            ItemTag.statusTags.forEach { tag ->
                val on = tag in item.tags
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(value = on, role = Role.Checkbox) { onToggle(tag) }
                        .padding(horizontal = 4.dp)
                        .testTag("status-${tag.label}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CheckSquare(on, AppTheme.colors.cta, AppTheme.colors.borderStrong)
                    Text(tag.label, style = AppTheme.text.bodyLgBold, color = AppTheme.colors.fg1, modifier = Modifier.weight(1f))
                    Text(tag.hint, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
                }
                HorizontalDivider(color = AppTheme.colors.border)
            }
            PrimaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    }
}
