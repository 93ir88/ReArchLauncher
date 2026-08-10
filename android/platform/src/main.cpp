// android/platform/src/main.cpp
// Qt 6 Android entry point for ReArchLauncher.
// Registers all Android QML singletons and launches the Caelestia shell.

#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickWindow>
#include <QSurfaceFormat>

#include "AndroidWindowBridge.hpp"

#ifdef Q_OS_ANDROID
#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#include <QCoreApplication>
// Qt6 public Android context API (replaces QtAndroidPrivate)
namespace QtAndroidCompat {
    static inline QJniObject context() {
        return QJniObject(QNativeInterface::QAndroidApplication::context());
    }
}
#endif
#endif

int main(int argc, char* argv[]) {
    // Qt Android high-DPI setup
    QGuiApplication::setHighDpiScaleFactorRoundingPolicy(
        Qt::HighDpiScaleFactorRoundingPolicy::PassThrough);

    QGuiApplication app(argc, argv);
    app.setApplicationName("ReArchLauncher");
    app.setApplicationVersion("1.0.0");
    app.setOrganizationName("com.rearch");

    // Vulkan / OpenGL ES setup for Caelestia's shaders
    QSurfaceFormat fmt;
    fmt.setRenderableType(QSurfaceFormat::OpenGLES);
    fmt.setProfile(QSurfaceFormat::CoreProfile);
    fmt.setVersion(3, 1);
    QSurfaceFormat::setDefaultFormat(fmt);

    // Register C++ singletons exposed to QML
    qmlRegisterSingletonType<AndroidWindowBridge>(
        "com.rearch.android", 1, 0, "AndroidWindowBridge",
        [](QQmlEngine*, QJSEngine*) -> QObject* {
            return new AndroidWindowBridge();
        }
    );

#ifdef Q_OS_ANDROID
    // Request runtime permissions
    // Permission android.permission.POST_NOTIFICATIONS requested via AndroidManifest.xml
    // Permission android.permission.RECORD_AUDIO requested via AndroidManifest.xml  // audio visualizer
#endif

    QQmlApplicationEngine engine;

    // Tell QML where to find:
    //   1. The Quickshell shims  (Quickshell.* modules)
    //   2. The Caelestia plugin  (Caelestia.* modules)
    //   3. The Caelestia QML     (components, modules, services)
    engine.addImportPath("qrc:/qt/qml");
    engine.addImportPath(":/caelestia-patched");

    // Set env vars that Caelestia reads (XDG_* equivalents)
    engine.rootContext()->setContextProperty(
        "REARCH_FILES_DIR",
        AndroidWindowBridge::instance() ? AndroidWindowBridge::instance()->filesDir() : ""
    );

    // Load the actual Caelestia shell.qml (patched copy, not original)
    // This is the entry point Caelestia uses on Linux too
    const QUrl shellUrl(QStringLiteral("qrc:/caelestia-patched/shell.qml"));

    QObject::connect(
        &engine, &QQmlApplicationEngine::objectCreationFailed,
        &app, []() { QCoreApplication::exit(-1); },
        Qt::QueuedConnection
    );

    engine.load(shellUrl);

    if (engine.rootObjects().isEmpty()) {
        qCritical() << "[ReArch] Failed to load shell.qml";
        return -1;
    }

    return app.exec();
}
