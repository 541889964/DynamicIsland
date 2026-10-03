package com.island.dynamic.service
import android.app.*
import android.content.Intent
import android.hardware.display.*
import android.media.*
import android.media.projection.*
import android.os.*
import com.island.dynamic.model.*
import java.io.File
class ScreenRecordService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, buildNotification())
        if (intent?.action == "STOP") { stopRecording(); return START_NOT_STICKY }
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        @Suppress("DEPRECATION") val data = intent?.getParcelableExtra<Intent>("data")
        if (resultCode != -1 && data != null) startRecording(resultCode, data)
        return START_NOT_STICKY
    }
    private fun startRecording(resultCode: Int, data: Intent) {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, data)
        val file = File(getExternalFilesDir(null), "island_${System.currentTimeMillis()}.mp4")
        recorder = MediaRecorder(this).apply {
            setVideoSource(3)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath); prepare() }
        val m = resources.displayMetrics
        virtualDisplay = projection?.createVirtualDisplay("IslandRec",
            m.widthPixels, m.heightPixels, m.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recorder!!.surface, null, null)
        recorder?.start()
        IslandStateHolder.isRecording = true; IslandStateHolder.mode = IslandMode.RECORDING
    }
    private fun stopRecording() {
        runCatching { recorder?.stop(); recorder?.release()
            virtualDisplay?.release(); projection?.stop() }
        IslandStateHolder.isRecording = false; IslandStateHolder.mode = IslandMode.IDLE
        stopSelf()
    }
    private fun buildNotification(): Notification {
        val chId = "island_rec"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(chId, "录屏", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, chId)
            .setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("屏幕录制中…").build()
    }
    override fun onDestroy() { stopRecording(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
