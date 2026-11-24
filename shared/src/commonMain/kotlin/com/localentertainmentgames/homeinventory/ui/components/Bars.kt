package com.localentertainmentgames.homeinventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.navigation.Tab
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme

private data class NavSpec(val tab: Tab, val label: String, val icon: ImageVector)

private val navSpecs = listOf(
    NavSpec(Tab.Home, "Home", AppIcons.Home),
    NavSpec(Tab.Items, "Items", AppIcons.List),
    NavSpec(Tab.Add, "Add", AppIcons.Add),
    NavSpec(Tab.Report, "Report", AppIcons.Description),
)

/** NavigationBar items, 56 dp, 11 sp labels, CTA colour for the active tab. */
@Composable
fun AppBottomBar(current: Tab?, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(AppTheme.colors.bg)) {
        HorizontalDivider(color = AppTheme.colors.border)
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp)
                .padding(top = 2.dp)
                .height(60.dp),
        ) {
            navSpecs.forEach { spec ->
                NavigationBarItem(
                    selected = spec.tab == current,
                    onClick = { onSelect(spec.tab) },
                    icon = { Icon(spec.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text(spec.label, style = AppTheme.text.navLabel) },
                    modifier = Modifier.testTag("nav-${spec.label}"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.colors.cta,
                        selectedTextColor = AppTheme.colors.cta,
                        unselectedIconColor = AppTheme.colors.fg3,
                        unselectedTextColor = AppTheme.colors.fg3,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

/** TopAppBar, 56 dp, back (or close) + h6 title. */
@Composable
fun AppTopBar(
    title: String,
    onNavigate: (() -> Unit)?,
    navIcon: ImageVector = AppIcons.Back,
    navLabel: String = "Back",
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onNavigate != null) {
                IconButton(onClick = onNavigate) { Icon(navIcon, contentDescription = navLabel, tint = AppTheme.colors.fg1) }
            }
        },
        actions = { actions() },
        expandedHeight = 56.dp,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.bg, titleContentColor = AppTheme.colors.fg1),
    )
}
