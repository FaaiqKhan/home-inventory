package com.localentertainmentgames.homeinventory.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.localentertainmentgames.homeinventory.platform.CameraAccess
import com.localentertainmentgames.homeinventory.platform.CameraPermission
import com.localentertainmentgames.homeinventory.platform.LocalAppPlatform
import com.localentertainmentgames.homeinventory.ui.components.CameraExplainerDialog

/**
 * Explainer first, then the system prompt. When the OS won't prompt again (iOS after one denial,
 * Android after two), Continue opens app Settings instead.
 */
fun continueCameraRequest(access: CameraAccess, openSettings: () -> Unit, onGranted: () -> Unit) {
    if (access.canRequest) access.request { granted -> if (granted) onGranted() } else openSettings()
}

/** Returns a launcher that runs [onGranted] with camera permission, asking (with the explainer) if needed. */
@Composable
fun rememberCameraGate(onGranted: () -> Unit): () -> Unit {
    val platform = LocalAppPlatform.current
    val access = platform.rememberCameraAccess()
    val granted by rememberUpdatedState(onGranted)
    var dialog by remember { mutableStateOf(false) }
    if (dialog) {
        CameraExplainerDialog(
            onContinue = {
                dialog = false
                continueCameraRequest(access, platform::openAppSettings) { granted() }
            },
            onNotNow = { dialog = false },
        )
    }
    return { if (access.status == CameraPermission.Granted) granted() else dialog = true }
}
