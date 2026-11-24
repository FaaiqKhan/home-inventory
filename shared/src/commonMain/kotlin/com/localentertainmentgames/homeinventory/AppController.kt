package com.localentertainmentgames.homeinventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.localentertainmentgames.homeinventory.data.BackupCodec
import com.localentertainmentgames.homeinventory.data.Files
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import com.localentertainmentgames.homeinventory.domain.InsuranceReport
import com.localentertainmentgames.homeinventory.domain.ItemFilter
import com.localentertainmentgames.homeinventory.pdf.ReportPdf
import com.localentertainmentgames.homeinventory.platform.AppPlatform
import com.localentertainmentgames.homeinventory.ui.navigation.Navigator
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** App-level actions shared by several screens: navigation, toasts, exports. */
class AppController(
    val repo: InventoryRepository,
    val platform: AppPlatform,
    val clock: Clock,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.Default,
    val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val nav = Navigator()

    /** Item list search and filters, kept while you open items and come back. */
    var itemFilter by mutableStateOf(ItemFilter())

    fun openExpiring() {
        itemFilter = ItemFilter(expiringOnly = true)
        nav.push(Screen.Items(expiringOnly = true))
    }

    var toast by mutableStateOf<String?>(null)
        private set
    private var toastJob: Job? = null

    fun showToast(message: String) {
        toast = message
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(TOAST_MILLIS)
            toast = null
        }
    }

    fun today(): LocalDate = clock.todayIn(timeZone)

    fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    /** Writes the whole database as JSON and hands it to the save sheet. */
    fun exportBackup(): Job = scope.launch {
        repo.flush()
        val name = "home-inventory-backup-${today()}.json"
        val path = "${platform.exportDir}/$name"
        val ok = runCatching { withContext(io) { Files.writeText(path, BackupCodec.encode(repo.data.value)) } }.isSuccess
        if (!ok) {
            showToast("Couldn't write the backup")
            return@launch
        }
        platform.saveFile(path, "application/json", name)
        repo.markBackedUp()
        showToast("Backup ready · choose where to save it")
    }

    /** Builds the insurance PDF, then opens the save sheet ([share] = false) or the share sheet. */
    fun exportReport(share: Boolean): Job = scope.launch {
        val report = InsuranceReport.build(repo.data.value.items, today())
        val name = "insurance-report-${today()}.pdf"
        val path = "${platform.exportDir}/$name"
        val ok = runCatching {
            withContext(io) {
                val bytes = ReportPdf.render(report) { platform.thumbnailJpeg(it, THUMB_PX) }
                Files.writeBytes(path, bytes)
            }
        }.isSuccess
        if (!ok) {
            showToast("Couldn't create the PDF")
            return@launch
        }
        if (share) {
            platform.shareFile(path, "application/pdf", "Insurance report")
            showToast("Opening share sheet…")
        } else {
            platform.saveFile(path, "application/pdf", name)
            showToast("Report ready · choose where to save it")
        }
    }

    companion object {
        const val TOAST_MILLIS = 2800L
        const val THUMB_PX = 160
    }
}
