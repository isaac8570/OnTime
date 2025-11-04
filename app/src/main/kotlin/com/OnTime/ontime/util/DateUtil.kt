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
}
