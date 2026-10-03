package com.island.dynamic.music
import com.google.gson.JsonParser
import com.island.dynamic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
object NeteaseApi {
    private const val BASE = "http://localhost:3000"
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    suspend fun search(keyword: String, limit: Int = 20): List<Song> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$BASE/search?keywords=${java.net.URLEncoder.encode(keyword, "UTF-8")}&type=1&limit=$limit"
            val body = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: return@runCatching emptyList()
            val result = JsonParser.parseString(body).asJsonObject["result"].asJsonObject
            result["songs"].asJsonArray.map { el ->
                val obj = el.asJsonObject
                val artists = obj["artists"].asJsonArray.joinToString("/") { it.asJsonObject["name"].asString }
                val album = obj["album"].asJsonObject
                Song(obj["id"].asLong(), obj["name"].asString, artists,
                    album["name"].asString, album["picUrl"]?.asString ?: "", obj["duration"].asLong)
            }
        }.getOrDefault(emptyList())
    }
    suspend fun getSongUrl(id: Long): String = withContext(Dispatchers.IO) {
        runCatching {
            val body = client.newCall(Request.Builder().url("$BASE/song/url/v1?id=$id&level=standard").build())
                .execute().body?.string() ?: return@runCatching ""
            JsonParser.parseString(body).asJsonObject["data"].asJsonArray[0].asJsonObject["url"]?.asString ?: ""
        }.getOrDefault("")
    }
}
