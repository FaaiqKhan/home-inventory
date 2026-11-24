package com.localentertainmentgames.homeinventory.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.localentertainmentgames.homeinventory.resources.Res
import com.localentertainmentgames.homeinventory.resources.fraunces
import com.localentertainmentgames.homeinventory.resources.inter
import com.localentertainmentgames.homeinventory.resources.jetbrains_mono
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Immutable
data class AppFonts(val display: FontFamily, val ui: FontFamily, val mono: FontFamily)

val LocalAppFonts = staticCompositionLocalOf {
    AppFonts(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Monospace)
}

@Composable
private fun variableFamily(res: FontResource, weights: List<Int>): FontFamily = FontFamily(
    weights.map { w ->
        Font(
            resource = res,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

/** Fraunces (display), Inter (UI + body), JetBrains Mono (numerals). Variable TTFs loaded per weight. */
@Composable
fun rememberAppFonts(): AppFonts = AppFonts(
    display = variableFamily(Res.font.fraunces, listOf(400, 600, 700)),
    ui = variableFamily(Res.font.inter, listOf(400, 500, 600, 700)),
    mono = variableFamily(Res.font.jetbrains_mono, listOf(500, 600, 700)),
)

private fun style(family: FontFamily, weight: Int, size: Int, lineHeight: Double, spacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(
        fontFamily = family,
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        letterSpacing = spacing,
    )

/** Type scale from --le-h1..h6 / --le-body-* / --le-button / --le-tab / --le-overline. */
fun leTypography(f: AppFonts) = Typography(
    displayLarge = style(f.display, 700, 48, 1.1, (-0.02).em),
    displayMedium = style(f.display, 700, 40, 1.1, (-0.02).em),
    displaySmall = style(f.display, 700, 32, 1.1, (-0.01).em),
    headlineLarge = style(f.display, 700, 32, 1.1, (-0.01).em),
    headlineMedium = style(f.display, 700, 24, 1.2, (-0.01).em),
    headlineSmall = style(f.display, 700, 20, 1.2, (-0.01).em),
    titleLarge = style(f.display, 700, 18, 1.2),
    titleMedium = style(f.ui, 700, 16, 1.4),
    titleSmall = style(f.ui, 700, 14, 1.5),
    bodyLarge = style(f.ui, 400, 16, 1.6),
    bodyMedium = style(f.ui, 400, 14, 1.5),
    bodySmall = style(f.ui, 400, 12, 1.5),
    labelLarge = style(f.ui, 700, 16, 1.4, 0.2.sp),
    labelMedium = style(f.ui, 600, 14, 1.2),
    labelSmall = style(f.ui, 600, 10, 1.2),
)

/** Extra text styles used by the kit that have no M3 slot. */
@Immutable
data class AppTextStyles(
    val bodyLgBold: TextStyle,
    val bodyMdBold: TextStyle,
    val bodyXs: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val chip: TextStyle,
    val chipSmall: TextStyle,
    val navLabel: TextStyle,
    val overline: TextStyle,
    val fieldLabel: TextStyle,
    val monoHero: TextStyle,
    val monoBig: TextStyle,
    val monoLarge: TextStyle,
    val monoMedium: TextStyle,
    val monoSmall: TextStyle,
    val monoTiny: TextStyle,
)

fun leTextStyles(f: AppFonts) = AppTextStyles(
    bodyLgBold = style(f.ui, 700, 16, 1.3),
    bodyMdBold = style(f.ui, 700, 14, 1.5),
    bodyXs = style(f.ui, 400, 10, 1.4),
    button = style(f.ui, 700, 16, 1.4, 0.2.sp),
    buttonSmall = style(f.ui, 700, 14, 1.0),
    chip = style(f.ui, 700, 13, 1.0),
    chipSmall = style(f.ui, 700, 10, 1.0),
    navLabel = style(f.ui, 700, 11, 1.0),
    overline = style(f.ui, 600, 12, 1.0, 0.08.em),
    fieldLabel = style(f.ui, 700, 13, 1.2),
    monoHero = style(f.mono, 700, 44, 1.1, (-0.02).em),
    monoBig = style(f.mono, 700, 24, 1.2),
    monoLarge = style(f.mono, 700, 18, 1.3),
    monoMedium = style(f.mono, 700, 16, 1.3),
    monoSmall = style(f.mono, 700, 13, 1.0),
    monoTiny = style(f.mono, 500, 10, 1.3),
)

val LocalAppTextStyles = staticCompositionLocalOf { leTextStyles(AppFonts(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Monospace)) }
