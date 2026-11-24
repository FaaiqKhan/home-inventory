package com.localentertainmentgames.homeinventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.data.FileStorage
import com.localentertainmentgames.homeinventory.data.InventoryRepository
import com.localentertainmentgames.homeinventory.platform.AppPlatform
import com.localentertainmentgames.homeinventory.platform.LocalAppPlatform
import com.localentertainmentgames.homeinventory.platform.SystemBackHandler
import com.localentertainmentgames.homeinventory.ui.components.Toast
import com.localentertainmentgames.homeinventory.ui.navigation.Screen
import com.localentertainmentgames.homeinventory.ui.screens.AddItemScreen
import com.localentertainmentgames.homeinventory.ui.screens.EditItemScreen
import com.localentertainmentgames.homeinventory.ui.screens.EvacuationScreen
import com.localentertainmentgames.homeinventory.ui.screens.HomeScreen
import com.localentertainmentgames.homeinventory.ui.screens.ItemDetailScreen
import com.localentertainmentgames.homeinventory.ui.screens.ItemListScreen
import com.localentertainmentgames.homeinventory.ui.screens.ReportScreen
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.HomeInventoryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock

/** Root of the shared UI, used by MainActivity and MainViewController. */
@Composable
fun App(
    platform: AppPlatform,
    repository: InventoryRepository? = null,
    clock: Clock = Clock.System,
    darkTheme: Boolean? = null,
) {
    val repo = repository ?: remember(platform) {
        InventoryRepository(
            storage = FileStorage("${platform.dataDir}/inventory.json"),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            clock = clock,
        )
    }
    val scope = rememberCoroutineScope()
    val controller = remember(repo) { AppController(repo, platform, clock, scope) }
    LaunchedEffect(repo) { if (!repo.loaded.value) repo.load() }
    val loaded by repo.loaded.collectAsState()

    CompositionLocalProvider(LocalAppPlatform provides platform) {
        if (darkTheme == null) {
            HomeInventoryTheme { Root(controller, loaded) }
        } else {
            HomeInventoryTheme(darkTheme = darkTheme) { Root(controller, loaded) }
        }
    }
}

@Composable
private fun Root(controller: AppController, loaded: Boolean) {
    Box(Modifier.fillMaxSize().background(AppTheme.colors.bg)) {
        if (loaded) AppContent(controller)
        Toast(
            controller.toast,
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 64.dp),
        )
    }
}

@Composable
private fun AppContent(c: AppController) {
    val nav = c.nav
    SystemBackHandler(enabled = nav.canPop) { nav.pop() }
    when (val screen = nav.current) {
        Screen.Home -> HomeScreen(c)
        is Screen.Items -> ItemListScreen(c, screen.expiringOnly)
        is Screen.Detail -> ItemDetailScreen(c, screen.itemId)
        is Screen.Edit -> EditItemScreen(c, screen.itemId)
        is Screen.Add -> AddItemScreen(c, screen.manual)
        Screen.Evacuation -> EvacuationScreen(c)
        Screen.Report -> ReportScreen(c)
    }
}
