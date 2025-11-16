package com.OnTime.ontime.service

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent

class SmartNotificationService(private val context: Context) {
    
    private val ragService = RAGService(context)
    private val notificationManager = NotificationManager(context)
    
    // RAG 기반 스마트 알림 발송
    suspend fun sendSmartNotification(
        event: CalendarEvent,
        travelTime: String,
        weatherInfo: String? = null
    ) {
        val smartMessage = ragService.generateSmartNotification(
            event = event,
            travelTime = travelTime,
            weatherInfo = weatherInfo
        )
        
        notificationManager.showDepartureNotification(
            title = "🤖 AI 출발 알림",
            message = smartMessage,
            eventId = event.id
        )
    }
    
    // 사용자 피드백 수집 (출발했는지 확인)
    fun recordUserFeedback(event: CalendarEvent, departed: Boolean, departureTime: Long) {
        val wasLate = departureTime > (event.startTime - 30 * 60 * 1000) // 30분 전 기준
        
        ragService.saveUserPattern(
            event = event,
            actualDepartureTime = departureTime,
            wasLate = wasLate
        )
    }
}
