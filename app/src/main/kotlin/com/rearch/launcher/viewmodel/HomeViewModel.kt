package com.rearch.launcher.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.media.session.MediaController
import android.os.BatteryManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rearch.launcher.model.*
import com.rearch.launcher.services.KeybindService
import com.rearch.launcher.services.MagiskBridge
import com.rearch.launcher.services.MediaNotificationService
import com.rearch.launcher.ui.components.DashboardTab
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application.applicationContext
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        startClock()
        loadApps()
        observeMedia()
        observeBattery()
        registerKeybindReceiver()
    }

    // ─── Clock ───────────────────────────────────────────────────────────────

    private fun startClock() {
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                _uiState.update { it.copy(
                    currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    currentDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
                )}
                delay(10_000)
            }
        }
    }

    // ─── Installed apps ──────────────────────────────────────────────────────

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm   = ctx.packageManager
            val apps = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.GET_META_DATA
            ).map { info ->
                val icon = (info.loadIcon(pm) as? BitmapDrawable)?.bitmap?.asImageBitmap()
                AppInfo(
                    packageName = info.activityInfo.packageName,
                    label       = info.loadLabel(pm).toString(),
                    icon        = icon
                )
            }.sortedBy { it.label }
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    // ─── Media ───────────────────────────────────────────────────────────────

    private fun observeMedia() {
        viewModelScope.launch {
            MediaNotificationService.mediaState.collectLatest { media ->
                _uiState.update { it.copy(mediaState = media) }
            }
        }
    }

    fun handleMediaAction(action: MediaAction) {
        val controller = _uiState.value.mediaState.controller ?: return
        val transport  = controller.transportControls
        when (action) {
            is MediaAction.Play     -> transport.play()
            is MediaAction.Pause    -> transport.pause()
            is MediaAction.Next     -> transport.skipToNext()
            is MediaAction.Previous -> transport.skipToPrevious()
            is MediaAction.Seek     -> transport.seekTo(action.positionMs)
        }
    }

    // ─── Battery ─────────────────────────────────────────────────────────────

    private fun observeBattery() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ctx.registerReceiver(batteryReceiver, filter)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status  = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val pct     = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
            val charging= status == BatteryManager.BATTERY_STATUS_CHARGING ||
                          status == BatteryManager.BATTERY_STATUS_FULL
            _uiState.update { it.copy(batteryLevel = pct, isCharging = charging) }
        }
    }

    // ─── Dashboard ───────────────────────────────────────────────────────────

    fun toggleDashboard() = _uiState.update { it.copy(dashboardVisible = !it.dashboardVisible) }
    fun setDashboardTab(tab: DashboardTab) = _uiState.update { it.copy(dashboardTab = tab) }

    // ─── App Drawer ──────────────────────────────────────────────────────────

    fun openAppDrawer() = _uiState.update { it.copy(appDrawerVisible = true) }
    fun closeAppDrawer() = _uiState.update { it.copy(appDrawerVisible = false) }

    // ─── App launching (freeform) ─────────────────────────────────────────────

    fun launchApp(app: AppInfo) {
        viewModelScope.launch {
            closeAppDrawer()
            if (MagiskBridge.isRooted) {
                // Open in freeform window
                val pm = ctx.packageManager
                val launch = pm.getLaunchIntentForPackage(app.packageName)
                val activityName = launch?.component?.className ?: return@launch
                val w = 1000; val h = 700
                val x = (80..300).random(); val y = (80..200).random()

                MagiskBridge.launchInFreeformWindow(app.packageName, activityName, x, y, w, h)

                val newWindow = OpenWindow(
                    id          = "${app.packageName}_${System.currentTimeMillis()}",
                    packageName = app.packageName,
                    appName     = app.label,
                    icon        = app.icon,
                    x           = x.toFloat(),
                    y           = y.toFloat(),
                    width       = w.toFloat(),
                    height      = h.toFloat()
                )
                _uiState.update { it.copy(openWindows = it.openWindows + newWindow) }
            } else {
                // No root — regular launch
                ctx.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(it)
                }
            }
        }
    }

    // ─── Window management ───────────────────────────────────────────────────

    fun moveWindow(id: String, dx: Float, dy: Float) {
        _uiState.update { state ->
            state.copy(openWindows = state.openWindows.map { w ->
                if (w.id == id) w.copy(x = w.x + dx, y = w.y + dy) else w
            })
        }
        // Also move the real system window via root
        val win = _uiState.value.openWindows.find { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (MagiskBridge.isRooted && win.taskId >= 0) {
                MagiskBridge.moveWindow(win.taskId, win.x.toInt(), win.y.toInt(), win.width.toInt(), win.height.toInt())
            }
        }
    }

    fun resizeWindow(id: String, dw: Float, dh: Float) {
        _uiState.update { state ->
            state.copy(openWindows = state.openWindows.map { w ->
                if (w.id == id) w.copy(
                    width  = (w.width  + dw).coerceAtLeast(200f),
                    height = (w.height + dh).coerceAtLeast(150f)
                ) else w
            })
        }
    }

    fun closeWindow(id: String) {
        val win = _uiState.value.openWindows.find { it.id == id }
        _uiState.update { it.copy(openWindows = it.openWindows.filter { w -> w.id != id }) }
        win?.let { w ->
            viewModelScope.launch(Dispatchers.IO) {
                if (MagiskBridge.isRooted && w.taskId >= 0) {
                    MagiskBridge.runAsRoot("am force-stop ${w.packageName}")
                }
            }
        }
    }

    fun focusWindow(id: String) {
        _uiState.update { state ->
            val windows = state.openWindows.map { w -> w.copy(isFocused = w.id == id) }
            state.copy(openWindows = windows)
        }
    }

    // ─── Workspaces ──────────────────────────────────────────────────────────

    fun switchWorkspace(index: Int) = _uiState.update { it.copy(currentWorkspace = index) }

    // ─── Keybind receiver ────────────────────────────────────────────────────

    private fun registerKeybindReceiver() {
        val filter = IntentFilter().apply {
            addAction(KeybindService.ACTION_WORKSPACE_SWITCH)
            addAction(KeybindService.ACTION_WINDOW_CLOSE)
            addAction(KeybindService.ACTION_OPEN_LAUNCHER)
            addAction(KeybindService.ACTION_MEDIA_PLAYPAUSE)
            addAction(KeybindService.ACTION_WINDOW_FOCUS)
        }
        ctx.registerReceiver(keybindReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private val keybindReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                KeybindService.ACTION_WORKSPACE_SWITCH -> {
                    val idx = intent.getIntExtra(KeybindService.EXTRA_WORKSPACE_INDEX, 1)
                    switchWorkspace(idx)
                }
                KeybindService.ACTION_WINDOW_CLOSE -> {
                    val focused = _uiState.value.openWindows.find { it.isFocused }
                    focused?.let { closeWindow(it.id) }
                }
                KeybindService.ACTION_OPEN_LAUNCHER -> openAppDrawer()
                KeybindService.ACTION_MEDIA_PLAYPAUSE -> {
                    val ms = _uiState.value.mediaState
                    handleMediaAction(if (ms.isPlaying) MediaAction.Pause else MediaAction.Play)
                }
                KeybindService.ACTION_WINDOW_FOCUS -> {
                    // Cycle through windows by direction
                    val dir  = intent.getStringExtra(KeybindService.EXTRA_DIRECTION) ?: "next"
                    val wins = _uiState.value.openWindows
                    if (wins.isEmpty()) return
                    val curIdx = wins.indexOfFirst { it.isFocused }.coerceAtLeast(0)
                    val nextIdx = when (dir) {
                        "next" -> (curIdx + 1) % wins.size
                        else   -> (curIdx + 1) % wins.size
                    }
                    focusWindow(wins[nextIdx].id)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            ctx.unregisterReceiver(batteryReceiver)
            ctx.unregisterReceiver(keybindReceiver)
        } catch (_: Exception) {}
    }
}
