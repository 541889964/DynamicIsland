package com.island.dynamic.service
import android.app.*
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
class DownloadService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(4, buildNotification()); return START_NOT_STICKY }
    private fun buildNotification(): Notification {
        val chId = "island_download"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(chId, "下载", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, chId)
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("灵动岛下载器运行中").build()
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
