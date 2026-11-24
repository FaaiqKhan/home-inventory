package com.localentertainmentgames.homeinventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.localentertainmentgames.homeinventory.data.BackupCodec
import com.localentertainmentgames.homeinventory.data.InMemoryStorage
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import com.localentertainmentgames.homeinventory.data.SampleData
import com.localentertainmentgames.homeinventory.model.InventoryData
import com.localentertainmentgames.homeinventory.platform.AppPlatform
import com.localentertainmentgames.homeinventory.platform.CameraAccess
import com.localentertainmentgames.homeinventory.platform.CameraPermission
import com.localentertainmentgames.homeinventory.platform.PlatformKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

/** 2026-11-24 10:00 UTC. */
val FIXED_NOW: Instant = Instant.parse("2026-11-24T10:00:00Z")

class FixedClock(var instant: Instant = FIXED_NOW) : Clock {
    override fun now(): Instant = instant
}

const val SAMPLE_BARCODE = "0194253397427"

/** Records every platform call and lets tests choose camera permission behaviour. */
class FakePlatform(
    permission: CameraPermission = CameraPermission.Granted,
    var canRequestPermission: Boolean = true,
    var grantOnRequest: Boolean = true,
    override val kind: PlatformKind = PlatformKind.Android,
) : AppPlatform {
    private val root = "$SystemTemporaryDirectory/hi-test-${Random.nextLong().toULong()}"
    override val dataDir: String = "$root/data"
    override val exportDir: String = "$root/exports"

    val shared = mutableListOf<String>()
    val saved = mutableListOf<String>()
    val dialed = mutableListOf<String>()
    var settingsOpened = 0
    var permissionRequests = 0
    var lastTorch: Boolean? = null
    var nextPhoto: String? = "/photos/p1.jpg"
    var photoCaptures = 0
    /** Path the fake file picker returns; null means the user cancelled. */
    var nextBackupPick: String? = null
    var backupPicks = 0

    var permissionState by mutableStateOf(permission)

    private val access = object : CameraAccess {
        override val status: CameraPermission get() = permissionState
        override val canRequest: Boolean get() = canRequestPermission
        override fun request(onResult: (Boolean) -> Unit) {
            permissionRequests++
            permissionState = if (grantOnRequest) CameraPermission.Granted else CameraPermission.Denied
            onResult(grantOnRequest)
        }
    }

    override fun openAppSettings() { settingsOpened++ }
    override fun dial(phone: String) { dialed += phone }
    override fun shareFile(path: String, mimeType: String, title: String) { shared += path }
    override fun saveFile(path: String, mimeType: String, fileName: String) { saved += path }
    override fun thumbnailJpeg(path: String, maxPx: Int): ByteArray? = null

    @Composable
    override fun rememberCameraAccess(): CameraAccess = access

    @Composable
    override fun BarcodeScanner(modifier: Modifier, torchOn: Boolean, onBarcode: (String) -> Unit) {
        lastTorch = torchOn
        Box(modifier.testTag("fake-scanner").clickable { onBarcode(SAMPLE_BARCODE) })
    }

    @Composable
    override fun rememberPhotoCapture(onResult: (String?) -> Unit): () -> Unit = {
        photoCaptures++
        onResult(nextPhoto)
    }

    @Composable
    override fun rememberBackupPicker(onResult: (String?) -> Unit): () -> Unit = {
        backupPicks++
        onResult(nextBackupPick)
    }
}

fun sampleData(clock: Clock = FixedClock()): InventoryData = SampleData.build(clock.now().toEpochMilliseconds(), clock)

/** Repository whose storage already holds [data]; writes run immediately. */
fun testRepository(data: InventoryData? = null, clock: Clock = FixedClock(), storage: InMemoryStorage = InMemoryStorage(data?.let(BackupCodec::encode))) =
    InventoryRepository(storage, CoroutineScope(Dispatchers.Unconfined), clock, newId = IdSequence()::next)

class IdSequence {
    private var n = 0
    fun next(): String = "id-${n++}"
}
