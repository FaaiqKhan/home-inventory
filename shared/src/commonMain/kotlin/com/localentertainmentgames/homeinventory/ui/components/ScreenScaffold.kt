package com.localentertainmentgames.homeinventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme

/** Screen frame honouring WindowInsets.safeDrawing; the OS draws the status and gesture bars. */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    background: Color = AppTheme.colors.bg,
    topBar: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        topBar()
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (bottomBar != null) {
            bottomBar()
        } else {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
        }
    }
}
