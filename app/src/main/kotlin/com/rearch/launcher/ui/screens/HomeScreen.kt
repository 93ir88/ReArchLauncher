package com.rearch.launcher.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rearch.launcher.ui.components.*
import com.rearch.launcher.ui.theme.CaelestiaColors
import com.rearch.launcher.viewmodel.HomeViewModel

@SuppressLint("ClickableViewAccessibility")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    Box(modifier = modifier) {

        // ─── Wallpaper Layer ────────────────────────────────────────────
        WallpaperLayer(wallpaperUri = uiState.wallpaperUri)

        // ─── Left Sidebar (icon dock matching screenshot) ───────────────
        LeftSidebar(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(48.dp),
            onDashboardToggle = vm::toggleDashboard,
            currentWorkspace = uiState.currentWorkspace,
            onWorkspaceChange = vm::switchWorkspace
        )

        // ─── Top Bar ───────────────────────────────────────────────────
        TopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 56.dp),
            currentTime = uiState.currentTime,
            currentDate = uiState.currentDate,
            batteryLevel = uiState.batteryLevel,
            isCharging = uiState.isCharging,
            networkStrength = uiState.networkStrength,
            onSearchOpen = vm::openAppDrawer
        )

        // ─── Desktop Clock (bottom-right, matching screenshot 21:40) ────
        DesktopClock(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = 32.dp),
            time = uiState.currentTime,
            date = uiState.currentDate
        )

        // ─── Dashboard Popup ────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.dashboardVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY  = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 64.dp, end = 16.dp)
        ) {
            DashboardPanel(
                selectedTab = uiState.dashboardTab,
                onTabChange = vm::setDashboardTab,
                mediaState = uiState.mediaState,
                onMediaAction = vm::handleMediaAction,
                onDismiss = vm::toggleDashboard
            )
        }

        // ─── App Drawer (swipe-up) ───────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.appDrawerVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY  = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            AppDrawerOverlay(
                apps = uiState.installedApps,
                onAppLaunch = { appInfo ->
                    vm.launchApp(appInfo)
                },
                onDismiss = vm::closeAppDrawer
            )
        }

        // ─── Freeform Windows Layer ──────────────────────────────────────
        FreeformWindowLayer(
            windows = uiState.openWindows,
            onWindowMove   = vm::moveWindow,
            onWindowResize = vm::resizeWindow,
            onWindowClose  = vm::closeWindow,
            onWindowFocus  = vm::focusWindow
        )

        // ─── Swipe gesture detector for app drawer ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -30f) vm.openAppDrawer()
                    }
                }
        )
    }
}
