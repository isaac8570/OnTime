package com.OnTime.ontime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.OnTime.ontime.NotificationBuilder // Import NotificationBuilder
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.SettingsRepository // Import SettingsRepository
import kotlinx.coroutines.runBlocking // For calling suspend functions
import java.time.LocalTime // Required for NotificationBuilder
import java.time.ZoneId // Required for LocalTime conversion
import java.time.Instant // Required for LocalTime conversion


class NotificationScheduler(
    private val context: Context,
    private val notificationBuilder: NotificationBuilder, // Add NotificationBuilder dependency
    private val settingsRepository: SettingsRepository // Add SettingsRepository dependency
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNotification( // Changed to suspend and return String?
        event: CalendarEvent,
        departureTime: Long,
        estimatedTravelTimeMinutes: Int, // This is googleEtaMin
        predictedActualTravelTimeMinutes: Int?, // Predicted actual travel time from model/tracking
        weatherInfo: String // Add weatherInfo parameter
    ): String? { // Return String?

        // Fetch user preferences
        val userPreferences = settingsRepository.getUserPreferences() // Call directly, not runBlocking

        // Generate personalized message using NotificationBuilder
        val notificationMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.generateNotificationMessage(
                userPreferences = userPreferences,
                eventName = event.title,
                eventTime = LocalTime.ofInstant(Instant.ofEpochMilli(event.startTime), ZoneId.systemDefault()), // Convert Long to LocalTime
                travelTime = estimatedTravelTimeMinutes,
                actualTravelTime = predictedActualTravelTimeMinutes,
                weatherInfo = weatherInfo, // Use the passed weatherInfo
                userPattern = "과거 패턴 정보가 있을 경우 여기에 추가" // TODO: Integrate actual user pattern logic
            )
        } else {
            "일정에 늦지 않도록 준비하세요!"
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_location", event.location)
            putExtra("travel_time", estimatedTravelTimeMinutes)
            putExtra("event_id", event.id)
            putExtra("notification_message", notificationMessage ?: "일정에 늦지 않도록 준비하세요!") // Pass generated message
            // Add notification count from user preferences
            putExtra("notification_count", userPreferences.notificationCount)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12 (S, API 31) 이상인 경우 권한 확인 로직을 추가합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 정확한 알람을 설정할 수 있는 권한이 있는지 확인합니다.
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    departureTime - (10 * 60 * 1000), // 10 minutes before departure
                    pendingIntent
                )
            } else {
                // 권한이 없을 경우, 덜 정확한 알람으로 대체하여 앱의 비정상 종료를 방지합니다.
                // 또는 사용자에게 설정 화면으로 안내하여 권한을 요청할 수 있습니다.
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    departureTime - (10 * 60 * 1000), // 10분 전부터
                    10 * 60 * 1000, // 10분 이내에 알람을 실행
                    pendingIntent
                )
            }
        } else {
            // Android 12 미만 버전에서는 기존 방식대로 즉시 설정합니다.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                departureTime - (10 * 60 * 1000), // 10 minutes before departure
                pendingIntent
            )
        }
        return notificationMessage // Return the generated message
    }

    fun cancelNotification(eventId: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    /**
     * For testing purposes, generates and shows a notification immediately.
     */
    suspend fun sendInstantTestNotification() {
        val testEventName = "성수역에서 친구와 약속"
        val testTravelTime = 25 // 25 minutes
        val testEventTime = LocalTime.now().plusMinutes(testTravelTime.toLong() + 10) // Event is in 35 mins, departure in 10 mins

        // Fetch user preferences for message tone
        val userPreferences = settingsRepository.getUserPreferences()

        // Generate the notification message with Gemini
        val notificationMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.generateNotificationMessage(
                userPreferences = userPreferences,
                eventName = testEventName,
                eventTime = testEventTime,
                travelTime = testTravelTime,
                actualTravelTime = null, // No actual travel time for test
                weatherInfo = "맑음", // Mock weather
                userPattern = "테스트 시나리오"
            )
        } else {
            "$testEventName 약속 10분 전입니다! 지금 출발하세요."
        }

        // Show the notification immediately
        if (notificationMessage != null) {
            val notificationManager = NotificationManager(context)
            notificationManager.showDepartureNotification(
                title = "📲 출발 시간 알림 (테스트)",
                message = notificationMessage,
                eventId = "test_notification_${System.currentTimeMillis()}"
            )
        }
    }
}