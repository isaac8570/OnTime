package com.OnTime.ontime.api

import com.OnTime.ontime.BuildConfig
import com.OnTime.ontime.data.models.NotificationTone
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

class GeminiApiService {

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            maxOutputTokens = 100
        }
    )

    suspend fun generateNotificationText(
        eventTitle: String,
        destination: String,
        transportMode: String,
        estimatedDuration: String,
        userTone: NotificationTone,
        weatherInfo: String? // Optional weather info
    ): String {
        val systemInstruction = when (userTone) {
            NotificationTone.STRONG -> "당신은 절대 지각을 허용하지 않는 단호하고 엄격한 비서입니다. 경고하듯이 간결하고 단호한 어조로 말해야 합니다."
            NotificationTone.SOFT -> "당신은 매우 친절하고 다정한 비서입니다. 사용자를 격려하고 응원하는 부드러운 어조로 말해야 합니다."
        }

        val weatherPrompt = weatherInfo?.let { "현재 날씨는 ${it}입니다." } ?: ""

        val userQuery = """
        다음 일정에 맞춰 출발 알림 메시지를 생성해 줘.
        - 일정: ${eventTitle}
        - 목적지: ${destination}
        - 이동 수단: ${transportMode}
        - 예상 소요 시간: ${estimatedDuration}
        ${weatherPrompt}
        
        알림 메시지는 50자 이내의 짧고 임팩트 있는 문구 하나여야 하며, 시스템 지시에 설정된 어조를 반드시 따라야 합니다.
        """.trimIndent()

        try {
            val response = model.generateContent(userQuery)
            return response.text?.trim() ?: getDefaultNotification(userTone)
        } catch (e: Exception) {
            // Log the exception, e.g., using a custom Logger class
            // Logger.e("Gemini API Error", e.toString())
            return getDefaultNotification(userTone) // Return a default message on error
        }
    }

    private fun getDefaultNotification(userTone: NotificationTone): String {
        return when (userTone) {
            NotificationTone.STRONG -> "출발할 시간입니다. 즉시 이동하세요."
            NotificationTone.SOFT -> "이제 슬슬 출발해볼까요? 좋은 하루 보내세요!"
        }
    }
}