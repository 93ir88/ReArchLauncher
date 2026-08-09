package com.rearch.launcher.model

import android.graphics.ImageDecoder
import android.media.session.MediaController
import androidx.compose.ui.graphics.ImageBitmap

// ─── App Info ────────────────────────────────────────────────────────────────
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null
)

// ─── Open freeform window ────────────────────────────────────────────────────
data class OpenWindow(
    val id: String,
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap? = null,
    val x: Float = 80f,
    val y: Float = 80f,
    val width: Float = 400f,
    val height: Float = 300f,
    val taskId: Int = -1,
    val isFocused: Boolean = false
)

// ─── Media State ─────────────────────────────────────────────────────────────
data class MediaState(
    val title: String        = "",
    val artist: String       = "",
    val album: String        = "",
    val albumArtUri: String? = null,
    val isPlaying: Boolean   = false,
    val position: Long       = 0L,
    val duration: Long       = 0L,
    val sources: List<String> = emptyList(),
    val controller: MediaController? = null
)

// ─── Media Actions ───────────────────────────────────────────────────────────
sealed class MediaAction {
    object Play     : MediaAction()
    object Pause    : MediaAction()
    object Next     : MediaAction()
    object Previous : MediaAction()
    data class Seek(val positionMs: Long) : MediaAction()
}

// ─── Home UI State ───────────────────────────────────────────────────────────
data class HomeUiState(
    val wallpaperUri: String?      = null,
    val currentTime: String        = "00:00",
    val currentDate: String        = "Mon, Jan 1",
    val batteryLevel: Int          = 100,
    val isCharging: Boolean        = false,
    val networkStrength: Int       = 4,
    val dashboardVisible: Boolean  = false,
    val dashboardTab: com.rearch.launcher.ui.components.DashboardTab = com.rearch.launcher.ui.components.DashboardTab.MEDIA,
    val appDrawerVisible: Boolean  = false,
    val installedApps: List<AppInfo> = emptyList(),
    val openWindows: List<OpenWindow> = emptyList(),
    val currentWorkspace: Int      = 1,
    val mediaState: MediaState     = MediaState()
)
