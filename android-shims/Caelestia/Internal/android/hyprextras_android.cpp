// android-shims/Caelestia/Internal/android/hyprextras_android.cpp
//
// Android replacement for hyprextras.cpp
// Original talks to Hyprland via Unix socket IPC.
// This talks to Android WindowManager via JNI + ActivityManager.
//
// INTERFACE IS IDENTICAL to hyprextras.hpp — zero QML changes needed.

#include "../hyprextras.hpp"
#include "hyprdevices_android.hpp"
#include <QDebug>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QtCore/private/qandroidextras_p.h>  // Qt 6 Android JNI helpers

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#endif

namespace caelestia::internal::hypr {

// ─── HyprExtras (Android) ─────────────────────────────────────────────────
// Maps Hyprland IPC concepts → Android equivalents:
//
//   Hyprland concept          Android equivalent
//   ─────────────────         ───────────────────
//   options (hyprland.conf)   Android Settings.Global + Display metrics
//   devices.keyboards         InputManager.getInputDeviceIds()
//   dispatch(request)         ActivityTaskManager / no-op for desktop ops
//   batchMessage(msgs)        iterate dispatch() calls
//   usingLua                  always false on Android

HyprExtras::HyprExtras(QObject* parent)
    : QObject(parent)
    , m_devices(new HyprDevices(this))
{
    // Populate sane defaults matching Hyprland option structure
    // so QML consumers (Hypr.qml) don't break
    m_options = {
        { "general:gaps_in",           4          },
        { "general:gaps_out",          8          },
        { "general:border_size",       2          },
        { "decoration:rounding",       12         },
        { "decoration:blur:enabled",   true       },
        { "animations:enabled",        true       },
        { "misc:disable_hyprland_logo",true       },
        { "input:kb_layout",           "us"       },
        { "input:sensitivity",         0.0        },
    };

    // Initial device enumeration
    QTimer::singleShot(0, this, &HyprExtras::refreshDevices);

    qInfo() << "[ReArch] HyprExtras: Android mode, Hyprland IPC stubbed";
}

QVariantHash HyprExtras::options() const { return m_options; }

HyprDevices* HyprExtras::devices() const { return m_devices; }

// dispatch() — on Linux this sends commands to Hyprland.
// On Android: window management ops go through ActivityTaskManager (root).
// Most decoration/compositor ops are no-ops.
void HyprExtras::message(const QString& message) {
    qDebug() << "[ReArch] dispatch:" << message;

#ifdef Q_OS_ANDROID
    // Map a subset of Hyprland dispatch commands to Android operations
    if (message.startsWith("workspace ")) {
        const int wsId = message.mid(10).toInt();
        // Emit workspace change signal — handled by Android WorkspaceManager
        emit workspaceChangeRequested(wsId);
    } else if (message.startsWith("killactive")) {
        // Close focused window via ActivityTaskManager (needs root)
        QJniObject activity = QtAndroidPrivate::androidActivity();
        // am force-stop handled by MagiskBridge in Kotlin layer
        emit killActiveRequested();
    } else if (message.startsWith("fullscreen")) {
        emit fullscreenRequested();
    }
    // All other compositor-specific ops (layout, border, etc.) silently dropped
#endif
}

void HyprExtras::batchMessage(const QStringList& messages) {
    for (const QString& msg : messages) {
        message(msg);
    }
}

void HyprExtras::applyOptions(const QVariantHash& options) {
    for (auto it = options.cbegin(); it != options.cend(); ++it) {
        m_options.insert(it.key(), it.value());
    }
    emit optionsChanged();
}

void HyprExtras::refreshOptions() {
    // On Linux: re-reads hyprland.conf socket
    // On Android: re-reads SharedPreferences / DataStore
#ifdef Q_OS_ANDROID
    QJniObject context = QtAndroidPrivate::androidContext();
    if (context.isValid()) {
        // Could read Android Settings.Global here
        // For now just re-emit with current values
    }
#endif
    emit optionsChanged();
}

void HyprExtras::refreshDevices() {
    m_devices->refreshFromAndroid();
}

// ─── Dead code: Linux socket management (compiled out on Android) ──────────
void HyprExtras::socketError(QLocalSocket::LocalSocketError error) const {
    Q_UNUSED(error)
}
void HyprExtras::socketStateChanged(QLocalSocket::LocalSocketState state) {
    Q_UNUSED(state)
}
void HyprExtras::readEvent() {}
void HyprExtras::handleEvent(const QString& event) { Q_UNUSED(event) }

HyprExtras::SocketPtr HyprExtras::makeRequestJson(
    const QString& request,
    const std::function<void(bool, QJsonDocument)>& callback)
{
    Q_UNUSED(request)
    // On Android, immediately call back with empty result
    QTimer::singleShot(0, this, [callback] {
        callback(false, QJsonDocument());
    });
    return nullptr;
}

HyprExtras::SocketPtr HyprExtras::makeRequest(
    const QString& request,
    const std::function<void(bool, QByteArray)>& callback)
{
    Q_UNUSED(request)
    QTimer::singleShot(0, this, [callback] {
        callback(false, QByteArray());
    });
    return nullptr;
}

} // namespace caelestia::internal::hypr
