package com.localentertainmentgames.homeinventory.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.Elevation
import com.localentertainmentgames.homeinventory.ui.theme.Radius

/** Dark toast with a green check, pinned near the top of the screen. */
@Composable
fun Toast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(Radius.md),
            color = AppTheme.colors.fg1,
            shadowElevation = Elevation.e2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("toast"),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Check, contentDescription = null, tint = AppTheme.colors.success, modifier = Modifier.size(20.dp))
                Box(Modifier.width(10.dp))
                Text(message.orEmpty(), style = AppTheme.text.bodyMdBold, color = AppTheme.colors.bg)
            }
        }
    }
}

/** Camera explainer (AlertDialog with icon, 32 dp corners). Wording doubles as the iOS usage string. */
@Composable
fun CameraExplainerDialog(onContinue: () -> Unit, onNotNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotNow,
        shape = RoundedCornerShape(Radius.xl),
        containerColor = AppTheme.colors.bg,
        modifier = Modifier.testTag("camera-explainer"),
        icon = {
            Box(
                Modifier.size(56.dp).background(AppTheme.colors.cta050, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.Camera, contentDescription = null, tint = AppTheme.colors.cta, modifier = Modifier.size(28.dp)) }
        },
        title = { Text("Allow camera access?", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Home Inventory uses the camera for these things only:",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.fg3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExplainerRow(AppIcons.Barcode, "Scan barcodes", "Saves the barcode number so you can find the item later.")
                ExplainerRow(AppIcons.Camera, "Photograph your things", "Items, serial plates and receipts, for insurance claims.")
                ExplainerRow(AppIcons.Shield, "Stays on this phone", "Photos and scans are never uploaded. No account needed.")
                Text(
                    "Your phone will ask for permission next.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.fg3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { PrimaryButton("Continue", onClick = onContinue, height = 48.dp, textStyle = AppTheme.text.buttonSmall) },
        dismissButton = { LinkButton("Not now", onClick = onNotNow) },
    )
}

@Composable
private fun ExplainerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon, AppTheme.colors.cta050, AppTheme.colors.cta, size = 40.dp, iconSize = 22.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.text.bodyMdBold, color = AppTheme.colors.fg1)
            Text(body, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = AppTheme.colors.fg3)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.xl),
        containerColor = AppTheme.colors.bg,
        title = { Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = AppTheme.colors.fg2) },
        confirmButton = {
            PrimaryButton(
                confirmLabel,
                onClick = onConfirm,
                height = 48.dp,
                textStyle = AppTheme.text.buttonSmall,
                containerColor = if (destructive) AppTheme.colors.error else AppTheme.colors.cta,
            )
        },
        dismissButton = { LinkButton("Cancel", onClick = onDismiss) },
    )
}

