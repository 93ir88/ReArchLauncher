#pragma once

#include <QObject>
#include <QTimer>
#include <QStandardItemModel>
#include <QString>
#include <QVariantMap>
#include <qqmlintegration.h>

#ifdef Q_OS_ANDROID
#include <QJniObject>
#endif

class AndroidWindowBridge : public QObject {
    Q_OBJECT
    QML_ELEMENT
    QML_SINGLETON

    Q_PROPERTY(QStandardItemModel* toplevelModel  READ toplevelModel  CONSTANT)
    Q_PROPERTY(QStandardItemModel* workspaceModel READ workspaceModel CONSTANT)
    Q_PROPERTY(QStandardItemModel* monitorModel   READ monitorModel   CONSTANT)
    Q_PROPERTY(QString filesDir                   READ filesDir       CONSTANT)
    Q_PROPERTY(int currentWorkspace READ currentWorkspace WRITE setCurrentWorkspace NOTIFY workspaceChanged)

public:
    explicit AndroidWindowBridge(QObject* parent = nullptr);
    static AndroidWindowBridge* instance() { return s_instance; }

    QStandardItemModel* toplevelModel()  const { return m_toplevelModel; }
    QStandardItemModel* workspaceModel() const { return m_workspaceModel; }
    QStandardItemModel* monitorModel()   const { return m_monitorModel; }

    Q_INVOKABLE QString getEnv(const QString& name) const;
    Q_INVOKABLE QString filesDir() const;
    Q_INVOKABLE void dispatch(const QString& request);

    int currentWorkspace() const { return m_currentWorkspace; }
    void setCurrentWorkspace(int index);

public slots:
    void refreshToplevels();
    void refreshWorkspaces();
    void refreshMonitors();
    void refreshAll();

signals:
    void taskChanged(const QString& eventName, const QVariantMap& data);
    void workspaceChanged(int index);
    void killActiveRequested();
    void fullscreenRequested();

private:
    static AndroidWindowBridge* s_instance;

    QStandardItemModel* m_toplevelModel  = nullptr;
    QStandardItemModel* m_workspaceModel = nullptr;
    QStandardItemModel* m_monitorModel   = nullptr;
    QTimer*             m_pollTimer      = nullptr;
    int                 m_currentWorkspace = 1;

    void runAsRoot(const QString& cmd) const;
    void moveTaskToWorkspace(int workspace, int taskId);

#ifdef Q_OS_ANDROID
    QJniObject getSystemService(const QString& name) const;
#endif
};
