// android/platform/qml/Quickshell/Hyprland/Hyprland.qml
//
// Android replacement for Quickshell.Hyprland singleton.
// On Linux: Quickshell talks to Hyprland IPC socket.
// On Android: we talk to Android ActivityTaskManager + WindowManager.
//
// QML API surface is IDENTICAL — all Caelestia QML files work unchanged.
pragma Singleton

import QtQuick
import QtQuick.Controls
import com.rearch.android 1.0   // JNI bridge to Android WindowManager

QtObject {
    id: root

    // ─── Window/Toplevel management ─────────────────────────────────────────
    // On Linux: HyprlandToplevel = a mapped Wayland window
    // On Android: AndroidToplevel = a running Activity / freeform window task

    readonly property var toplevels: toplevelModel
    readonly property var workspaces: workspaceModel
    readonly property var monitors:   monitorModel

    // The active/focused toplevel
    readonly property var activeToplevel: AndroidWindowBridge.activeToplevel

    // Focused workspace (Android: current "virtual desktop" / task stack)
    readonly property var focusedWorkspace: workspaceModel.focused

    // Focused monitor (Android: primary display)
    readonly property var focusedMonitor: monitorModel.primary

    readonly property bool usingLua: false   // always false on Android

    // ─── Refresh functions (called by Caelestia's Hypr.qml) ─────────────────

    function refreshToplevels()  { AndroidWindowBridge.refreshToplevels()  }
    function refreshWorkspaces() { AndroidWindowBridge.refreshWorkspaces()  }
    function refreshMonitors()   { AndroidWindowBridge.refreshMonitors()    }

    // ─── dispatch() — the core Hyprland IPC function ─────────────────────────
    // Maps subset of Hyprland dispatch commands to Android equivalents.

    function dispatch(request) {
        AndroidWindowBridge.dispatch(request)
    }

    // ─── monitorFor(screen) ──────────────────────────────────────────────────
    function monitorFor(screen) {
        return monitorModel.primary
    }

    // ─── Event bridge ───────────────────────────────────────────────────────
    // Caelestia listens to rawEvent to know when to refresh.
    // On Android: fired by AndroidWindowBridge when tasks change.

    signal rawEvent(var event)

    Connections {
        target: AndroidWindowBridge
        function onTaskChanged(eventName, data) {
            root.rawEvent({ name: eventName, data: data })
        }
    }

    // ─── Internal models ────────────────────────────────────────────────────

    property ListModel toplevelModel:  AndroidWindowBridge.toplevelModel
    property ListModel workspaceModel: AndroidWindowBridge.workspaceModel
    property ListModel monitorModel:   AndroidWindowBridge.monitorModel
}
