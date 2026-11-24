package com.localentertainmentgames.homeinventory.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

sealed interface Screen {
    data object Home : Screen
    data class Items(val expiringOnly: Boolean = false) : Screen
    data class Detail(val itemId: String) : Screen
    data class Edit(val itemId: String) : Screen
    data class Add(val manual: Boolean = false) : Screen
    data object Evacuation : Screen
    data object Report : Screen
}

enum class Tab { Home, Items, Add, Report }

/** Back stack for the app. Tabs reset the stack to Home + tab, so Back from a tab returns Home. */
class Navigator(start: Screen = Screen.Home) {
    val stack: SnapshotStateList<Screen> = mutableStateListOf(start)

    val current: Screen get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        if (current != screen) stack.add(screen)
    }

    fun pop(): Boolean {
        if (!canPop) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Replaces the top screen, e.g. Edit -> Detail after saving. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    /** Pops back to the nearest Detail(itemId) or removes screens about a deleted item. */
    fun removeItemScreens(itemId: String) {
        stack.removeAll { (it is Screen.Detail && it.itemId == itemId) || (it is Screen.Edit && it.itemId == itemId) }
        if (stack.isEmpty()) stack.add(Screen.Home)
    }

    fun selectTab(tab: Tab) {
        val target = when (tab) {
            Tab.Home -> Screen.Home
            Tab.Items -> Screen.Items()
            Tab.Add -> Screen.Add()
            Tab.Report -> Screen.Report
        }
        stack.clear()
        stack.add(Screen.Home)
        if (target != Screen.Home) stack.add(target)
    }

    val currentTab: Tab?
        get() = when (current) {
            Screen.Home -> Tab.Home
            is Screen.Items -> Tab.Items
            Screen.Report -> Tab.Report
            is Screen.Add -> Tab.Add
            else -> null
        }
}
