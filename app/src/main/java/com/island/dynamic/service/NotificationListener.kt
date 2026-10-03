package com.island.dynamic.service
import android.service.notification.*
import com.island.dynamic.model.*
class NotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (IslandStateHolder.appMode == AppMode.LIFE) {
            IslandStateHolder.mode = IslandMode.NOTIFICATION
            IslandStateHolder.title = title; IslandStateHolder.subtitle = text
        }
    }
}
