package com.localentertainmentgames.homeinventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

@Composable
fun HomeInventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val fonts = rememberAppFonts()
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val typography = remember(fonts) { leTypography(fonts) }
    val textStyles = remember(fonts) { leTextStyles(fonts) }
    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalAppFonts provides fonts,
        LocalAppTextStyles provides textStyles,
    ) {
        MaterialTheme(
            colorScheme = leColorScheme(extended),
            typography = typography,
            shapes = LeShapes,
            content = content,
        )
    }
}

/** Evacuation mode: nested MaterialTheme(darkColorScheme) regardless of the system setting. */
@Composable
fun EvacuationTheme(content: @Composable () -> Unit) = HomeInventoryTheme(darkTheme = true, content = content)

object AppTheme {
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
    val text: AppTextStyles
        @Composable @ReadOnlyComposable get() = LocalAppTextStyles.current
    val fonts: AppFonts
        @Composable @ReadOnlyComposable get() = LocalAppFonts.current
}
