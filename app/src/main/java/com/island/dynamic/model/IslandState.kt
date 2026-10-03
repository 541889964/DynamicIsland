package com.island.dynamic.model
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppMode { LIFE, MUSIC, DOWNLOAD }
enum class IslandMode { IDLE, NOTIFICATION, CHARGING, RECORDING, WEATHER, CALENDAR, MUSIC_CONTROL, DOWNLOADING }
enum class PlayMode { SEQUENTIAL, SHUFFLE, SINGLE_LOOP, LIST_LOOP }
enum class SearchSource { ONLINE, LOCAL }
enum class DownloadStatus { PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED }

data class Song(val id: Long, val name: String, val artist: String,
    val album: String = "", val coverUrl: String = "",
    val duration: Long = 0L, val playUrl: String = "", val isLocal: Boolean = false)

data class DownloadTask(val id: String, val url: String, val fileName: String,
    val totalSize: Long = 0L, val downloadedSize: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val speed: Long = 0L, val threads: Int = 16,
    val errorMessage: String = "", val savePath: String = "") {
    val progress: Float get() = if (totalSize > 0) downloadedSize.toFloat() / totalSize else 0f
}

object IslandStateHolder {
    var appMode by mutableStateOf(AppMode.LIFE)
    var mode by mutableStateOf(IslandMode.IDLE)
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var batteryLevel by mutableStateOf(100)
    var isCharging by mutableStateOf(false)
    var weatherTemp by mutableStateOf("--")
    var weatherDesc by mutableStateOf("")
    var weatherCity by mutableStateOf("定位中...")
    var currentTime by mutableStateOf("")
    var calendarDay by mutableStateOf("")
    var isRecording by mutableStateOf(false)
}

object MusicStateHolder {
    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var playMode by mutableStateOf(PlayMode.SEQUENTIAL)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var isSearching by mutableStateOf(false)
    var searchLimit by mutableStateOf(20)
    val searchResults = mutableStateListOf<Song>()
    val playQueue = mutableStateListOf<Song>()
    var currentIndex by mutableStateOf(0)
    val localMusic = mutableStateListOf<Song>()
    var isScanningLocal by mutableStateOf(false)
    var scanProgress by mutableStateOf(0f)
    fun advanceIndex() {
        if (playQueue.isEmpty()) return
        currentIndex = when (playMode) {
            PlayMode.SHUFFLE -> playQueue.indices.random()
            PlayMode.SINGLE_LOOP -> currentIndex
            PlayMode.LIST_LOOP -> (currentIndex + 1) % playQueue.size
            PlayMode.SEQUENTIAL -> if (currentIndex + 1 < playQueue.size) currentIndex + 1 else 0
        }
        currentSong = playQueue.getOrNull(currentIndex)
    }
}

object DownloadStateHolder {
    val tasks = mutableStateListOf<DownloadTask>()
    var currentTaskId by mutableStateOf<String?>(null)
    var totalSpeed by mutableStateOf(0L)
    var activeThreads by mutableStateOf(0)
    fun updateTask(id: String, transform: (DownloadTask) -> DownloadTask) {
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx >= 0) tasks[idx] = transform(tasks[idx])
    }
    fun addTask(task: DownloadTask) { tasks.add(0, task) }
    fun removeTask(id: String) { tasks.removeAll { it.id == id } }
}
