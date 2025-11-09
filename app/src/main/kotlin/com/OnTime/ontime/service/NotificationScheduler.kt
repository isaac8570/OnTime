package com.OnTime.ontime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            departureTime,
            pendingIntent
        )
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
