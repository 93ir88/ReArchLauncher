// android/platform/qml/Quickshell/Quickshell.qml
//
// Android replacement for the Quickshell singleton.
// Provides: Quickshell.env(), Quickshell.screens, Quickshell.workingDirectory
pragma Singleton

import QtQuick
import com.rearch.android 1.0

QtObject {
    id: root

    // screens — on Android: just the primary display
    readonly property var screens: [primaryScreen]

    property QtObject primaryScreen: QtObject {
        property string name:   "eDP-1"
        property int width:     Screen.width
        property int height:    Screen.height
        property real devicePixelRatio: Screen.devicePixelRatio
        property real refreshRate: Screen.refreshRate
    }

    // workingDirectory — maps to Android app's files dir
    readonly property string workingDirectory: AndroidWindowBridge.filesDir

    // env() — reads environment variables.
    // On Android: reads from AndroidWindowBridge which checks System.getenv
    // and also checks SharedPreferences overrides.
    function env(name) {
        return AndroidWindowBridge.getEnv(name) ?? ""
    }
}
