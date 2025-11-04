package com.OnTime.ontime.data.models

data class CalendarEvent(
    val id: String,
    val title: String,
    val location: String?,
    val startTime: Long,
    val endTime: Long,
    val description: String?
)
