package com.rearch.launcher.services

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.rearch.launcher.model.MediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Listens to active media sessions and exposes a MediaState flow
 * consumed by the Dashboard Media widget.
 */
class MediaNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaNotifService"
        var instance: MediaNotificationService? = null
            private set

        private val _mediaState = MutableStateFlow(MediaState())
        val mediaState: StateFlow<MediaState> = _mediaState
    }

    private var activeController: MediaController? = null

    private val sessionCallback = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
            updateActiveSession(controllers)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            msm.addOnActiveSessionsChangedListener(sessionCallback, componentName)
            updateActiveSession(msm.getActiveSessions(componentName))
        } catch (e: Exception) {
            Log.e(TAG, "Session listener error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        activeController?.unregisterCallback(controllerCallback)
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()
        activeController?.registerCallback(controllerCallback)
        pushState(activeController)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { pushState(activeController) }
        override fun onPlaybackStateChanged(state: PlaybackState?)   { pushState(activeController) }
    }

    private fun pushState(controller: MediaController?) {
        if (controller == null) {
            _mediaState.value = MediaState()
            return
        }
        val meta  = controller.metadata
        val state = controller.playbackState
        _mediaState.value = MediaState(
            title        = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)  ?: "",
            artist       = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            album        = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)  ?: "",
            albumArtUri  = null, // bitmap would need conversion; omit for now
            isPlaying    = state?.state == PlaybackState.STATE_PLAYING,
            position     = state?.position ?: 0L,
            duration     = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            sources      = listOf(controller.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }),
            controller   = controller
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?)  { /* handled via session callback */ }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { /* handled via session callback */ }
}
