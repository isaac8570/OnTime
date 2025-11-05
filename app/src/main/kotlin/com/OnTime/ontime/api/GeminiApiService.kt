package com.OnTime.ontime.api

import com.OnTime.ontime.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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
        temp: String,
        weather: String,
        destination: String,
        userTone: String
    ): String {
        val systemInstruction = """
            당신은 사용자의 개인 비서이며, ${userTone} 톤으로 응답해야 합니다. 
            도착지 온도는 ${temp}이고, ${destination}에 ${weather}이 내리고 있습니다. 
            공감과 함께 행동 아이디어를 제안하십시오.
            출력은 50자 이내의 짧고 임팩트 있는 문구 하나여야 합니다.
        """.trimIndent()

        val userQuery = "지금 출발해야 하는 상황에 맞는 짧고 임팩트 있는 알림 문구를 생성해 줘."

        val prompt = content {
            text(systemInstruction)
            text(userQuery)
        }

        val response = model.generateContent(prompt)
        return response.text?.trim() ?: "출발 시간입니다!"
    }
}

