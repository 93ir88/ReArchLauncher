// android/platform/src/AndroidWindowBridge.cpp
//
// C++ QObject registered as QML singleton "AndroidWindowBridge".
// Bridges Quickshell API calls → Android JNI calls.
// Root (Magisk/KernelSU) used for privileged window management.

#include "AndroidWindowBridge.hpp"
#include <QDebug>
#include <QProcess>

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#include <QtCore/private/qandroidextras_p.h>
#endif

AndroidWindowBridge* AndroidWindowBridge::s_instance = nullptr;

AndroidWindowBridge::AndroidWindowBridge(QObject* parent) : QObject(parent) {
    s_instance = this;

    m_toplevelModel  = new QStandardItemModel(this);
    m_workspaceModel = new QStandardItemModel(this);
    m_monitorModel   = new QStandardItemModel(this);

    // Bootstrap: create 4 virtual workspaces
    for (int i = 1; i <= 4; ++i) {
        auto* ws = new QStandardItem(QString::number(i));
        ws->setData(i, Qt::UserRole);
        ws->setData(QString("workspace_%1").arg(i), Qt::UserRole + 1);
        m_workspaceModel->appendRow(ws);
    }

    // Primary monitor
    auto* mon = new QStandardItem("primary");
    mon->setData("eDP-1", Qt::UserRole);
    m_monitorModel->appendRow(mon);

    // Poll task list every 2s (Android background restrictions apply)
    m_pollTimer = new QTimer(this);
    connect(m_pollTimer, &QTimer::timeout, this, &AndroidWindowBridge::refreshToplevels);
    m_pollTimer->start(2000);

    refreshAll();
}

// ─── Hyprland dispatch → Android ──────────────────────────────────────────

void AndroidWindowBridge::dispatch(const QString& request) {
    qDebug() << "[Bridge] dispatch:" << request;

#ifdef Q_OS_ANDROID
    if (request.startsWith("workspace ")) {
        bool ok;
        int wsId = request.mid(10).toInt(&ok);
        if (ok) setCurrentWorkspace(wsId);
    } else if (request == "killactive") {
        runAsRoot("am force-stop $(am stack list | grep mFocused=true | awk '{print $2}')");
    } else if (request.startsWith("fullscreen")) {
        // Android: use immersive mode via root
        runAsRoot("wm overscan 0,0,0,0");
    } else if (request.startsWith("movetoworkspace ")) {
        // Move focused task to workspace N
        const QStringList parts = request.split(" ");
        if (parts.size() >= 2) moveTaskToWorkspace(parts[1].toInt(), -1);
    } else if (request.startsWith("float")) {
        emit fullscreenRequested();
    }
#else
    Q_UNUSED(request)
#endif
}

// ─── Task/window enumeration ──────────────────────────────────────────────

void AndroidWindowBridge::refreshToplevels() {
#ifdef Q_OS_ANDROID
    QJniObject activityManager = getSystemService("activity");
    if (!activityManager.isValid()) return;

    // getRunningTasks(max) — requires GET_TASKS permission (granted via root)
    QJniObject tasks = activityManager.callObjectMethod(
        "getRunningTasks",
        "(I)Ljava/util/List;",
        (jint)50
    );

    m_toplevelModel->clear();
    if (!tasks.isValid()) return;

    jint size = tasks.callMethod<jint>("size");
    for (jint i = 0; i < size; i++) {
        QJniObject task = tasks.callObjectMethod(
            "get", "(I)Ljava/lang/Object;", i);
        if (!task.isValid()) continue;

        QJniObject topActivity = task.getObjectField(
            "topActivity", "Landroid/content/ComponentName;");
        QJniObject packageName = topActivity.callObjectMethod<jstring>("getPackageName");

        auto* item = new QStandardItem(packageName.toString());
        item->setData(i == 0, Qt::UserRole);  // first = focused
        m_toplevelModel->appendRow(item);
    }

    emit taskChanged("openwindow", QVariantMap());
#endif
}

void AndroidWindowBridge::refreshWorkspaces() {
    // Workspaces are virtual — just re-emit
    emit taskChanged("workspace", QVariantMap());
}

void AndroidWindowBridge::refreshMonitors() {
    emit taskChanged("focusedmon", QVariantMap());
}

void AndroidWindowBridge::refreshAll() {
    refreshToplevels();
    refreshWorkspaces();
    refreshMonitors();
}

// ─── Environment variable bridge ──────────────────────────────────────────

QString AndroidWindowBridge::getEnv(const QString& name) const {
#ifdef Q_OS_ANDROID
    // First check Android-specific overrides stored in SharedPreferences
    QJniObject context = QtAndroidPrivate::androidContext();
    QJniObject prefs = context.callObjectMethod(
        "getSharedPreferences",
        "(Ljava/lang/String;I)Landroid/content/SharedPreferences;",
        QJniObject::fromString("rearch_env").object<jstring>(),
        (jint)0
    );
    if (prefs.isValid()) {
        QJniObject val = prefs.callObjectMethod(
            "getString",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            QJniObject::fromString(name).object<jstring>(),
            QJniObject::fromString("").object<jstring>()
        );
        const QString result = val.toString();
        if (!result.isEmpty()) return result;
    }

    // Fall back to system environment
    QJniObject sysEnv = QJniObject::callStaticObjectMethod(
        "java/lang/System", "getenv",
        "(Ljava/lang/String;)Ljava/lang/String;",
        QJniObject::fromString(name).object<jstring>()
    );
    return sysEnv.isValid() ? sysEnv.toString() : QString();
#else
    return qEnvironmentVariable(name.toUtf8());
#endif
}

QString AndroidWindowBridge::filesDir() const {
#ifdef Q_OS_ANDROID
    QJniObject context = QtAndroidPrivate::androidContext();
    QJniObject filesDir = context.callObjectMethod<jobject>("getFilesDir");
    QJniObject path = filesDir.callObjectMethod<jstring>("getAbsolutePath");
    return path.toString();
#else
    return QDir::homePath();
#endif
}

// ─── Workspace management ──────────────────────────────────────────────────

void AndroidWindowBridge::setCurrentWorkspace(int index) {
    m_currentWorkspace = index;
    emit workspaceChanged(index);
    emit taskChanged("workspace", QVariantMap{{"id", index}});
}

void AndroidWindowBridge::moveTaskToWorkspace(int workspace, int taskId) {
    Q_UNUSED(workspace)
    Q_UNUSED(taskId)
    // Root operation: am stack move-task <taskId> <stack>
}

// ─── Root shell ────────────────────────────────────────────────────────────

void AndroidWindowBridge::runAsRoot(const QString& cmd) const {
#ifdef Q_OS_ANDROID
    QProcess p;
    p.start("su", {"-c", cmd});
    p.waitForFinished(3000);
    qDebug() << "[Root]" << cmd << "→" << p.exitCode();
#else
    qDebug() << "[Root stub]" << cmd;
#endif
}

#ifdef Q_OS_ANDROID
QJniObject AndroidWindowBridge::getSystemService(const QString& name) const {
    QJniObject context = QtAndroidPrivate::androidContext();
    return context.callObjectMethod(
        "getSystemService",
        "(Ljava/lang/String;)Ljava/lang/Object;",
        QJniObject::fromString(name).object<jstring>()
    );
}
#endif
