package com.localentertainmentgames.homeinventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.localentertainmentgames.homeinventory.model.ItemTag
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Elevation
import com.localentertainmentgames.homeinventory.ui.theme.PillShape
import com.localentertainmentgames.homeinventory.ui.theme.Radius

/** .le-card: bg, 1 dp border, 20 dp radius, elevation 2. */
@Composable
fun LeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = AppTheme.colors.bg,
    border: Color = AppTheme.colors.border,
    radius: Dp = Radius.lg,
    elevation: Dp = Elevation.e1,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val inner: @Composable () -> Unit = { Column(Modifier.padding(padding), content = content) }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = BorderStroke(1.dp, border), shadowElevation = elevation, content = inner)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = BorderStroke(1.dp, border), shadowElevation = elevation, content = inner)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = AppTheme.colors.bg,
    foreground: Color = AppTheme.colors.fg1,
    border: Color? = null,
    dot: Color? = null,
    style: TextStyle = AppTheme.text.chip,
    horizontal: Dp = 12.dp,
    vertical: Dp = 8.dp,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, PillShape) else Modifier)
            .padding(horizontal = horizontal, vertical = vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(text, style = style, color = foreground, maxLines = 1)
    }
}

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) = Pill(
    text = "Offline · on device",
    modifier = modifier,
    background = AppTheme.colors.success050,
    foreground = AppTheme.colors.successText,
    dot = AppTheme.colors.success,
    style = AppTheme.text.chip.copy(fontSize = 12.sp),
    horizontal = 10.dp,
)

data class TagColors(val bg: Color, val fg: Color)

@Composable
fun tagColors(tag: ItemTag): TagColors {
    val c = AppTheme.colors
    return when (tag) {
        ItemTag.GoBag -> TagColors(c.brand050, if (c.isDark) c.brand300 else c.brand)
        ItemTag.Irreplaceable -> TagColors(c.warning050, c.warningText)
        ItemTag.Insured -> TagColors(c.success050, c.successText)
        ItemTag.Lost -> TagColors(c.error050, c.error)
        ItemTag.Claimed -> TagColors(c.warning050, c.warningText)
    }
}

@Composable
fun TagPill(tag: ItemTag, large: Boolean = false) {
    val colors = tagColors(tag)
    Pill(
        text = tag.label,
        background = colors.bg,
        foreground = colors.fg,
        style = if (large) AppTheme.text.chip else AppTheme.text.chipSmall,
        horizontal = if (large) 12.dp else 8.dp,
        vertical = if (large) 8.dp else 4.dp,
    )
}

/** Go-bag packed ratio. */
@Composable
fun PackProgress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)),
        color = AppTheme.colors.success,
        trackColor = AppTheme.colors.surface2,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Flat tonal placeholder with an image icon; Coil AsyncImage on top when a photo exists. */
@Composable
fun PhotoThumb(path: String?, size: Dp, modifier: Modifier = Modifier, radius: Dp = Radius.sm, iconSize: Dp = 24.dp) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(AppTheme.colors.surface2)
            .border(1.dp, AppTheme.colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Image, contentDescription = null, tint = AppTheme.colors.fg4, modifier = Modifier.size(iconSize))
        if (path != null) {
            AsyncImage(model = path, contentDescription = "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Icon in a rounded tinted square, used for card leading icons. */
@Composable
fun IconTile(icon: ImageVector, background: Color, tint: Color, size: Dp = 44.dp, iconSize: Dp = 24.dp, radius: Dp = Radius.sm) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(background),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize)) }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = AppTheme.colors.fg3) {
    Text(text.uppercase(), style = AppTheme.text.overline, color = color, modifier = modifier)
}

/** Rounded-square checkbox (design note: success or CTA fill when checked). */
@Composable
fun CheckSquare(
    checked: Boolean,
    checkedColor: Color,
    uncheckedBorder: Color,
    size: Dp = 28.dp,
    borderWidth: Dp = 2.dp,
    tickColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (checked) checkedColor else Color.Transparent)
            .border(borderWidth, if (checked) checkedColor else uncheckedBorder, shape)
            .semantics { contentDescription = if (checked) "Checked" else "Not checked" },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(AppIcons.Check, contentDescription = null, tint = tickColor, modifier = Modifier.size(size * 0.65f))
    }
}

