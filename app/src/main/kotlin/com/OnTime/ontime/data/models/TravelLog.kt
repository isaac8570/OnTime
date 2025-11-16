package com.OnTime.ontime.data.models

import java.util.Date

data class TravelLog(
    val userId: String = "", // Will be populated by repository from FirebaseAuth
    val eventId: String = "",
    val eventTitle: String = "",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val googleEtaMin: Int = 0,
    val actualEtaMin: Int = 0,
    val weather: String = "",
    val hourOfDay: Int = 0,
    val dayOfWeek: Int = 0, // 1-7 for Sunday-Saturday or Monday-Sunday, depending on locale/convention
    val distanceKm: Double = 0.0, // This needs to be calculated from LocationRepository if available
    val timestamp: Date = Date() // Firestore can handle Date objects directly
)
