package com.OnTime.ontime.service

import android.content.Context
import androidx.work.*
import com.OnTime.ontime.data.models.CalendarEvent
import java.util.concurrent.TimeUnit

class AutoLearningService(private val context: Context) {
    
    private val ragService = FirebaseRAGService()
    
    // 일정 시작 시 자동 학습 예약
    fun scheduleAutoLearning(event: CalendarEvent, travelTime: String) {
        val learningData = workDataOf(
            "eventId" to event.id,
            "eventTitle" to event.title,
            "eventLocation" to (event.location ?: ""),
            "scheduledTime" to event.startTime,
            "travelTime" to travelTime
        )
        
        // 일정 시작 10분 후에 학습 실행
        val learningWork = OneTimeWorkRequestBuilder<LearningWorker>()
            .setInputData(learningData)
            .setInitialDelay(
                (event.startTime - System.currentTimeMillis()) + 10 * 60 * 1000,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(learningWork)
    }
    
    // 실시간 위치 기반 출발 감지
    suspend fun detectDepartureAndLearn(
        event: CalendarEvent,
        currentLocation: android.location.Location,
        travelTime: String
    ) {
        val eventLocation = event.location ?: return
        
        // 목적지와의 거리 계산 (간단한 직선거리)
        val eventLatLng = geocodeAddress(eventLocation) ?: return
        val distance = calculateDistance(
            currentLocation.latitude, currentLocation.longitude,
            eventLatLng.first, eventLatLng.second
        )
        
        // 목적지 근처(500m 이내)에 도착했으면 학습 데이터 저장
        if (distance < 0.5) {
            val wasLate = System.currentTimeMillis() > event.startTime
            val actualTravelTime = System.currentTimeMillis() - (event.startTime - 30 * 60 * 1000)
            
            ragService.storeUserExperience(
                event = event,
                travelTime = travelTime,
                actualDepartureTime = System.currentTimeMillis() - actualTravelTime,
                wasLate = wasLate,
                weatherCondition = getCurrentWeather()
            )
        }
    }
    
    private suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        // Google Geocoding API 또는 간단한 주소 매칭
        return when {
            address.contains("홍대") -> Pair(37.5563, 126.9236)
            address.contains("강남") -> Pair(37.4979, 127.0276)
            address.contains("명동") -> Pair(37.5636, 126.9834)
            else -> null
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    
    private fun getCurrentWeather(): String {
        // 실제로는 날씨 API 호출
        return "맑음"
    }
}

class LearningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val ragService = FirebaseRAGService()
        
        // 일정 완료 후 사용자에게 피드백 요청
        val notificationManager = NotificationManager(applicationContext)
        notificationManager.showDepartureNotification(
            title = "일정 완료! 📝",
            message = "정시에 도착하셨나요? 탭해서 알려주세요",
            eventId = "feedback_${inputData.getString("eventId")}"
        )
        
        return Result.success()
    }
}
