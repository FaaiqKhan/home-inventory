package com.localentertainmentgames.homeinventory

import androidx.compose.ui.window.ComposeUIViewController
import com.localentertainmentgames.homeinventory.platform.IosAppPlatform
import com.localentertainmentgames.homeinventory.platform.IosRepository
import platform.UIKit.UIViewController

/** Entry point called from Swift: `MainViewControllerKt.MainViewController()`. */
fun MainViewController(): UIViewController {
    val platform = IosAppPlatform()
    return ComposeUIViewController { App(platform = platform, repository = IosRepository.get(platform)) }
}
