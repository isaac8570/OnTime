package com.OnTime.ontime.data.models

data class UserPreferences(
    val transportMode: TransportMode = TransportMode.TRANSIT,
    val notificationTone: NotificationTone = NotificationTone.FRIENDLY,
    val notificationCount: Int = 2 // 알림 횟수 추가
)

enum class TransportMode {
    TRANSIT,
    DRIVING,
    WALKING
}

/**
 * 알림 메시지의 톤을 나타내는 열거형 클래스입니다.
 */
enum class NotificationTone(val description: String) {
    FRIENDLY("친근하고 부드러운 말투"),
    FORMAL("격식 있고 명확한 말투"),
    CONCISE("핵심만 간결하게 전달하는 말투"),
    HUMOROUS("재치와 유머를 섞은 말투")
}