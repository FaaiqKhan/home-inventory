package com.localentertainmentgames.homeinventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Elevation
import com.localentertainmentgames.homeinventory.ui.theme.PillShape

/** .le-btn-primary: CTA blue pill with a neutral 4 dp shadow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    containerColor: Color = AppTheme.colors.cta,
    contentColor: Color = Color.White,
    textStyle: TextStyle = AppTheme.text.button,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AppTheme.colors.borderStrong,
            disabledContentColor = AppTheme.colors.fg3,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = Elevation.cta, pressedElevation = Elevation.cta),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        ButtonContent(text, icon, textStyle, 22.dp)
    }
}

/** .le-btn-secondary: outline pill, 1.5 dp CTA border. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
    textStyle: TextStyle = AppTheme.text.buttonSmall,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = PillShape,
        border = BorderStroke(1.5.dp, AppTheme.colors.cta),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.cta),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        ButtonContent(text, icon, textStyle, 18.dp)
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.height(48.dp)) {
        Text(text, style = AppTheme.text.buttonSmall, color = AppTheme.colors.cta)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, style: TextStyle, iconSize: Dp) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(6.dp))
    }
    Text(text, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
}
