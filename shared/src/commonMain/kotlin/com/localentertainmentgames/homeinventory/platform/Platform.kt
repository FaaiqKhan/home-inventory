package com.localentertainmentgames.homeinventory.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

enum class PlatformKind { Android, Ios, Other }

enum class CameraPermission { Granted, NotDetermined, Denied }

/** Camera permission state for the current screen. [status] is Compose state and refreshes on resume. */
interface CameraAccess {
    val status: CameraPermission

    /** False once the OS will no longer show its prompt (iOS after one denial, Android after two). */
    val canRequest: Boolean

    fun request(onResult: (Boolean) -> Unit)
}

/**
 * Everything that needs platform code (expect/actual in the handoff): camera preview and barcode scan,
 * torch, photo capture, image thumbnails for the PDF, save and share sheets, app Settings, phone calls.
 */
interface AppPlatform {
    val kind: PlatformKind

    /** Private app directory for the database and photos. */
    val dataDir: String

    /** Scratch directory for exported files handed to share/save sheets. */
    val exportDir: String

    fun openAppSettings()
    fun dial(phone: String)
    fun shareFile(path: String, mimeType: String, title: String)

    /** Android: system "Save to" picker. iOS: Files export sheet (no public Downloads folder). */
    fun saveFile(path: String, mimeType: String, fileName: String)

    /** A downscaled JPEG of the photo at [path], for embedding in the PDF. */
    fun thumbnailJpeg(path: String, maxPx: Int): ByteArray?

    @Composable
    fun rememberCameraAccess(): CameraAccess

    /** Live camera preview that reports the first barcode or QR code it reads. */
    @Composable
    fun BarcodeScanner(modifier: Modifier, torchOn: Boolean, onBarcode: (String) -> Unit)

    /** Returns a launcher; [onResult] gets the saved photo path or null if cancelled. */
    @Composable
    fun rememberPhotoCapture(onResult: (String?) -> Unit): () -> Unit

    /**
     * Returns a launcher for the system file picker (JSON files). [onResult] gets the path of a
     * local copy of the chosen file, which the caller may delete, or null if cancelled.
     */
    @Composable
    fun rememberBackupPicker(onResult: (String?) -> Unit): () -> Unit
}

val LocalAppPlatform = staticCompositionLocalOf<AppPlatform> { error("No AppPlatform provided") }

fun AppPlatform.cameraSettingsPath(): String = when (kind) {
    PlatformKind.Ios -> "Settings → Home Inventory → Camera"
    else -> "Settings → Apps → Home Inventory → Permissions → Camera"
}

/** System back gesture/button. Android only; iOS uses on-screen back buttons. */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
