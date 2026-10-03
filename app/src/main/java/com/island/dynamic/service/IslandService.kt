package com.island.dynamic.service
import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import androidx.compose.ui.platform.ComposeView
import com.island.dynamic.receiver.BatteryMonitor
import com.island.dynamic.ui.IslandComposeContent
import com.island.dynamic.weather.WeatherFetcher
import kotlinx.coroutines.*
class IslandService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var islandView: View
    private lateinit var batteryMonitor: BatteryMonitor
    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addIslandOverlay()
        TimeTicker(scope).start(); WeatherFetcher(scope).start()
        batteryMonitor = BatteryMonitor(this).also { it.register() }
    }
    private fun addIslandOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
        islandView = ComposeView(this).apply { setContent { IslandComposeContent() } }
        windowManager.addView(islandView, params)
    }
    private fun buildNotification(): Notification {
        val chId = "island_fg"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(chId, "灵动岛常驻", NotificationManager.IMPORTANCE_MIN))
        return Notification.Builder(this, chId)
            .setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("灵动岛运行中").build()
    }
    override fun onDestroy() {
        scope.cancel(); batteryMonitor.unregister()
        runCatching { windowManager.removeView(islandView) }; super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
