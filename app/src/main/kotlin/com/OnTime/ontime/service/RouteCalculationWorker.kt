package com.OnTime.ontime.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.OnTime.ontime.api.GeminiApiService
import com.OnTime.ontime.data.models.WeatherData

class RouteCalculationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val geminiService = GeminiApiService()
    
    override suspend fun doWork(): Result {
        return try {
            // 1. 날씨 데이터 가져오기 (하드코딩)
            val weatherData = WeatherData(
                temperature = "12°C",
                condition = "첫눈"
            )
            
            // 2. 일정 정보 가져오기
            val destination = inputData.getString("destination") ?: "강남역"
            val userTone = inputData.getString("userTone") ?: "감성적이고 설렘을 유도하는"
            
            // 3. Gemini로 알림 문구 생성
            val notificationText = geminiService.generateNotificationText(
                temp = weatherData.temperature,
                weather = weatherData.condition,
                destination = destination,
                userTone = userTone
            )
            
            // 4. 알림 발송
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showNotification(
                title = "출발 시간입니다!",
                message = notificationText
            )
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

