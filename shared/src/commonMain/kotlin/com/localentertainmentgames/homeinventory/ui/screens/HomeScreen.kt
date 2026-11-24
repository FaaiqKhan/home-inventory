package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.domain.DashboardSummary
import com.localentertainmentgames.homeinventory.domain.Expiry
import com.localentertainmentgames.homeinventory.model.DashboardLayout
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.ui.components.AppBottomBar
import com.localentertainmentgames.homeinventory.ui.components.IconTile
import com.localentertainmentgames.homeinventory.ui.components.LeCard
import com.localentertainmentgames.homeinventory.ui.components.LinkButton
import com.localentertainmentgames.homeinventory.ui.components.OfflineBadge
import com.localentertainmentgames.homeinventory.ui.components.Overline
import com.localentertainmentgames.homeinventory.ui.components.PackProgress
import com.localentertainmentgames.homeinventory.ui.components.Pill
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.components.SecondaryButton
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.navigation.Tab
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.DarkExtendedColors
import com.localentertainmentgames.homeinventory.ui.theme.Elevation
import com.localentertainmentgames.homeinventory.ui.theme.LeTokens
import com.localentertainmentgames.homeinventory.ui.theme.Radius

/** Actions every home layout offers. */
private class HomeActions(
    val openEvacuation: () -> Unit,
    val addItem: () -> Unit,
    val exportBackup: () -> Unit,
    val openExpiring: () -> Unit,
    val openReport: () -> Unit,
)

@Composable
fun HomeScreen(c: AppController) {
    val data by c.repo.data.collectAsState()
    val summary = remember(data) { DashboardSummary.from(data, c.today()) }
    val hide = data.settings.hideValues
    var settingsOpen by remember { mutableStateOf(false) }
    val actions = HomeActions(
        openEvacuation = { c.nav.push(Screen.Evacuation) },
        addItem = { c.nav.push(Screen.Add()) },
        exportBackup = { c.exportBackup() },
        openExpiring = c::openExpiring,
        openReport = { c.nav.push(Screen.Report) },
    )

    ScreenScaffold(bottomBar = { AppBottomBar(Tab.Home, c.nav::selectTab) }) {
        BoxWithConstraints {
            val minH = maxHeight
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(
                    Modifier.heightIn(min = minH).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeHeader(Format.lastBackup(data.settings.lastBackupAtMillis, c.nowMillis()), onSettings = { settingsOpen = true })
                    when (data.settings.dashboardLayout) {
                        DashboardLayout.ValueFirst -> ValueFirst(summary, hide, actions)
                        DashboardLayout.ReadinessFirst -> ReadinessFirst(summary, hide, actions)
                        DashboardLayout.ActionFirst -> ActionFirst(summary, hide, actions)
                    }
                }
            }
        }
    }
    if (settingsOpen) SettingsSheet(c, data.settings, onDismiss = { settingsOpen = false })
}

@Composable
private fun HomeHeader(subtitle: String, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Home Inventory", style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.fg1, maxLines = 1, softWrap = false)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, modifier = Modifier.testTag("last-backup"))
        }
        OfflineBadge()
        IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
            Icon(AppIcons.Settings, contentDescription = "Settings", tint = AppTheme.colors.fg3, modifier = Modifier.size(22.dp))
        }
    }
}

// 1a · Value first

@Composable
private fun ColumnScope.ValueFirst(s: DashboardSummary, hide: Boolean, a: HomeActions) {
    // Hero card with the brand gradient and the sparkle stars (hero 1a only).
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).background(AppTheme.colors.brandGradient)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Icon(AppIcons.Star, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(end = 0.dp).size(64.dp).alpha(0.16f))
        Icon(AppIcons.Star, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 62.dp).size(28.dp).alpha(0.12f))
        Column {
            Overline("Total inventory value", color = LeTokens.Primary050)
            Text(
                Format.money(s.totalValueCents, hide),
                style = AppTheme.text.monoHero,
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp).testTag("total-value"),
                maxLines = 1,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CountLabel(s.itemCount, Format.plural(s.itemCount, "item"))
                CountLabel(s.withPhotos, "with photos")
            }
        }
    }
    LeCard(onClick = a.openEvacuation, modifier = Modifier.fillMaxWidth().testTag("gobag-card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(AppIcons.Bag, AppTheme.colors.brand050, AppTheme.colors.brand)
            Column(Modifier.weight(1f)) {
                Text("Go-Bag Status", style = AppTheme.text.bodyMdBold, color = AppTheme.colors.fg1)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = AppTheme.fonts.mono, fontWeight = FontWeight.Bold, color = AppTheme.colors.fg1)) {
                            append("${s.packed} of ${s.bagSize}")
                        }
                        append(" packed · ${s.missingCount} missing")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.fg3,
                )
            }
            Icon(AppIcons.ChevronRight, null, tint = AppTheme.colors.fg4)
        }
        PackProgress(s.packedFraction, Modifier.padding(top = 10.dp))
    }
    ExpiringChipsCard(s, a.openExpiring)
    Spacer(Modifier.weight(1f))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton("Add Item", onClick = a.addItem, icon = AppIcons.Add, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Evacuation Mode", onClick = a.openEvacuation, modifier = Modifier.weight(1f))
            SecondaryButton("Export Backup", onClick = a.exportBackup, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CountLabel(n: Int, label: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = AppTheme.fonts.mono, fontWeight = FontWeight.Bold, color = Color.White)) { append(n.toString()) }
            append(" $label")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = LeTokens.Primary050,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpiringChipsCard(s: DashboardSummary, onClick: () -> Unit) {
    WarningCard(onClick, Modifier.testTag("expiring-card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.Clock, null, tint = AppTheme.colors.warningText, modifier = Modifier.size(20.dp))
            Text("Expiring soon", style = AppTheme.text.bodyMdBold, color = AppTheme.colors.fg1, modifier = Modifier.weight(1f))
            Text("Next ${Expiry.WINDOW_DAYS} days", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
        }
        val groups = s.expiringGroups
        if (groups.isEmpty()) {
            Text("Nothing expires soon.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.fg3, modifier = Modifier.padding(top = 6.dp))
        } else {
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    Pill(g.label, border = AppTheme.colors.borderStrong, dot = if (g.isUrgent) AppTheme.colors.error else AppTheme.colors.warning)
                }
            }
        }
    }
}

@Composable
private fun WarningCard(onClick: () -> Unit, modifier: Modifier = Modifier, radius: androidx.compose.ui.unit.Dp = Radius.lg, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(radius),
        color = AppTheme.colors.warning050,
        border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
        modifier = modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() } }
}

// 1b · Readiness first

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.ReadinessFirst(s: DashboardSummary, hide: Boolean, a: HomeActions) {
    LeCard(modifier = Modifier.fillMaxWidth().testTag("gobag-card"), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.Bag, null, tint = AppTheme.colors.brand, modifier = Modifier.size(18.dp))
            Overline("Go-Bag Status")
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.packed.toString(), style = AppTheme.text.monoHero.copy(fontSize = 52.sp, lineHeight = 52.sp), color = AppTheme.colors.fg1)
            Text("/ ${s.bagSize}", style = AppTheme.text.monoBig.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.fg4, modifier = Modifier.padding(bottom = 4.dp))
            Text("packed", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.fg3, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        }
        PackProgress(s.packedFraction, Modifier.padding(top = 10.dp), height = 10.dp)
        if (s.missing.isNotEmpty()) {
            Text("Still to pack", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, modifier = Modifier.padding(top = 8.dp))
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.missing.take(3).forEach { Pill(it, background = AppTheme.colors.error050, border = AppTheme.colors.borderStrong) }
                if (s.missing.size > 3) Pill("+${s.missing.size - 3} more", border = AppTheme.colors.borderStrong)
            }
        } else {
            Text("Everything is packed.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.successText, modifier = Modifier.padding(top = 8.dp))
        }
        PrimaryButton("Open Evacuation Mode", onClick = a.openEvacuation, height = 48.dp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
    WarningCard(onClick = a.openExpiring, modifier = Modifier.testTag("expiring-card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.Clock, null, tint = AppTheme.colors.warningText, modifier = Modifier.size(20.dp))
            Text("Expiring soon", style = AppTheme.text.bodyMdBold, color = AppTheme.colors.fg1, modifier = Modifier.weight(1f))
            LinkButton("View all", onClick = a.openExpiring)
        }
        if (s.expiring.isEmpty()) {
            Text("Nothing expires in the next ${Expiry.WINDOW_DAYS} days.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.fg3)
        }
        s.expiring.take(3).forEach { e ->
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    val name = if (e.item.quantity > 1) "${e.item.name} ×${e.item.quantity}" else e.item.name
                    Text(name, style = AppTheme.text.bodyMdBold, color = AppTheme.colors.fg1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(e.where, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
                }
                Pill(
                    e.label,
                    background = if (e.isUrgent) AppTheme.colors.error050 else AppTheme.colors.bg,
                    border = AppTheme.colors.borderStrong,
                    style = AppTheme.text.monoSmall.copy(fontSize = 12.sp),
                    horizontal = 10.dp,
                    vertical = 7.dp,
                )
            }
        }
    }
    Surface(
        onClick = a.openReport,
        shape = RoundedCornerShape(Radius.lg),
        color = AppTheme.colors.surface,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Total inventory value · ${s.itemCount} ${Format.plural(s.itemCount, "item")}", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
                Text(Format.money(s.totalValueCents, hide), style = AppTheme.text.monoLarge.copy(fontSize = 22.sp), color = AppTheme.colors.fg1, modifier = Modifier.testTag("total-value"))
            }
            Icon(AppIcons.ChevronRight, null, tint = AppTheme.colors.fg4)
        }
    }
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("Add Item", onClick = a.addItem, icon = AppIcons.Add, modifier = Modifier.weight(1f))
        SecondaryButton("Export Backup", onClick = a.exportBackup, icon = AppIcons.Download, modifier = Modifier.weight(1f))
    }
}

// 1c · Action first

@Composable
private fun ColumnScope.ActionFirst(s: DashboardSummary, hide: Boolean, a: HomeActions) {
    val dark = DarkExtendedColors
    Surface(
        onClick = a.openEvacuation,
        shape = RoundedCornerShape(Radius.lg),
        color = dark.bg,
        shadowElevation = Elevation.e2,
        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).testTag("gobag-card"),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconTile(AppIcons.Warning, LeTokens.Accent, LeTokens.Fg1, size = 56.dp, iconSize = 32.dp, radius = Radius.md)
            Column(Modifier.weight(1f)) {
                Text("Evacuation Mode", style = MaterialTheme.typography.headlineSmall, color = dark.fg1)
                Text("${s.packed} of ${s.bagSize} go-bag items packed", style = MaterialTheme.typography.bodyMedium, color = dark.fg3)
            }
            Icon(AppIcons.ChevronRight, null, tint = dark.fg3, modifier = Modifier.size(28.dp))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionTile("Add Item", AppIcons.Add, AppTheme.colors.cta, Color.White, null, a.addItem, Modifier.weight(1f))
        ActionTile("Export Backup", AppIcons.Download, AppTheme.colors.cta050, AppTheme.colors.cta, AppTheme.colors.cta, a.exportBackup, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Total value", Format.money(s.totalValueCents, hide), Modifier.weight(1.5f).testTag("total-value-tile"), onClick = a.openReport)
        StatTile("Go-bag", "${s.packed}/${s.bagSize}", Modifier.weight(1f), onClick = a.openEvacuation)
        StatTile("Expiring", s.expiring.size.toString(), Modifier.weight(1f), onClick = a.openExpiring)
    }
    WarningCard(onClick = a.openExpiring, modifier = Modifier.testTag("expiring-card"), radius = Radius.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.Clock, null, tint = AppTheme.colors.warningText, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                val first = s.expiring.firstOrNull()
                val title = when {
                    first == null -> "Nothing expiring soon"
                    first.daysLeft < 0 -> "${first.item.name} has expired"
                    first.daysLeft == 0 -> "${first.item.name} expires today"
                    else -> "${first.item.name} expires in ${first.label}"
                }
                Text(title, style = AppTheme.text.bodyMdBold, color = AppTheme.colors.fg1)
                val sub = Expiry.restSummary(s.expiring) ?: "Checking the next ${Expiry.WINDOW_DAYS} days"
                Text(sub, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
            }
            Icon(AppIcons.ChevronRight, null, tint = AppTheme.colors.fg4, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, bg: Color, fg: Color, border: Color?, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.lg),
        color = bg,
        contentColor = fg,
        border = border?.let { BorderStroke(1.5.dp, it) },
        shadowElevation = if (border == null) Elevation.cta else 0.dp,
        modifier = modifier.heightIn(min = 96.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Text(label, style = AppTheme.text.button)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    LeCard(onClick = onClick, modifier = modifier, radius = Radius.md, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, maxLines = 1)
        Spacer(Modifier.width(2.dp))
        Text(value, style = AppTheme.text.monoLarge, color = AppTheme.colors.fg1, maxLines = 1)
    }
}
