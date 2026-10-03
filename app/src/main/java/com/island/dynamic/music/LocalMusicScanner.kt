package com.island.dynamic.music
import android.content.Context
import android.provider.MediaStore
import com.island.dynamic.model.MusicStateHolder
import com.island.dynamic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
object LocalMusicScanner {
    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC")?.use { c ->
            val total = c.count.coerceAtLeast(1); var i = 0
            val idC = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val tiC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val arC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val alC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val duC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val daC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) {
                val title = c.getString(tiC) ?: continue
                val artist = c.getString(arC) ?: "未知艺术家"
                val path = c.getString(daC) ?: continue
                songs.add(Song(c.getLong(idC), title, if (artist == "<unknown>") "未知艺术家" else artist,
                    c.getString(alC) ?: "", duration = c.getLong(duC), playUrl = "file://$path", isLocal = true))
                i++; if (i % 10 == 0) MusicStateHolder.scanProgress = i.toFloat() / total
            }
        }
        MusicStateHolder.scanProgress = 1f
        songs
    }
}
