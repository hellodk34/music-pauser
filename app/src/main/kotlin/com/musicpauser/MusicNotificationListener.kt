package com.musicpauser

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MusicNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        PlaybackManager.setContext(this)
        PlaybackManager.clear()
        val active = activeNotifications
        for (sbn in active) {
            PlaybackManager.onNotificationPosted(this, sbn)
        }
        PlaybackManager.recompute(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        PlaybackManager.clear()
        PlaybackManager.recompute(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null) PlaybackManager.onNotificationPosted(this, sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) PlaybackManager.onNotificationRemoved(this, sbn)
    }
}
