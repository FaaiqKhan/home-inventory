package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.domain.ItemFilter
import com.localentertainmentgames.homeinventory.domain.categories
import com.localentertainmentgames.homeinventory.domain.filterBy
import com.localentertainmentgames.homeinventory.domain.rooms
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.model.ItemTag
import com.localentertainmentgames.homeinventory.ui.components.AppBottomBar
import com.localentertainmentgames.homeinventory.ui.components.DropdownChip
import com.localentertainmentgames.homeinventory.ui.components.IconTile
import com.localentertainmentgames.homeinventory.ui.components.PhotoThumb
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.components.SearchField
import com.localentertainmentgames.homeinventory.ui.components.SecondaryButton
import com.localentertainmentgames.homeinventory.ui.components.TagFilterChip
import com.localentertainmentgames.homeinventory.ui.components.TagPill
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.navigation.Tab
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme

@Composable
fun ItemListScreen(c: AppController, expiringOnly: Boolean) {
    val data by c.repo.data.collectAsState()
    var filter by c::itemFilter
    val today = c.today()
    val all = data.items
    val filtered = remember(all, filter, today) { all.filterBy(filter, today).sortedBy { it.name.lowercase() } }

    ScreenScaffold(bottomBar = { AppBottomBar(Tab.Items, c.nav::selectTab) }) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Items", style = MaterialTheme.typography.headlineMedium, color = AppTheme.colors.fg1, modifier = Modifier.weight(1f))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontFamily = AppTheme.fonts.mono, fontWeight = FontWeight.Bold, color = AppTheme.colors.fg1)) {
                                append(filtered.size.toString())
                            }
                            append(" " + Format.plural(filtered.size, "item"))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.fg3,
                        modifier = Modifier.testTag("result-count"),
                    )
                }
                SearchField(filter.query, { filter = filter.copy(query = it) }, "Search name, serial, room, tag")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DropdownChip("Room", filter.room, all.rooms(), "All rooms", { filter = filter.copy(room = it) })
                    DropdownChip("Category", filter.category, all.categories(), "All categories", { filter = filter.copy(category = it) })
                    ItemTag.filterTags.forEach { tag ->
                        TagFilterChip(tag.label, tag in filter.tags, { filter = filter.toggle(tag) })
                    }
                    TagFilterChip("expiring", filter.expiringOnly, { filter = filter.copy(expiringOnly = !filter.expiringOnly) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    all.isEmpty() -> FirstRunEmpty(onScan = { c.nav.push(Screen.Add()) }, onManual = { c.nav.push(Screen.Add(manual = true)) })
                    filtered.isEmpty() -> NoMatches(onClear = { filter = ItemFilter() })
                    else -> LazyColumn(Modifier.fillMaxSize().testTag("item-list"), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp)) {
                        items(filtered, key = { it.id }) { item -> ItemRow(item) { c.nav.push(Screen.Detail(item.id)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhotoThumb(item.photos.firstOrNull(), 56.dp)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = AppTheme.text.bodyLgBold, color = AppTheme.colors.fg1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Format.location(item.location), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.tags.isNotEmpty()) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemTag.entries.filter { it in item.tags }.forEach { TagPill(it) }
                    }
                }
            }
            Text("×${item.quantity}", style = AppTheme.text.monoMedium, color = AppTheme.colors.fg1)
        }
        HorizontalDivider(color = AppTheme.colors.border)
    }
}

@Composable
private fun FirstRunEmpty(onScan: () -> Unit, onManual: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 36.dp).padding(bottom = 40.dp).testTag("empty-first-run"),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(AppIcons.Barcode, AppTheme.colors.brand050, AppTheme.colors.brand, size = 96.dp, iconSize = 48.dp, radius = 28.dp)
        Text("Nothing here yet", style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.fg1)
        Text(
            "Scan a barcode or photograph your first item. Everything is saved on this phone, no account needed.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.fg3,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 290.dp),
        )
        PrimaryButton("Scan First Item", onClick = onScan, icon = AppIcons.Barcode, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        SecondaryButton("Add Manually", onClick = onManual, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NoMatches(onClear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 60.dp).testTag("empty-no-matches"),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(AppIcons.Search, null, tint = AppTheme.colors.fg4, modifier = Modifier.size(40.dp))
        Text("No matches", style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.fg1)
        Text("Nothing fits that search and filter combination.", style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.fg3, textAlign = TextAlign.Center)
        SecondaryButton("Clear Filters", onClick = onClear, modifier = Modifier.padding(top = 4.dp))
    }
}
