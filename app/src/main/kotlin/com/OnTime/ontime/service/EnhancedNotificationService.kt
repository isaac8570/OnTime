package com.OnTime.ontime.service

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent

class EnhancedNotificationService(private val context: Context) {
    
    private val firebaseRAG = FirebaseRAGService()
    private val notificationManager = NotificationManager(context)
    
    // RAG 기반 스마트 알림 발송
    suspend fun sendRAGNotification(
        event: CalendarEvent,
        travelTime: String,
        weatherCondition: String?
    ) {
        val ragMessage = firebaseRAG.generateRAGNotification(
            event = event,
            travelTime = travelTime,
            weatherCondition = weatherCondition
        )
        
        notificationManager.showDepartureNotification(
            title = "🧠 AI 맞춤 알림",
            message = ragMessage,
            eventId = event.id
        )
    }
    
    // 사용자 경험 학습
    suspend fun learnFromExperience(
        event: CalendarEvent,
        travelTime: String,
        actualDepartureTime: Long,
        wasLate: Boolean,
        weatherCondition: String?
    ) {
        firebaseRAG.storeUserExperience(
            event = event,
            travelTime = travelTime,
            actualDepartureTime = actualDepartureTime,
            wasLate = wasLate,
            weatherCondition = weatherCondition
        )
    }
}
