package com.OnTime.ontime.data.models

data class TravelInfo(
    val durationMinutes: Int,
    val durationText: String,
    val distanceText: String,
    val routeDetails: String?,
    val mode: String
)
