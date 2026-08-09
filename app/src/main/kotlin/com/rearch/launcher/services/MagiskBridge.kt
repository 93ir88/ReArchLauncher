package com.rearch.launcher.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * MagiskBridge — root shell interface.
 *
 * Requires Magisk or KernelSU root. Executes shell commands via `su`.
 *
 * Key functions:
 *   1. Enable Android freeform window mode (enable_freeform_windows setting)
 *   2. Grant WRITE_SECURE_SETTINGS / WRITE_SETTINGS to ourselves
 *   3. Window placement control via `wm` and `am` commands
 */
object MagiskBridge {

    private const val TAG = "MagiskBridge"

    var isRooted = false
        private set

    /** Call once on app start (IO thread) */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        isRooted = checkRoot()
        if (isRooted) {
            enableFreeformMode()
            grantSelfPermissions(context)
            Log.i(TAG, "Root init complete — freeform enabled")
        } else {
            Log.w(TAG, "No root detected — freeform windows unavailable, fallback to intent-based launching")
        }
    }

    // ─── Freeform mode ──────────────────────────────────────────────────────

    /**
     * Enable Android's built-in freeform window mode.
     * Equivalent to: adb shell settings put global enable_freeform_support 1
     *                adb shell settings put global force_resizable_activities 1
     */
    suspend fun enableFreeformMode() = withContext(Dispatchers.IO) {
        runAsRoot(
            "settings put global enable_freeform_support 1",
            "settings put global force_resizable_activities 1",
            "settings put global development_settings_enabled 1",
            // Re-start activity manager so settings take effect without reboot
            "am restart"
        )
    }

    /**
     * Launch an app in a freeform window at specified screen position and size.
     *
     * @param packageName  e.g. "com.spotify.music"
     * @param activityName e.g. ".MainActivity"
     * @param x, y         window top-left in pixels
     * @param w, h         window width/height in pixels
     */
    suspend fun launchInFreeformWindow(
        packageName: String,
        activityName: String,
        x: Int, y: Int,
        w: Int, h: Int
    ) = withContext(Dispatchers.IO) {
        // --display and --bounds are Magisk-level wm/am extensions available via root
        val bounds = "$x,$y,${x + w},${y + h}"
        runAsRoot("am start --display 0 --window-bounds $bounds -n $packageName/$activityName")
    }

    /**
     * Move an existing freeform window (by task ID) to new position.
     */
    suspend fun moveWindow(taskId: Int, x: Int, y: Int, w: Int, h: Int) = withContext(Dispatchers.IO) {
        runAsRoot("am task moveToFront $taskId", "wm bounds $taskId $x $y $w $h")
    }

    // ─── Permission self-grant ───────────────────────────────────────────────

    private suspend fun grantSelfPermissions(context: Context) = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        runAsRoot(
            "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS",
            "pm grant $pkg android.permission.WRITE_SETTINGS",
            "pm grant $pkg android.permission.CHANGE_CONFIGURATION",
            "pm grant $pkg android.permission.SYSTEM_ALERT_WINDOW",
        )
    }

    // ─── Keybind helpers ─────────────────────────────────────────────────────

    /** Set a system-wide key remap via root (requires KeyMapper Magisk module or similar) */
    suspend fun remapKey(fromKeycode: Int, toAction: String) = withContext(Dispatchers.IO) {
        // This targets devices with Magisk module `remap-keys` installed
        runAsRoot("echo '$fromKeycode=$toAction' >> /data/local/tmp/rearch_keymaps")
    }

    // ─── Shell utilities ─────────────────────────────────────────────────────

    private fun checkRoot(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec("su -c id")
            val output = p.inputStream.bufferedReader().readLine() ?: return false
            p.waitFor()
            output.contains("uid=0")
        } catch (e: Exception) {
            Log.d(TAG, "Root check failed: ${e.message}")
            false
        }
    }

    fun runAsRoot(vararg commands: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val stdin   = process.outputStream.bufferedWriter()
            commands.forEach { cmd ->
                stdin.write("$cmd\n")
                Log.d(TAG, "su: $cmd")
            }
            stdin.write("exit\n")
            stdin.flush()
            stdin.close()

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val code   = process.waitFor()
            ShellResult(code, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "Shell error: ${e.message}")
            ShellResult(-1, "", e.message ?: "")
        }
    }

    data class ShellResult(val code: Int, val stdout: String, val stderr: String) {
        val success get() = code == 0
    }
}
