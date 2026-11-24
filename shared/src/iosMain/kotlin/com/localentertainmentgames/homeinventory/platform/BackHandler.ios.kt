package com.localentertainmentgames.homeinventory.platform

import androidx.compose.runtime.Composable

/** iOS has no system back button; screens show their own back and close buttons. */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
