package com.localentertainmentgames.homeinventory.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Raw design tokens from colors_and_type.css (--le-* and --su-*). */
object LeTokens {
    // Brand primaries
    val Primary = Color(0xFF6949FF)
    val Primary600 = Color(0xFF543ACC)
    val Primary300 = Color(0xFF876DFF)
    val Primary200 = Color(0xFFC3B6FF)
    val Primary050 = Color(0xFFF4F1FF)

    val Cta = Color(0xFF246BFD)
    val Cta600 = Color(0xFF3779FF)
    val Cta050 = Color(0xFFF6F9FF)

    val Accent = Color(0xFFFACC15)
    val Accent050 = Color(0xFFFFFCEB)

    // Status
    val Success = Color(0xFF12D18E)
    val Success050 = Color(0xFFF1FDF5)
    val SuccessText = Color(0xFF0B8055)
    val Warning = Color(0xFFFACC15)
    val Warning050 = Color(0xFFFFFCEB)
    val WarningText = Color(0xFFA67800)
    val Error = Color(0xFFF75555)
    val Error050 = Color(0xFFFFF7F8)

    // Light neutrals
    val Bg = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFAFAFA)
    val Surface2 = Color(0xFFF5F5F5)
    val Border = Color(0xFFEEEEEE)
    val BorderStrong = Color(0xFFE0E0E0)
    val Fg1 = Color(0xFF212121)
    val Fg2 = Color(0xFF424242)
    val Fg3 = Color(0xFF616161)
    val Fg4 = Color(0xFF9E9E9E)

    // Dark neutrals
    val Dark1 = Color(0xFF181A20)
    val Dark2 = Color(0xFF1F222A)
    val Dark3 = Color(0xFF262A35)
    val Dark4 = Color(0xFF35383F)
    val DarkBorderStrong = Color(0xFF4A4D54)

    val Scrim = Color(0x52130F26) // rgba(19,15,38,.32)
    val ScrimStrong = Color(0x85130F26) // rgba(19,15,38,.52)
}

/**
 * Colors without a Material 3 slot (success, warning, gold accent, brand tints, the fg ramp).
 * Provided through [LocalExtendedColors] as the handoff card asks.
 */
@Immutable
data class ExtendedColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val fg1: Color,
    val fg2: Color,
    val fg3: Color,
    val fg4: Color,
    val cta: Color,
    val cta050: Color,
    val brand: Color,
    val brand050: Color,
    val brand300: Color,
    val success: Color,
    val success050: Color,
    val successText: Color,
    val warning: Color,
    val warning050: Color,
    val warningText: Color,
    val error: Color,
    val error050: Color,
    val accent: Color,
    val accent050: Color,
    val isDark: Boolean,
) {
    val brandGradient: Brush get() = Brush.verticalGradient(listOf(LeTokens.Primary, LeTokens.Primary300))
}

val LightExtendedColors = ExtendedColors(
    bg = LeTokens.Bg,
    surface = LeTokens.Surface,
    surface2 = LeTokens.Surface2,
    border = LeTokens.Border,
    borderStrong = LeTokens.BorderStrong,
    fg1 = LeTokens.Fg1,
    fg2 = LeTokens.Fg2,
    fg3 = LeTokens.Fg3,
    fg4 = LeTokens.Fg4,
    cta = LeTokens.Cta,
    cta050 = LeTokens.Cta050,
    brand = LeTokens.Primary,
    brand050 = LeTokens.Primary050,
    brand300 = LeTokens.Primary300,
    success = LeTokens.Success,
    success050 = LeTokens.Success050,
    successText = LeTokens.SuccessText,
    warning = LeTokens.Warning,
    warning050 = LeTokens.Warning050,
    warningText = LeTokens.WarningText,
    error = LeTokens.Error,
    error050 = LeTokens.Error050,
    accent = LeTokens.Accent,
    accent050 = LeTokens.Accent050,
    isDark = false,
)

val DarkExtendedColors = LightExtendedColors.copy(
    bg = LeTokens.Dark1,
    surface = LeTokens.Dark2,
    surface2 = LeTokens.Dark3,
    border = LeTokens.Dark4,
    borderStrong = LeTokens.DarkBorderStrong,
    fg1 = Color(0xFFFFFFFF),
    fg2 = Color(0xFFEEEEEE),
    fg3 = Color(0xFFBDBDBD),
    fg4 = Color(0xFF757575),
    brand300 = LeTokens.Primary300,
    // Status tints keep enough contrast on dark surfaces.
    success050 = Color(0xFF1B3A2F),
    warning050 = Color(0xFF3A3420),
    error050 = Color(0xFF3A2227),
    brand050 = Color(0xFF2F2E41),
    cta050 = Color(0xFF1F2A44),
    accent050 = Color(0xFF3A3420),
    successText = Color(0xFF12D18E),
    warningText = Color(0xFFFACC15),
    isDark = true,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** --le-cta -> primary, --le-primary -> tertiary, --le-bg -> surface, --le-surface/-2 -> surfaceContainerLow/surfaceContainer, --le-border -> outlineVariant. */
fun leColorScheme(x: ExtendedColors): ColorScheme {
    val base = if (x.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = x.cta,
        onPrimary = Color.White,
        primaryContainer = x.cta050,
        onPrimaryContainer = x.cta,
        secondary = x.brand,
        onSecondary = Color.White,
        secondaryContainer = x.brand050,
        onSecondaryContainer = if (x.isDark) x.brand300 else x.brand,
        tertiary = x.brand,
        onTertiary = Color.White,
        tertiaryContainer = x.brand050,
        onTertiaryContainer = if (x.isDark) x.brand300 else x.brand,
        error = x.error,
        onError = Color.White,
        errorContainer = x.error050,
        onErrorContainer = x.error,
        background = x.bg,
        onBackground = x.fg1,
        surface = x.bg,
        onSurface = x.fg1,
        surfaceVariant = x.surface2,
        onSurfaceVariant = x.fg3,
        surfaceContainerLowest = x.bg,
        surfaceContainerLow = x.surface,
        surfaceContainer = x.surface2,
        surfaceContainerHigh = x.bg,
        surfaceContainerHighest = x.surface2,
        surfaceBright = x.bg,
        surfaceDim = x.surface2,
        inverseSurface = x.fg1,
        inverseOnSurface = x.bg,
        outline = x.borderStrong,
        outlineVariant = x.border,
        scrim = LeTokens.Scrim,
    )
}
