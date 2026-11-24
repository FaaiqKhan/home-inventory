package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.domain.InsuranceReport
import com.localentertainmentgames.homeinventory.domain.ReportRow
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.ui.components.AppBottomBar
import com.localentertainmentgames.homeinventory.ui.components.AppTopBar
import com.localentertainmentgames.homeinventory.ui.components.LeCard
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.components.SecondaryButton
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.navigation.Tab
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Radius

@Composable
fun ReportScreen(c: AppController) {
    val data by c.repo.data.collectAsState()
    val hide = data.settings.hideValues
    val report = remember(data.items) { InsuranceReport.build(data.items, c.today()) }

    ScreenScaffold(
        topBar = {
            AppTopBar("Insurance Report", onNavigate = { c.nav.selectTab(Tab.Home) }, actions = {
                Text(report.dateLabel, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3, modifier = Modifier.padding(end = 12.dp))
            })
        },
        bottomBar = { AppBottomBar(Tab.Report, c.nav::selectTab) },
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LeCard(modifier = Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Row(Modifier.height(IntrinsicSize.Min).heightIn(min = 48.dp)) {
                        Stat("Total value", Format.money(report.totalValueCents, hide), Modifier.weight(1.6f).testTag("report-total"))
                        VerticalDivider(color = AppTheme.colors.border)
                        Stat("Items", report.itemCount.toString(), Modifier.weight(1f).padding(start = 14.dp))
                        VerticalDivider(color = AppTheme.colors.border)
                        Stat("Photos", (report.itemCount - report.missingPhotoCount).toString(), Modifier.weight(1f).padding(start = 14.dp))
                    }
                }
                if (report.missingPhotoCount > 0) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm)).background(AppTheme.colors.warning050)
                            .border(1.dp, AppTheme.colors.borderStrong, RoundedCornerShape(Radius.sm)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(AppIcons.Warning, null, tint = AppTheme.colors.warningText, modifier = Modifier.size(18.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("${report.missingPhotoCount} ${Format.plural(report.missingPhotoCount, "item")}")
                                }
                                append(" ${if (report.missingPhotoCount == 1) "has" else "have"} no photo. Insurers may ask for one.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.fg2,
                            modifier = Modifier.testTag("missing-photos"),
                        )
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.md)).border(1.dp, AppTheme.colors.border, RoundedCornerShape(Radius.md))) {
                    Row(
                        Modifier.fillMaxWidth().background(AppTheme.colors.surface).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HeaderCell("Photo", Modifier.width(40.dp))
                        HeaderCell("Item · Serial", Modifier.weight(1f))
                        HeaderCell("Bought", Modifier.width(62.dp))
                        HeaderCell("Value", Modifier.width(76.dp), TextAlign.End)
                    }
                    val preview = report.rows.take(InsuranceReport.PREVIEW_ROWS)
                    LazyColumn(Modifier.weight(1f, fill = false).testTag("report-table")) {
                        items(preview, key = { it.itemId }) { row ->
                            HorizontalDivider(color = AppTheme.colors.border)
                            ReportRowView(row, hide) { c.nav.push(Screen.Detail(row.itemId)) }
                        }
                    }
                    val more = report.rows.size - preview.size
                    HorizontalDivider(color = AppTheme.colors.border)
                    Text(
                        when {
                            report.rows.isEmpty() -> "No items yet. Add some to build the report."
                            more > 0 -> "+ $more more ${Format.plural(more, "item")} in the full PDF"
                            else -> "All items shown"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.fg3,
                        modifier = Modifier.fillMaxWidth().background(AppTheme.colors.surface).padding(horizontal = 12.dp, vertical = 10.dp).testTag("report-more"),
                    )
                }
            }
            Row(Modifier.padding(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Export PDF", onClick = { c.exportReport(share = false) }, icon = AppIcons.Download, modifier = Modifier.weight(1.4f))
                SecondaryButton("Share", onClick = { c.exportReport(share = true) }, icon = AppIcons.Share, height = 52.dp, textStyle = AppTheme.text.button, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
        Text(value, style = AppTheme.text.monoBig, color = AppTheme.colors.fg1, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(text.uppercase(), style = AppTheme.text.overline.copy(fontSize = 10.sp), color = AppTheme.colors.fg3, modifier = modifier, textAlign = align, maxLines = 1)
}

@Composable
private fun ReportRowView(row: ReportRow, hide: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val hasPhoto = row.photoPath != null
        val warn = AppTheme.colors.warningText
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (hasPhoto) AppTheme.colors.surface2 else AppTheme.colors.warning050)
                .then(
                    if (hasPhoto) {
                        Modifier.border(1.dp, AppTheme.colors.border, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.drawBehind {
                            drawRoundRect(warn, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))), cornerRadius = CornerRadius(8.dp.toPx()))
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (hasPhoto) "IMG" else "NONE", style = AppTheme.text.monoTiny.copy(fontSize = 9.sp), color = AppTheme.colors.fg4)
            if (hasPhoto) AsyncImage(model = row.photoPath, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f)) {
            Text(row.name, style = AppTheme.text.chip.copy(lineHeight = 17.sp), color = AppTheme.colors.fg1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.serial, style = AppTheme.text.monoTiny, color = AppTheme.colors.fg3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(row.bought, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg2, modifier = Modifier.width(62.dp))
        Text(Format.money(row.valueCents, hide), style = AppTheme.text.monoSmall, color = AppTheme.colors.fg1, modifier = Modifier.width(76.dp), textAlign = TextAlign.End, maxLines = 1)
    }
}
