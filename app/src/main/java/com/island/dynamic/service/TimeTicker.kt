package com.island.dynamic.service
import com.island.dynamic.model.IslandStateHolder
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
class TimeTicker(private val scope: CoroutineScope) {
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dayFmt = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINESE)
    fun start() { scope.launch { while (isActive) {
        val now = Date()
        IslandStateHolder.currentTime = timeFmt.format(now)
        IslandStateHolder.calendarDay = dayFmt.format(now)
        delay(1000L - System.currentTimeMillis() % 1000) } } }
}
