package com.localentertainmentgames.homeinventory

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.localentertainmentgames.homeinventory.model.DashboardLayout
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test

/** Renders each screen at phone size into build/screenshots for visual review against the design. */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private fun shoot(name: String, layout: DashboardLayout = DashboardLayout.ValueFirst, steps: androidx.compose.ui.test.ComposeUiTest.() -> Unit = {}) =
        runDesktopComposeUiTest(width = 390, height = 844) {
            val data = sampleData().let { it.copy(settings = it.settings.copy(dashboardLayout = layout)) }
            val repo = testRepository(data)
            setContent { App(FakePlatform(), repo, FixedClock(), darkTheme = false) }
            waitUntil(timeoutMillis = 5_000) { repo.loaded.value }
            waitForIdle()
            steps()
            waitForIdle()
            val png = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!
            File(out, "$name.png").writeBytes(png.bytes)
        }

    @Test fun home1a() = shoot("1a-home-value")
    @Test fun home1b() = shoot("1b-home-readiness", DashboardLayout.ReadinessFirst)
    @Test fun home1c() = shoot("1c-home-action", DashboardLayout.ActionFirst)
    @Test fun items() = shoot("1d-items") { onNodeWithTag("nav-Items").performClick() }
    @Test fun detail() = shoot("1e-detail") {
        onNodeWithTag("nav-Report").performClick()
        onNodeWithText("MacBook Pro 14″").performClick()
    }
    @Test fun evacuation() = shoot("1f-evacuation") { onNodeWithTag("gobag-card").performClick() }
    @Test fun scan() = shoot("1g-scan") { onNodeWithTag("nav-Add").performClick() }
    @Test fun form() = shoot("1g-form") {
        onNodeWithTag("nav-Add").performClick()
        onNodeWithTag("fake-scanner").performClick()
    }
    @Test fun report() = shoot("1h-report") { onNodeWithTag("nav-Report").performClick() }
}
