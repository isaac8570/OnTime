package com.OnTime.ontime.data.models

import com.OnTime.ontime.api.WeatherItem

data class CalendarEvent(
    val id: String,
    val title: String,
    val location: String?,
    val startTime: Long,
    val endTime: Long,
    val description: String?,
    var weatherInfo: List<WeatherItem>? = null, // Add weather information
    var travelDuration: String? = null,
    var originLatLng: Pair<Double, Double>? = null,
    var destinationLatLng: Pair<Double, Double>? = null
)
