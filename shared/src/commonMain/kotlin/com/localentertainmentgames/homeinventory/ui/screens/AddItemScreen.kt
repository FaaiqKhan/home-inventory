package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.AppController
import com.localentertainmentgames.homeinventory.domain.rooms
import com.localentertainmentgames.homeinventory.platform.CameraPermission
import com.localentertainmentgames.homeinventory.platform.LocalAppPlatform
import com.localentertainmentgames.homeinventory.platform.cameraSettingsPath
import com.localentertainmentgames.homeinventory.ui.components.AppTopBar
import com.localentertainmentgames.homeinventory.ui.components.CameraExplainerDialog
import com.localentertainmentgames.homeinventory.ui.components.LinkButton
import com.localentertainmentgames.homeinventory.ui.components.PrimaryButton
import com.localentertainmentgames.homeinventory.ui.components.ScreenScaffold
import com.localentertainmentgames.homeinventory.ui.components.SecondaryButton
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.LeTokens
import com.localentertainmentgames.homeinventory.ui.theme.Radius

@Composable
fun AddItemScreen(c: AppController, startManual: Boolean) {
    val platform = LocalAppPlatform.current
    val data by c.repo.data.collectAsState()
    val lastLocation = remember { data.items.maxByOrNull { it.createdAtMillis }?.location ?: listOf("Home") }
    val flow = remember(startManual) { AddItemFlow(startManual, lastLocation) }
    val access = platform.rememberCameraAccess()
    val capturePhoto = platform.rememberPhotoCapture { path -> if (path != null) flow.onPhotoTaken(path) }
    val gatedCapture = rememberCameraGate(onGranted = capturePhoto)

    LaunchedEffect(access.status, flow.scanned) {
        if (!flow.scanned && access.status == CameraPermission.NotDetermined && !flow.explainerDismissed) flow.openExplainer()
    }

    fun save(close: Boolean) {
        val item = flow.validated() ?: return
        c.repo.addItem(item)
        if (close) {
            c.nav.replace(Screen.Items())
            c.showToast("Saved")
        } else {
            flow.resetForNext()
            c.showToast("Saved. Ready for the next scan.")
        }
    }

    ScreenScaffold(
        topBar = {
            AppTopBar(flow.title, onNavigate = { c.nav.pop() }, navIcon = AppIcons.Close, navLabel = "Close", actions = {
                Text(
                    "${flow.savedCount} saved",
                    style = AppTheme.text.monoSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = AppTheme.colors.fg3,
                    modifier = Modifier.padding(end = 12.dp).testTag("saved-count"),
                )
            })
        },
    ) {
        when {
            flow.scanned -> FormStage(c, flow, data.items.rooms(), onAddPhoto = gatedCapture, onSave = ::save)
            access.status == CameraPermission.Granted -> ScanStage(
                flow,
                onTakePhoto = capturePhoto,
                scanner = { mod -> platform.BarcodeScanner(mod, flow.flashOn) { code -> flow.onBarcode(code) } },
            )
            else -> PermissionBlocked(
                settingsPath = platform.cameraSettingsPath(),
                onWhy = { flow.openExplainer() },
                onOpenSettings = {
                    platform.openAppSettings()
                    c.showToast("Opening Settings…")
                },
                onManual = flow::enterManually,
            )
        }
    }

    if (flow.explainerOpen && !flow.scanned) {
        CameraExplainerDialog(
            onContinue = {
                flow.dismissExplainer()
                continueCameraRequest(access, platform::openAppSettings) {}
            },
            onNotNow = flow::dismissExplainer,
        )
    }
}

@Composable
private fun ScanStage(flow: AddItemFlow, onTakePhoto: () -> Unit, scanner: @Composable (Modifier) -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).background(LeTokens.Dark1).testTag("viewfinder"),
        ) {
            scanner(Modifier.fillMaxSize())
            Viewfinder()
            Text(
                "Point at a barcode or QR code",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).fillMaxWidth(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val on = flow.flashOn
            Surface(
                onClick = flow::toggleFlash,
                shape = CircleShape,
                color = if (on) AppTheme.colors.accent else Color.Transparent,
                contentColor = if (on) LeTokens.Fg1 else AppTheme.colors.fg2,
                border = BorderStroke(1.5.dp, if (on) AppTheme.colors.accent else AppTheme.colors.borderStrong),
                modifier = Modifier.size(52.dp).testTag("flash"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Flash, contentDescription = if (on) "Flashlight on" else "Flashlight off", modifier = Modifier.size(22.dp))
                }
            }
            PrimaryButton("Take Photo", onClick = onTakePhoto, icon = AppIcons.Camera, modifier = Modifier.weight(1f))
        }
        SecondaryButton("Enter Manually", onClick = flow::enterManually, modifier = Modifier.fillMaxWidth())
    }
}

/** Corner brackets and a red scan line over the camera preview. */
@Composable
private fun BoxScope.Viewfinder() {
    Box(Modifier.matchParentSize().padding(horizontal = 44.dp)) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(180.dp)) {
            val w = 4.dp
            Corner(Modifier.align(Alignment.TopStart), top = true, start = true, w = w)
            Corner(Modifier.align(Alignment.TopEnd), top = true, start = false, w = w)
            Corner(Modifier.align(Alignment.BottomStart), top = false, start = true, w = w)
            Corner(Modifier.align(Alignment.BottomEnd), top = false, start = false, w = w)
            Box(Modifier.align(Alignment.Center).padding(horizontal = 10.dp).fillMaxWidth().height(2.dp).background(LeTokens.Error))
        }
    }
}

@Composable
private fun Corner(modifier: Modifier, top: Boolean, start: Boolean, w: androidx.compose.ui.unit.Dp) {
    val size = 36.dp
    Box(modifier.size(size)) {
        Box(Modifier.align(if (top) Alignment.TopStart else Alignment.BottomStart).fillMaxWidth().height(w).background(Color.White))
        Box(Modifier.align(if (start) Alignment.TopStart else Alignment.TopEnd).size(w, size).background(Color.White))
    }
}

@Composable
private fun PermissionBlocked(settingsPath: String, onWhy: () -> Unit, onOpenSettings: () -> Unit, onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(AppTheme.colors.surface)
                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(Radius.lg))
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("camera-blocked"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Box {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(AppTheme.colors.surface2), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Camera, null, tint = AppTheme.colors.fg3, modifier = Modifier.size(48.dp))
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(8.dp, 8.dp).size(32.dp).clip(CircleShape).background(AppTheme.colors.surface).padding(3.dp)
                        .clip(CircleShape).background(AppTheme.colors.error),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
            Text("Camera access is off", style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.fg1, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Without it, you can't scan barcodes or photograph items. You can still add everything by hand.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.fg3,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 290.dp),
            )
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    append("Turn it back on in ")
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = AppTheme.fonts.mono, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.fg1))
                    append(settingsPath)
                    pop()
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.fg3,
                textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(Radius.sm)).background(AppTheme.colors.surface2).padding(horizontal = 12.dp, vertical = 8.dp).widthIn(max = 300.dp),
            )
            LinkButton("Why does it need the camera?", onClick = onWhy)
        }
        PrimaryButton("Open Settings", onClick = onOpenSettings, icon = AppIcons.Settings, modifier = Modifier.fillMaxWidth())
        SecondaryButton("Enter Manually", onClick = onManual, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FormStage(c: AppController, flow: AddItemFlow, rooms: List<String>, onAddPhoto: () -> Unit, onSave: (close: Boolean) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (flow.hasBarcode) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(AppTheme.colors.success050)
                        .border(1.dp, AppTheme.colors.borderStrong, RoundedCornerShape(Radius.md)).padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(AppIcons.Barcode, null, tint = AppTheme.colors.successText, modifier = Modifier.size(26.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Barcode scanned", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.fg3)
                        Text(flow.draft.barcode, style = AppTheme.text.monoMedium, color = AppTheme.colors.fg1, modifier = Modifier.testTag("barcode"))
                    }
                    TextButton(onClick = flow::rescan) { Text("Rescan", style = AppTheme.text.chip, color = AppTheme.colors.cta) }
                }
            }
            ItemFormFields(
                draft = flow.draft,
                onChange = { flow.draft = it },
                showErrors = flow.showErrors,
                rooms = rooms,
                onAddPhoto = onAddPhoto,
                nameHelper = "No network, so product names aren't looked up.",
                detailsInitiallyOpen = false,
            )
        }
        HorizontalDivider(color = AppTheme.colors.border)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Save & Add Another", onClick = { onSave(false) }, height = 56.dp, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Save & Close", onClick = { onSave(true) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
