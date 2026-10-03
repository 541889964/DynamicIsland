package com.island.dynamic.download
import android.content.Context
import com.island.dynamic.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

object MultiPartDownloader {
    private const val THREADS = 16
    private const val MAX_RETRY = 3
    private const val BUFFER_SIZE = 64 * 1024
    private const val SPEED_WINDOW_MS = 1000L
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true).build()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, Boolean>()

    fun download(context: Context, task: DownloadTask, scope: CoroutineScope,
        onProgress: (DownloadTask) -> Unit, onComplete: (DownloadTask, Boolean) -> Unit) {
        if (downloadJobs.containsKey(task.id)) return
        val job = scope.launch(Dispatchers.IO) {
            try {
                val targetFile = File(context.getExternalFilesDir(null) ?: context.filesDir, task.fileName)
                val headResp = client.newCall(Request.Builder().url(task.url).head().build()).execute()
                val totalSize = headResp.header("Content-Length")?.toLongOrNull() ?: 0L
                val acceptRanges = headResp.header("Accept-Ranges")?.contains("bytes") ?: false
                if (totalSize <= 0) { downloadSingle(task, targetFile, onProgress, onComplete); return@launch }
                DownloadStateHolder.updateTask(task.id) {
                    it.copy(totalSize = totalSize, status = DownloadStatus.DOWNLOADING,
                        savePath = targetFile.absolutePath, threads = if (acceptRanges) THREADS else 1)
                }
                val actualThreads = if (acceptRanges) THREADS else 1
                val chunkSize = totalSize / actualThreads
                val downloaded = AtomicLong(0L)
                RandomAccessFile(targetFile, "rw").use { it.setLength(totalSize) }
                val speedJob = scope.launch(Dispatchers.IO) {
                    var lastBytes = 0L
                    while (isActive) {
                        delay(SPEED_WINDOW_MS)
                        val cur = downloaded.get(); val speed = cur - lastBytes; lastBytes = cur
                        DownloadStateHolder.updateTask(task.id) { it.copy(downloadedSize = cur, speed = speed) }
                        DownloadStateHolder.totalSpeed = speed
                        onProgress(DownloadStateHolder.tasks.find { it.id == task.id } ?: task)
                    }
                }
                val errors = Channel<Throwable>(Channel.UNLIMITED)
                val parts = (0 until actualThreads).map { i ->
                    val start = i * chunkSize
                    val end = if (i == actualThreads - 1) totalSize - 1 else start + chunkSize - 1
                    async(Dispatchers.IO) { downloadPart(task, targetFile, start, end, downloaded, errors) }
                }
                try { parts.awaitAll() } finally { speedJob.cancel() }
                var hasError = false
                while (!errors.isEmpty) { errors.tryReceive().getOrNull()?.let { hasError = true } }
                if (hasError) {
                    DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.FAILED, errorMessage = "部分分块下载失败") }
                    onComplete(task, false)
                } else {
                    DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.COMPLETED, downloadedSize = totalSize, speed = 0L) }
                    onComplete(task, true)
                }
            } catch (e: CancellationException) {
                DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.PAUSED) }
                throw e
            } catch (e: Exception) {
                DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.FAILED, errorMessage = e.message ?: "未知错误") }
                onComplete(task, false)
            } finally { downloadJobs.remove(task.id); pauseFlags.remove(task.id) }
        }
        downloadJobs[task.id] = job
    }

    private suspend fun downloadPart(task: DownloadTask, file: File, start: Long, end: Long,
        downloaded: AtomicLong, errors: Channel<Throwable>) {
        var attempt = 0; var currentStart = start
        while (attempt < MAX_RETRY) {
            if (pauseFlags[task.id] == true) throw CancellationException("paused")
            try {
                val req = Request.Builder().url(task.url).header("Range", "bytes=$currentStart-$end").build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful && resp.code != 206) throw RuntimeException("HTTP ${resp.code}")
                    val body = resp.body ?: throw RuntimeException("空响应")
                    val input = body.byteStream()
                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(currentStart)
                        val buf = ByteArray(BUFFER_SIZE); var len: Int
                        while (input.read(buf).also { len = it } != -1) {
                            if (pauseFlags[task.id] == true) throw CancellationException("paused")
                            raf.write(buf, 0, len); currentStart += len; downloaded.addAndGet(len.toLong())
                        }
                    }
                }
                return
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                attempt++
                if (attempt >= MAX_RETRY) { errors.trySend(e); return }
                delay(1000L * attempt)
            }
        }
    }

    private suspend fun downloadSingle(task: DownloadTask, targetFile: File,
        onProgress: (DownloadTask) -> Unit, onComplete: (DownloadTask, Boolean) -> Unit) {
        try {
            val resp = client.newCall(Request.Builder().url(task.url).build()).execute()
            val body = resp.body ?: throw RuntimeException("空响应")
            val total = body.contentLength()
            DownloadStateHolder.updateTask(task.id) { it.copy(totalSize = total, status = DownloadStatus.DOWNLOADING, threads = 1) }
            var downloaded = 0L
            val startTime = System.currentTimeMillis()
            targetFile.outputStream().use { out ->
                val buf = ByteArray(BUFFER_SIZE); var len: Int
                body.byteStream().also { input ->
                    while (input.read(buf).also { len = it } != -1) {
                        out.write(buf, 0, len); downloaded += len
                        val elapsed = max(1, System.currentTimeMillis() - startTime)
                        DownloadStateHolder.updateTask(task.id) { it.copy(downloadedSize = downloaded, speed = downloaded * 1000 / elapsed) }
                    }
                }
            }
            DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.COMPLETED, downloadedSize = total, speed = 0L) }
            onComplete(task, true)
        } catch (e: Exception) {
            DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.FAILED, errorMessage = e.message ?: "") }
            onComplete(task, false)
        }
    }

    fun pause(taskId: String) {
        pauseFlags[taskId] = true
        downloadJobs[taskId]?.cancel()
        DownloadStateHolder.updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED) }
    }
}
