package com.OnTime.ontime.data.models

data class UserPreferences(
    val transportMode: TransportMode = TransportMode.TRANSIT,
    val notificationTone: NotificationTone = NotificationTone.SOFT
)

enum class TransportMode {
    TRANSIT,
    DRIVING,
    WALKING
}

enum class NotificationTone {
    STRONG,
    SOFT
}
