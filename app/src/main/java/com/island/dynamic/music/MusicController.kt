package com.island.dynamic.music
import android.content.Context
import com.island.dynamic.model.*
import kotlinx.coroutines.*
object MusicController {
    private var service: com.island.dynamic.service.MusicPlaybackService? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    fun bind(svc: com.island.dynamic.service.MusicPlaybackService) { service = svc }
    fun unbind() { service = null }
    fun playSong(song: Song) { service?.playSong(song) }
    fun togglePlayPause() { service?.togglePlayPause() }
    fun playNext() { service?.playNext() }
    fun playPrev() { service?.playPrev() }
    fun cyclePlayMode() { service?.cyclePlayMode() }
    fun searchAndPlayFirst(keyword: String, limit: Int, onResult: (List<Song>) -> Unit = {}) {
        scope.launch {
            MusicStateHolder.isSearching = true
            val results = NeteaseApi.search(keyword, limit)
            MusicStateHolder.searchResults.clear(); MusicStateHolder.searchResults.addAll(results)
            MusicStateHolder.isSearching = false
            if (results.isNotEmpty()) {
                MusicStateHolder.playQueue.clear(); MusicStateHolder.playQueue.addAll(results)
                MusicStateHolder.currentIndex = 0
                MusicStateHolder.currentSong = results[0]
                service?.playSong(results[0])
            }
            onResult(results)
        }
    }
    fun scanLocalMusic(context: Context, onResult: (List<Song>) -> Unit = {}) {
        scope.launch {
            MusicStateHolder.isScanningLocal = true
            MusicStateHolder.scanProgress = 0f
            val songs = LocalMusicScanner.scan(context)
            MusicStateHolder.localMusic.clear(); MusicStateHolder.localMusic.addAll(songs)
            MusicStateHolder.isScanningLocal = false
            onResult(songs)
        }
    }
    fun playLocalSong(song: Song, from: List<Song>) {
        MusicStateHolder.playQueue.clear(); MusicStateHolder.playQueue.addAll(from)
        MusicStateHolder.currentIndex = from.indexOf(song).coerceAtLeast(0)
        MusicStateHolder.currentSong = song
        service?.playSong(song)
    }
}
