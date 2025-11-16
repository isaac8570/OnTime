package com.OnTime.ontime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra("event_title") ?: return
        val eventLocation = intent.getStringExtra("event_location") ?: return
        val travelTime = intent.getIntExtra("travel_time", 0)
        val eventId = intent.getStringExtra("event_id") ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val notificationManager = NotificationManager(context)
            
            val message = "$eventTitle 일정을 위해 지금 출발하세요! (예상 소요시간: ${travelTime}분)"
            
            notificationManager.showDepartureNotification(
                title = "출발 시간입니다!",
                message = message,
                eventId = eventId
            )
            
            // 반복 알림 스케줄링
            scheduleFollowUpNotifications(context, eventId, eventTitle, travelTime)
        }
    }

    private fun scheduleFollowUpNotifications(
        context: Context,
        eventId: String,
        eventTitle: String,
        travelTime: Int
    ) {
        // 5분 후 재알림
        val followUpIntent = Intent(context, FollowUpNotificationReceiver::class.java).apply {
            putExtra("event_title", eventTitle)
            putExtra("event_id", eventId)
            putExtra("travel_time", travelTime)
        }
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            "${eventId}_followup".hashCode(),
            followUpIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + (5 * 60 * 1000), // 5분 후
            pendingIntent
        )
    }
}
