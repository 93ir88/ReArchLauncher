// android/platform/qml/AndroidHypr.qml
//
// Drop-in replacement for caelestia/shell/services/Hypr.qml
// on Android. Provides IDENTICAL exported property/signal API
// but backed by Android WindowManager + InputManager instead of
// Hyprland IPC.
//
// INSTALL: the Android build's CMakeLists copies this over
// caelestia/shell/services/Hypr.qml before compilation.
// The original file is preserved as Hypr.qml.linux for reference.

pragma Singleton

import QtQuick
import Quickshell          // our Android shim
import Quickshell.Hyprland // our Android shim
import Caelestia
import Caelestia.Config
import Caelestia.Internal
import qs.components.misc

Singleton {
    id: root

    // ─── Topology (same property names as original Hypr.qml) ────────────────
    readonly property var toplevels:       Hyprland.toplevels
    readonly property var workspaces:      Hyprland.workspaces
    readonly property var monitors:        Hyprland.monitors
    readonly property bool usingLua:       false  // no Lua on Android

    readonly property var activeToplevel: {
        const t = Hyprland.activeToplevel
        return t?.workspace?.name.startsWith("special:") ||
               Hyprland.focusedWorkspace?.toplevels?.values?.length > 0 ? t : null
    }

    readonly property var focusedWorkspace: Hyprland.focusedWorkspace
    readonly property var focusedMonitor:   Hyprland.focusedMonitor
    readonly property int activeWsId:       focusedWorkspace?.id ?? 1

    // ─── Keyboard (from Android InputManager via HyprDevices shim) ─────────
    readonly property var keyboard: extras.devices.keyboards.find(kb => kb.main) ?? null
    readonly property bool capsLock:       keyboard?.capsLock    ?? false
    readonly property bool numLock:        keyboard?.numLock     ?? false
    readonly property string defaultKbLayout: keyboard?.layout.split(",")[0] ?? "us"
    readonly property string kbLayoutFull: keyboard?.activeKeymap ?? "English (US)"
    readonly property string kbLayout:     kbMap.get(kbLayoutFull) ?? "us"
    readonly property var kbMap:           new Map()

    readonly property alias extras:  extras
    readonly property alias options: extras.options
    readonly property alias devices: extras.devices

    property string lastSpecialWorkspace: ""

    signal configReloaded

    // ─── dispatch() — maps to Android window ops ────────────────────────────
    function dispatch(request) {
        Hyprland.dispatch(request)
    }

    // cycleSpecialWorkspace — workspace cycling logic unchanged
    function cycleSpecialWorkspace(direction) {
        const openSpecials = workspaces.values?.filter(w =>
            w.name.startsWith("special:") && w.lastIpcObject.windows > 0) ?? []
        if (openSpecials.length === 0) return

        const currentIndex = openSpecials.findIndex(w => w.name === lastSpecialWorkspace)
        let nextIndex = 0
        if (currentIndex !== -1) {
            nextIndex = direction === "next"
                ? (currentIndex + 1) % openSpecials.length
                : (currentIndex - 1 + openSpecials.length) % openSpecials.length
        }
        dispatch("workspace " + openSpecials[nextIndex].name)
    }

    function monitorNames() {
        return monitors.values?.map(e => e.name) ?? []
    }

    function monitorFor(screen) {
        return Hyprland.monitorFor(screen)
    }

    // reloadDynamicConfs — on Linux binds CapsLock/NumLock globally via Hyprland.
    // On Android: the AccessibilityService (KeybindService) handles this natively.
    function reloadDynamicConfs() {
        extras.refreshDevices()
    }

    Component.onCompleted: reloadDynamicConfs()

    // ─── CapsLock toast ─────────────────────────────────────────────────────
    onCapsLockChanged: {
        if (!GlobalConfig.utilities?.toasts?.capsLockChanged) return
        Toaster.toast(
            capsLock ? qsTr("Caps lock enabled")  : qsTr("Caps lock disabled"),
            capsLock ? qsTr("Caps lock is currently enabled") : qsTr("Caps lock is currently disabled"),
            capsLock ? "keyboard_capslock_badge" : "keyboard_capslock"
        )
    }

    onNumLockChanged: {
        if (!GlobalConfig.utilities?.toasts?.numLockChanged) return
        Toaster.toast(
            numLock ? qsTr("Num lock enabled")  : qsTr("Num lock disabled"),
            numLock ? qsTr("Num lock is currently enabled") : qsTr("Num lock is currently disabled"),
            numLock ? "looks_one" : "timer_1"
        )
    }

    onKbLayoutFullChanged: {
        if (hadKeyboard && GlobalConfig.utilities?.toasts?.kbLayoutChanged)
            Toaster.toast(qsTr("Keyboard layout changed"),
                          qsTr("Layout changed to: %1").arg(kbLayoutFull), "keyboard")
        hadKeyboard = !!keyboard
    }

    property bool hadKeyboard

    // ─── Event bridge: maps Android window events to Caelestia event names ──
    // Caelestia uses these exact event names internally.
    Connections {
        target: Hyprland
        function onRawEvent(event) {
            const n = event.name
            // Android fires these same event names via AndroidWindowBridge
            if (n === "openwindow" || n === "closewindow" || n === "movewindow") {
                Hyprland.refreshToplevels()
                Hyprland.refreshWorkspaces()
            } else if (n === "workspace" || n === "moveworkspace") {
                Hyprland.refreshWorkspaces()
                Hyprland.refreshMonitors()
            }
        }
    }

    // ─── HyprExtras (Android-stubbed C++ object) ────────────────────────────
    HyprExtras {
        id: extras
        usingLua: false
    }
}
