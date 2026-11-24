@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.localentertainmentgames.homeinventory.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.localentertainmentgames.homeinventory.data.FileStorage
import com.localentertainmentgames.homeinventory.data.Files
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.QOS_CLASS_USER_INITIATED
import platform.posix.memcpy

class IosAppPlatform : AppPlatform {
    override val kind = PlatformKind.Ios

    override val dataDir: String = run {
        val url = NSFileManager.defaultManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, true, null)
        (url?.path ?: NSTemporaryDirectory()).also { Files.ensureDir(it) }
    }

    override val exportDir: String = (NSTemporaryDirectory().trimEnd('/') + "/exports").also { Files.ensureDir(it) }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    override fun dial(phone: String) {
        val digits = phone.filter { it.isDigit() || it == '+' }
        val url = NSURL.URLWithString("tel:$digits") ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    override fun shareFile(path: String, mimeType: String, title: String) {
        val top = topViewController() ?: return
        val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
        sheet.popoverPresentationController?.apply {
            sourceView = top.view
            sourceRect = top.view.bounds.useContents { CGRectMake(size.width / 2, size.height, 1.0, 1.0) }
        }
        top.presentViewController(sheet, animated = true, completion = null)
    }

    /** iOS has no public Downloads folder, so "save" is the Files export sheet. */
    override fun saveFile(path: String, mimeType: String, fileName: String) {
        val top = topViewController() ?: return
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true)
        top.presentViewController(picker, animated = true, completion = null)
    }

    override fun thumbnailJpeg(path: String, maxPx: Int): ByteArray? {
        val image = UIImage.imageWithContentsOfFile(path) ?: return null
        val scaled = image.scaledTo(maxPx.toDouble())
        return UIImageJPEGRepresentation(scaled, 0.8)?.toByteArray()
    }

    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        val access = remember { IosCameraAccess() }
        DisposableEffect(Unit) {
            val token = NSNotificationCenter.defaultCenter.addObserverForName(
                UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue,
            ) { _ -> access.refresh() }
            onDispose { NSNotificationCenter.defaultCenter.removeObserver(token) }
        }
        return access
    }

    @Composable
    override fun BarcodeScanner(modifier: Modifier, torchOn: Boolean, onBarcode: (String) -> Unit) {
        val callback by rememberUpdatedState(onBarcode)
        val scanner = remember { IosScanner { callback(it) } }
        DisposableEffect(Unit) {
            scanner.start()
            onDispose { scanner.stop() }
        }
        LaunchedEffect(torchOn) { scanner.setTorch(torchOn) }
        UIKitView(factory = { scanner.view }, modifier = modifier)
    }

    @Composable
    override fun rememberPhotoCapture(onResult: (String?) -> Unit): () -> Unit {
        val callback by rememberUpdatedState(onResult)
        val delegate = remember { PhotoPickerDelegate(photoDir = "$dataDir/photos") { callback(it) } }
        return {
            val top = topViewController()
            if (top == null) {
                callback(null)
            } else {
                val picker = UIImagePickerController()
                val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                // The simulator has no camera; fall back to the photo library there.
                picker.sourceType = if (UIImagePickerController.isSourceTypeAvailable(camera)) {
                    camera
                } else {
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                }
                picker.delegate = delegate
                top.presentViewController(picker, animated = true, completion = null)
            }
        }
    }

    @Composable
    override fun rememberBackupPicker(onResult: (String?) -> Unit): () -> Unit {
        val callback by rememberUpdatedState(onResult)
        val delegate = remember { BackupPickerDelegate { callback(it) } }
        return {
            val top = topViewController()
            if (top == null) {
                callback(null)
            } else {
                // asCopy puts a private copy in the app's tmp folder, so no security-scoped access is needed.
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON), asCopy = true)
                picker.delegate = delegate
                picker.allowsMultipleSelection = false
                top.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class BackupPickerDelegate(private val onResult: (String?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onResult((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.path)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

/** One repository per process. */
object IosRepository {
    private var instance: InventoryRepository? = null

    fun get(platform: AppPlatform): InventoryRepository = instance ?: InventoryRepository(
        storage = FileStorage("${platform.dataDir}/inventory.json"),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ).also { instance = it }
}

private class IosCameraAccess : CameraAccess {
    private var current by mutableStateOf(read())

    override val status: CameraPermission get() = current

    /** iOS asks only once; after a denial only Settings works. */
    override val canRequest: Boolean get() = current == CameraPermission.NotDetermined

    private fun read(): CameraPermission = when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> CameraPermission.Granted
        AVAuthorizationStatusNotDetermined -> CameraPermission.NotDetermined
        else -> CameraPermission.Denied
    }

    fun refresh() {
        current = read()
    }

    override fun request(onResult: (Boolean) -> Unit) {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                refresh()
                onResult(granted)
            }
        }
    }
}

/** UIView whose preview layer follows its bounds. */
private class PreviewView : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }
}

private class MetadataDelegate(private val onCode: (String) -> Unit) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
        val code = didOutputMetadataObjects.firstNotNullOfOrNull { (it as? AVMetadataMachineReadableCodeObject)?.stringValue }
        if (code != null) onCode(code)
    }
}

/** AVFoundation camera preview + barcode/QR reader with torch control. */
private class IosScanner(onCode: (String) -> Unit) {
    private val session = AVCaptureSession()
    private val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    private val delegate = MetadataDelegate(onCode)
    private val preview = PreviewView()
    val view: UIView get() = preview

    init {
        preview.backgroundColor = platform.UIKit.UIColor.blackColor
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
            val output = AVCaptureMetadataOutput()
            if (session.canAddOutput(output)) {
                session.addOutput(output)
                output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
                val wanted = listOf(
                    AVMetadataObjectTypeEAN13Code, AVMetadataObjectTypeEAN8Code, AVMetadataObjectTypeUPCECode,
                    AVMetadataObjectTypeCode128Code, AVMetadataObjectTypeCode39Code, AVMetadataObjectTypeITF14Code,
                    AVMetadataObjectTypeQRCode, AVMetadataObjectTypeDataMatrixCode, AVMetadataObjectTypePDF417Code,
                )
                output.metadataObjectTypes = wanted.filter { it in output.availableMetadataObjectTypes }
            }
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            preview.layer.addSublayer(layer)
            preview.previewLayer = layer
        }
    }

    fun start() {
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0u)) { session.startRunning() }
    }

    fun stop() {
        setTorch(false)
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0u)) { session.stopRunning() }
    }

    fun setTorch(on: Boolean) {
        val d = device ?: return
        if (!d.hasTorch) return
        if (d.lockForConfiguration(null)) {
            d.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            d.unlockForConfiguration()
        }
    }
}

private class PhotoPickerDelegate(
    private val photoDir: String,
    private val onResult: (String?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, null)
        if (image == null) {
            onResult(null)
            return
        }
        Files.ensureDir(photoDir)
        val path = "$photoDir/${NSUUID().UUIDString}.jpg"
        val data = UIImageJPEGRepresentation(image.scaledTo(1600.0), 0.85)
        onResult(if (data != null && data.writeToFile(path, true)) path else null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onResult(null)
    }
}

/** Downscales so the long side is at most [maxSide] points, drawing upright (bakes in orientation). */
private fun UIImage.scaledTo(maxSide: Double): UIImage {
    val (w, h) = size.useContents { width to height }
    val scale = minOf(1.0, maxSide / maxOf(w, h))
    val target = CGSizeMake(w * scale, h * scale)
    val format = platform.UIKit.UIGraphicsImageRendererFormat.defaultFormat().apply { this.scale = 1.0 }
    return UIGraphicsImageRenderer(size = target, format = format).imageWithActions { _ ->
        drawInRect(CGRectMake(0.0, 0.0, w * scale, h * scale))
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
        ?: UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull()?.windows?.firstOrNull() as? UIWindow
    var vc = window?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

