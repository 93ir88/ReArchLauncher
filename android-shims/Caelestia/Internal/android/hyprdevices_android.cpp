// android-shims/Caelestia/Internal/android/hyprdevices_android.cpp
//
// Android replacement for hyprdevices.cpp
// Original reads keyboard info from Hyprland IPC JSON.
// This reads from Android InputManager via JNI.
//
// SAME INTERFACE as hyprdevices.hpp.

#include "../hyprdevices.hpp"
#include <QDebug>
#include <QJsonObject>

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#endif

namespace caelestia::internal::hypr {

// ─── HyprKeyboard ─────────────────────────────────────────────────────────

HyprKeyboard::HyprKeyboard(QJsonObject ipcObject, QObject* parent)
    : QObject(parent)
    , m_lastIpcObject(ipcObject)
{
}

QVariantHash HyprKeyboard::lastIpcObject() const {
    return m_lastIpcObject.toVariantHash();
}

QString HyprKeyboard::address() const {
    return m_lastIpcObject.value("address").toString("0x0");
}

QString HyprKeyboard::name() const {
    return m_lastIpcObject.value("name").toString("android-keyboard");
}

QString HyprKeyboard::layout() const {
    return m_lastIpcObject.value("layout").toString("us");
}

QString HyprKeyboard::activeKeymap() const {
    return m_lastIpcObject.value("activeKeymap").toString("English (US)");
}

bool HyprKeyboard::capsLock() const {
    return m_lastIpcObject.value("capsLock").toBool(false);
}

bool HyprKeyboard::numLock() const {
    return m_lastIpcObject.value("numLock").toBool(false);
}

bool HyprKeyboard::main() const {
    return m_lastIpcObject.value("main").toBool(true);
}

bool HyprKeyboard::updateLastIpcObject(QJsonObject object) {
    if (m_lastIpcObject == object) return false;

    const bool addressChanged  = object.value("address")     != m_lastIpcObject.value("address");
    const bool nameChanged     = object.value("name")        != m_lastIpcObject.value("name");
    const bool layoutChanged   = object.value("layout")      != m_lastIpcObject.value("layout");
    const bool keymapChanged   = object.value("activeKeymap")!= m_lastIpcObject.value("activeKeymap");
    const bool capsChanged     = object.value("capsLock")    != m_lastIpcObject.value("capsLock");
    const bool numChanged      = object.value("numLock")     != m_lastIpcObject.value("numLock");
    const bool mainChanged_    = object.value("main")        != m_lastIpcObject.value("main");

    m_lastIpcObject = object;

    emit lastIpcObjectChanged();
    if (addressChanged)  emit addressChanged();
    if (nameChanged)     emit nameChanged();
    if (layoutChanged)   emit layoutChanged();
    if (keymapChanged)   emit activeKeymapChanged();
    if (capsChanged)     emit capsLockChanged();
    if (numChanged)      emit numLockChanged();
    if (mainChanged_)    emit mainChanged();

    return true;
}

// ─── HyprDevices ──────────────────────────────────────────────────────────

HyprDevices::HyprDevices(QObject* parent) : QObject(parent) {
    refreshFromAndroid();
}

QQmlListProperty<HyprKeyboard> HyprDevices::keyboards() {
    return QQmlListProperty<HyprKeyboard>(this, &m_keyboards);
}

bool HyprDevices::updateLastIpcObject(QJsonObject object) {
    const QJsonArray kbs = object.value("keyboards").toArray();
    bool changed = false;

    for (const QJsonValue& v : kbs) {
        const QJsonObject kbObj = v.toObject();
        const QString addr = kbObj.value("address").toString();
        auto* existing = std::find_if(m_keyboards.begin(), m_keyboards.end(),
            [&addr](HyprKeyboard* kb) { return kb->address() == addr; });

        if (existing != m_keyboards.end()) {
            if ((*existing)->updateLastIpcObject(kbObj)) changed = true;
        } else {
            m_keyboards.append(new HyprKeyboard(kbObj, this));
            changed = true;
        }
    }

    if (changed) emit keyboardsChanged();
    return changed;
}

// ─── Android-specific: enumerate physical keyboards via InputManager ───────
void HyprDevices::refreshFromAndroid() {
#ifdef Q_OS_ANDROID
    QJniEnvironment env;
    QJniObject inputManagerService = QJniObject::fromString("input");
    QJniObject context = QtAndroidCompat::context();

    // InputManager.getInputDeviceIds()
    QJniObject inputManager = context.callObjectMethod(
        "getSystemService",
        "(Ljava/lang/String;)Ljava/lang/Object;",
        inputManagerService.object<jstring>()
    );

    if (!inputManager.isValid()) {
        // Fallback: synthesize a virtual keyboard entry (touch keyboard always present)
        synthesizeVirtualKeyboard();
        return;
    }

    jintArray deviceIds = inputManager.callObjectMethod<jintArray>("getInputDeviceIds");
    if (!deviceIds) {
        synthesizeVirtualKeyboard();
        return;
    }

    bool foundKeyboard = false;
    jint* ids = env->GetIntArrayElements(deviceIds, nullptr);
    jsize count = env->GetArrayLength(deviceIds);

    for (jsize i = 0; i < count; i++) {
        QJniObject device = inputManager.callObjectMethod(
            "getInputDevice",
            "(I)Landroid/view/InputDevice;",
            ids[i]
        );
        if (!device.isValid()) continue;

        // Check if device has keyboard sources
        jint sources = device.callMethod<jint>("getSources");
        const jint SOURCE_KEYBOARD = 0x00000101;
        if (!(sources & SOURCE_KEYBOARD)) continue;

        QJniObject deviceName = device.callObjectMethod<jstring>("getName");
        QJniObject keyboardLayout = device.callObjectMethod<jstring>("getKeyboardType");

        QJsonObject kbObj;
        kbObj["address"]     = QString("0x%1").arg(ids[i], 8, 16, QLatin1Char('0'));
        kbObj["name"]        = deviceName.toString();
        kbObj["layout"]      = "us";
        kbObj["activeKeymap"]= "English (US)";
        kbObj["capsLock"]    = false;
        kbObj["numLock"]     = false;
        kbObj["main"]        = !foundKeyboard; // first keyboard is main

        m_keyboards.append(new HyprKeyboard(kbObj, this));
        foundKeyboard = true;
    }
    env->ReleaseIntArrayElements(deviceIds, ids, JNI_ABORT);

    if (!foundKeyboard) synthesizeVirtualKeyboard();

#else
    // Non-Android: synthesize one virtual keyboard for dev builds
    synthesizeVirtualKeyboard();
#endif

    emit keyboardsChanged();
}

void HyprDevices::synthesizeVirtualKeyboard() {
    if (!m_keyboards.isEmpty()) return;
    QJsonObject kbObj;
    kbObj["address"]      = "0x00000001";
    kbObj["name"]         = "android-virtual-keyboard";
    kbObj["layout"]       = "us";
    kbObj["activeKeymap"] = "English (US)";
    kbObj["capsLock"]     = false;
    kbObj["numLock"]      = false;
    kbObj["main"]         = true;
    m_keyboards.append(new HyprKeyboard(kbObj, this));
}

} // namespace caelestia::internal::hypr
