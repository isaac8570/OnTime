package com.OnTime.ontime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FollowUpNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra("event_title") ?: return
        val eventId = intent.getStringExtra("event_id") ?: return
        val travelTime = intent.getIntExtra("travel_time", 0)

        val notificationManager = NotificationManager(context)
        
        notificationManager.showDepartureNotification(
            title = "아직 출발 안 하셨나요?",
            message = "$eventTitle 일정까지 ${travelTime}분 남았습니다. 지금 출발하세요!",
            eventId = "${eventId}_followup"
        )
    }
}
