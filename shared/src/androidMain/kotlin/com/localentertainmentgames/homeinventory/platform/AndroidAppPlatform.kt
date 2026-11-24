package com.localentertainmentgames.homeinventory.platform

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Android side of [AppPlatform]. Create it in Activity.onCreate: it registers the "save as" launchers,
 * which must happen before the activity starts.
 */
class AndroidAppPlatform(private val activity: ComponentActivity) : AppPlatform {
    override val kind = PlatformKind.Android
    override val dataDir: String = activity.filesDir.absolutePath
    override val exportDir: String = File(activity.cacheDir, "exports").apply { mkdirs() }.absolutePath

    private val authority = "${activity.packageName}.fileprovider"
    private var pendingSave: File? = null

    private val saveLaunchers: Map<String, ActivityResultLauncher<String>> = listOf("application/pdf", "application/json").associateWith { mime ->
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri -> copyPendingTo(uri) }
    }

    private fun copyPendingTo(uri: Uri?) {
        val src = pendingSave ?: return
        pendingSave = null
        if (uri == null) return
        runCatching {
            activity.contentResolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
        }.onFailure { Toast.makeText(activity, "Couldn't save the file", Toast.LENGTH_SHORT).show() }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    override fun dial(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' }))
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "No phone app on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareFile(path: String, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(activity, authority, File(path))
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(Intent.createChooser(send, title))
    }

    override fun saveFile(path: String, mimeType: String, fileName: String) {
        val launcher = saveLaunchers[mimeType]
        if (launcher == null) {
            shareFile(path, mimeType, fileName)
            return
        }
        pendingSave = File(path)
        launcher.launch(fileName)
    }

    override fun thumbnailJpeg(path: String, maxPx: Int): ByteArray? = runCatching {
        val bitmap = decodeScaled(File(path), maxPx) ?: return null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrNull()

    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        val lifecycleOwner = LocalLifecycleOwner.current
        val access = remember { AndroidCameraAccess(activity) }
        access.launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            access.onResult(granted)
        }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) access.refresh() }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        return access
    }

    @Composable
    override fun BarcodeScanner(modifier: Modifier, torchOn: Boolean, onBarcode: (String) -> Unit) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val callback by rememberUpdatedState(onBarcode)
        val scanner = remember { BarcodeScanning.getClient() }
        val controller = remember {
            LifecycleCameraController(activity).apply {
                setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                val executor = ContextCompat.getMainExecutor(activity)
                setImageAnalysisAnalyzer(
                    executor,
                    MlKitAnalyzer(listOf(scanner), CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
                        result.getValue(scanner)?.firstNotNullOfOrNull { it.rawValue }?.let { callback(it) }
                    },
                )
            }
        }
        DisposableEffect(lifecycleOwner) {
            controller.bindToLifecycle(lifecycleOwner)
            onDispose {
                controller.unbind()
                scanner.close()
            }
        }
        LaunchedEffect(torchOn) { controller.enableTorch(torchOn) }
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
        )
    }

    @Composable
    override fun rememberPhotoCapture(onResult: (String?) -> Unit): () -> Unit {
        val scope = rememberCoroutineScope()
        val callback by rememberUpdatedState(onResult)
        var pending by remember { mutableStateOf<File?>(null) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val file = pending
            pending = null
            if (!ok || file == null || file.length() == 0L) {
                file?.delete()
                callback(null)
            } else {
                scope.launch {
                    val path = withContext(Dispatchers.IO) { shrinkInPlace(file, PHOTO_MAX_PX); file.absolutePath }
                    callback(path)
                }
            }
        }
        return {
            val dir = File(dataDir, "photos").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            pending = file
            try {
                launcher.launch(FileProvider.getUriForFile(activity, authority, file))
            } catch (_: ActivityNotFoundException) {
                pending = null
                Toast.makeText(activity, "No camera app on this device", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        }
    }

    companion object {
        const val PHOTO_MAX_PX = 1600

        /** Decodes [file] no larger than [maxPx] on its long side, upright per EXIF. */
        fun decodeScaled(file: File, maxPx: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
            val raw = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            val scale = minOf(1f, maxPx.toFloat() / maxOf(raw.width, raw.height))
            val rotation = when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (scale == 1f && rotation == 0f) return raw
            val m = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
            }
            return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also { if (it !== raw) raw.recycle() }
        }

        /** Keeps stored photos small: long side at most [maxPx], JPEG 85. */
        fun shrinkInPlace(file: File, maxPx: Int) {
            val bmp = decodeScaled(file, maxPx) ?: return
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bmp.recycle()
        }
    }
}

private class AndroidCameraAccess(private val activity: ComponentActivity) : CameraAccess {
    private val prefs = activity.getSharedPreferences("camera", Context.MODE_PRIVATE)
    private var callback: ((Boolean) -> Unit)? = null
    var launcher: ActivityResultLauncher<String>? = null

    private var current by mutableStateOf(compute())
    private var rationale by mutableStateOf(activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))

    override val status: CameraPermission get() = current

    /** Android shows its prompt until the second denial; after that only Settings works. */
    override val canRequest: Boolean get() = current == CameraPermission.NotDetermined || rationale

    private fun compute(): CameraPermission = when {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> CameraPermission.Granted
        !prefs.getBoolean(KEY_ASKED, false) -> CameraPermission.NotDetermined
        else -> CameraPermission.Denied
    }

    fun refresh() {
        current = compute()
        rationale = activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
    }

    override fun request(onResult: (Boolean) -> Unit) {
        callback = onResult
        launcher?.launch(Manifest.permission.CAMERA) ?: onResult(false)
    }

    fun onResult(granted: Boolean) {
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        refresh()
        callback?.invoke(granted)
        callback = null
    }

    companion object {
        const val KEY_ASKED = "asked"
    }
}
