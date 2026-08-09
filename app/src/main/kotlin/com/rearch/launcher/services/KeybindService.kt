package com.rearch.launcher.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.rearch.launcher.MainActivity

/**
 * KeybindService — global keybind handler.
 *
 * Mirrors Hyprland-style binds:
 *   SUPER + H/J/K/L    → focus window left/down/up/right
 *   SUPER + Q           → close focused window
 *   SUPER + F           → toggle fullscreen
 *   SUPER + 1–4         → switch workspace
 *   SUPER + SHIFT+1–4   → move window to workspace
 *   SUPER + RETURN      → open app launcher
 *   SUPER + Space       → toggle media play/pause
 *   SUPER + Tab         → cycle windows
 *
 * All keybinds are configurable via DataStore (see KeybindConfig.kt).
 */
class KeybindService : AccessibilityService() {

    companion object {
        private const val TAG = "KeybindService"
        var instance: KeybindService? = null
            private set

        // Broadcast actions for the launcher to receive
        const val ACTION_WORKSPACE_SWITCH  = "com.rearch.launcher.WORKSPACE_SWITCH"
        const val ACTION_WINDOW_CLOSE      = "com.rearch.launcher.WINDOW_CLOSE"
        const val ACTION_WINDOW_FULLSCREEN = "com.rearch.launcher.WINDOW_FULLSCREEN"
        const val ACTION_OPEN_LAUNCHER     = "com.rearch.launcher.OPEN_LAUNCHER"
        const val ACTION_MEDIA_PLAYPAUSE   = "com.rearch.launcher.MEDIA_PLAYPAUSE"
        const val ACTION_WINDOW_FOCUS      = "com.rearch.launcher.WINDOW_FOCUS"
        const val EXTRA_WORKSPACE_INDEX    = "workspace_index"
        const val EXTRA_DIRECTION          = "direction"
    }

    private var superKeyHeld = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "KeybindService connected")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Track SUPER (META) key state
        when (event.keyCode) {
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT -> {
                superKeyHeld = event.action == KeyEvent.ACTION_DOWN
                return false // pass through so system can use it normally too
            }
        }

        if (event.action != KeyEvent.ACTION_DOWN) return false

        val super_ = superKeyHeld || event.isMetaPressed
        val shift  = event.isShiftPressed

        if (!super_) return false

        return when (event.keyCode) {

            // ── Window focus (Hyprland: SUPER + h/j/k/l) ────────────────
            KeyEvent.KEYCODE_H -> { broadcast(ACTION_WINDOW_FOCUS, EXTRA_DIRECTION to "left");  true }
            KeyEvent.KEYCODE_J -> { broadcast(ACTION_WINDOW_FOCUS, EXTRA_DIRECTION to "down");  true }
            KeyEvent.KEYCODE_K -> { broadcast(ACTION_WINDOW_FOCUS, EXTRA_DIRECTION to "up");    true }
            KeyEvent.KEYCODE_L -> { broadcast(ACTION_WINDOW_FOCUS, EXTRA_DIRECTION to "right"); true }

            // ── Close focused window (Hyprland: SUPER + Q) ───────────────
            KeyEvent.KEYCODE_Q -> { broadcast(ACTION_WINDOW_CLOSE); true }

            // ── Toggle fullscreen (Hyprland: SUPER + F) ──────────────────
            KeyEvent.KEYCODE_F -> { broadcast(ACTION_WINDOW_FULLSCREEN); true }

            // ── Open app drawer (Hyprland: SUPER + RETURN) ───────────────
            KeyEvent.KEYCODE_ENTER -> { broadcast(ACTION_OPEN_LAUNCHER); true }

            // ── Media play/pause (SUPER + Space) ─────────────────────────
            KeyEvent.KEYCODE_SPACE -> { broadcast(ACTION_MEDIA_PLAYPAUSE); true }

            // ── Workspace switch (SUPER + 1–4) ───────────────────────────
            KeyEvent.KEYCODE_1 -> { broadcastWorkspace(1, shift); true }
            KeyEvent.KEYCODE_2 -> { broadcastWorkspace(2, shift); true }
            KeyEvent.KEYCODE_3 -> { broadcastWorkspace(3, shift); true }
            KeyEvent.KEYCODE_4 -> { broadcastWorkspace(4, shift); true }

            // ── Cycle windows (SUPER + Tab) ───────────────────────────────
            KeyEvent.KEYCODE_TAB -> { broadcast(ACTION_WINDOW_FOCUS, EXTRA_DIRECTION to "next"); true }

            else -> false
        }
    }

    private fun broadcast(action: String, vararg extras: Pair<String, String>) {
        val intent = Intent(action).apply {
            extras.forEach { (k, v) -> putExtra(k, v) }
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastWorkspace(index: Int, moveWindow: Boolean) {
        val intent = Intent(ACTION_WORKSPACE_SWITCH).apply {
            putExtra(EXTRA_WORKSPACE_INDEX, index)
            putExtra("move_window", moveWindow)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* unused */ }
    override fun onInterrupt() { Log.w(TAG, "KeybindService interrupted") }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
