package com.OnTime.ontime.data.models

data class TravelLog(
    val userId: String = "",
    val eventId: String? = null,
    val eventTitle: String? = null,
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val googleEtaMin: Int = 0,
    val actualEtaMin: Int = 0,
    val weather: String = "",
    val hourOfDay: Int = 0,
    val dayOfWeek: Int = 0, // 1 for Monday, 7 for Sunday (as per ML script)
    val distanceKm: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis() // Timestamp of when the log was created
)
