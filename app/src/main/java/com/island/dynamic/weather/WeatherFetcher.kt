package com.island.dynamic.weather
import com.google.gson.JsonParser
import com.island.dynamic.model.IslandStateHolder
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
class WeatherFetcher(private val scope: CoroutineScope) {
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
    fun start() { scope.launch { while (isActive) { runCatching { fetch() }; delay(10 * 60 * 1000L) } } }
    private suspend fun fetch() = withContext(Dispatchers.IO) {
        val lb = client.newCall(Request.Builder().url("https://ipapi.co/json/").build()).execute().body?.string() ?: return@withContext
        val lj = JsonParser.parseString(lb).asJsonObject
        val lat = lj["latitude"].asDouble; val lon = lj["longitude"].asDouble
        val city = lj["city"]?.asString ?: lj["region"]?.asString ?: "本地"
        val wu = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timezone=auto"
        val wb = client.newCall(Request.Builder().url(wu).build()).execute().body?.string() ?: return@withContext
        val c = JsonParser.parseString(wb).asJsonObject["current"].asJsonObject
        IslandStateHolder.weatherCity = city
        IslandStateHolder.weatherTemp = "${c["temperature_2m"].asDouble.toInt()}°C"
        IslandStateHolder.weatherDesc = codeToChinese(c["weather_code"].asInt)
    }
    private fun codeToChinese(code: Int): String = when (code) {
        0 -> "晴"; 1, 2 -> "多云"; 3 -> "阴"; 45, 48 -> "雾"
        51, 53, 55 -> "毛毛雨"; 56, 57 -> "冻雨"; 61, 63, 65 -> "雨"; 66, 67 -> "冻雨"
        71, 73, 75, 77 -> "雪"; 80, 81, 82 -> "阵雨"; 85, 86 -> "阵雪"
        95 -> "雷阵雨"; 96, 99 -> "雷暴冰雹"; else -> "未知"
    }
}
