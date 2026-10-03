package com.island.dynamic.service
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.island.dynamic.model.*
import com.island.dynamic.music.*
import com.island.dynamic.settings.SettingsStore
import kotlinx.coroutines.*
class MusicPlaybackService : Service() {
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoSwitchJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        startForeground(3, buildNotification())
        player = ExoPlayer.Builder(this).build()
        MusicController.bind(this)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) onSongEnded() }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                MusicStateHolder.isPlaying = isPlaying
                if (isPlaying) { autoSwitchJob?.cancel()
                    if (IslandStateHolder.appMode == AppMode.MUSIC) IslandStateHolder.mode = IslandMode.MUSIC_CONTROL
                } else scheduleAutoSwitch()
            }
        })
        scope.launch { while (isActive) {
            MusicStateHolder.currentPosition = player.currentPosition
            MusicStateHolder.duration = player.duration.coerceAtLeast(0)
            delay(500) } }
    }
    private fun scheduleAutoSwitch() {
        autoSwitchJob?.cancel()
        autoSwitchJob = scope.launch {
            delay(SettingsStore.autoSwitchSec * 1000L)
            if (!MusicStateHolder.isPlaying) { IslandStateHolder.appMode = AppMode.LIFE; IslandStateHolder.mode = IslandMode.IDLE }
        }
    }
    fun playSong(song: Song) { scope.launch {
        val url = if (song.isLocal) song.playUrl else NeteaseApi.getSongUrl(song.id).ifBlank { return@launch }
        MusicStateHolder.currentSong = song.copy(playUrl = url)
        player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play()
    } }
    fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }
    fun playNext() { MusicStateHolder.advanceIndex(); MusicStateHolder.currentSong?.let { playSong(it) } }
    fun playPrev() {
        if (player.currentPosition > 3000) { player.seekTo(0); return }
        if (MusicStateHolder.currentIndex > 0) {
            MusicStateHolder.currentIndex--
            MusicStateHolder.currentSong = MusicStateHolder.playQueue[MusicStateHolder.currentIndex]
            MusicStateHolder.currentSong?.let { playSong(it) }
        }
    }
    fun cyclePlayMode() {
        MusicStateHolder.playMode = when (MusicStateHolder.playMode) {
            PlayMode.SEQUENTIAL -> PlayMode.LIST_LOOP
            PlayMode.LIST_LOOP -> PlayMode.SINGLE_LOOP
            PlayMode.SINGLE_LOOP -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SEQUENTIAL }
        player.repeatMode = when (MusicStateHolder.playMode) {
            PlayMode.SINGLE_LOOP -> Player.REPEAT_MODE_ONE
            PlayMode.LIST_LOOP -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF }
        player.shuffleModeEnabled = MusicStateHolder.playMode == PlayMode.SHUFFLE
    }
    private fun onSongEnded() {
        when (MusicStateHolder.playMode) {
            PlayMode.SINGLE_LOOP -> { player.seekTo(0); player.play() }
            else -> playNext() } }
    private fun buildNotification(): Notification {
        val chId = "island_music"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(chId, "音乐播放", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, chId)
            .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("灵动岛音乐").build() }
    override fun onDestroy() { autoSwitchJob?.cancel(); MusicController.unbind(); scope.cancel(); player.release(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
