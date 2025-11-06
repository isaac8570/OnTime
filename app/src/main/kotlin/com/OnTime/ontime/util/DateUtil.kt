package com.OnTime.ontime.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    fun formatDateTime(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    fun calculateDepartureTime(eventTime: Long, travelDurationSeconds: Int): Long {
        return eventTime - (travelDurationSeconds * 1000L)
    }

    fun getKmaApiBaseTime(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // KMA API base times (short-term forecast)
        val baseTimes = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")

        var baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
        var baseTime = "0000"

        // Find the latest base time that has passed
        for (i in baseTimes.indices.reversed()) {
            val hour = baseTimes[i].substring(0, 2).toInt()
            val minute = baseTimes[i].substring(2, 4).toInt()

            if (currentHour > hour || (currentHour == hour && currentMinute >= minute + 10)) { // +10 minutes buffer
                baseTime = baseTimes[i]
                break
            }
        }

        // If no base time found for today (e.g., before 0200), use yesterday's last base time
        if (baseTime == "0000") {
            calendar.add(Calendar.DATE, -1)
            baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            baseTime = "2300" // Last base time of previous day
        }
        return Pair(baseDate, baseTime)
    }
}
