package com.OnTime.ontime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build // Build 클래스를 import 합니다.
import com.OnTime.ontime.data.models.CalendarEvent

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(
        event: CalendarEvent,
        departureTime: Long,
        travelTimeMinutes: Int
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_location", event.location)
            putExtra("travel_time", travelTimeMinutes)
            putExtra("event_id", event.id)
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
                    departureTime,
                    pendingIntent
                )
            } else {
                // 권한이 없을 경우, 덜 정확한 알람으로 대체하여 앱의 비정상 종료를 방지합니다.
                // 또는 사용자에게 설정 화면으로 안내하여 권한을 요청할 수 있습니다.
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    departureTime - (5 * 60 * 1000), // 5분 전부터
                    10 * 60 * 1000, // 10분 이내에 알람을 실행
                    pendingIntent
                )
            }
        } else {
            // Android 12 미만 버전에서는 기존 방식대로 즉시 설정합니다.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                departureTime,
                pendingIntent
            )
        }
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
}
