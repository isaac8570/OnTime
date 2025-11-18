package com.OnTime.ontime.service

import com.google.ai.client.generativeai.GenerativeModel
import com.OnTime.ontime.BuildConfig
import com.OnTime.ontime.util.Logger

class LocationExtractorService {
    
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    suspend fun extractLocationFromText(eventTitle: String, eventDescription: String?): String? {
        val fullText = buildString {
            append(eventTitle)
            if (!eventDescription.isNullOrBlank()) {
                append(" ")
                append(eventDescription)
            }
        }
        
        val prompt = """
주어지는 문장은 캘린더의 일정 제목입니다. 다음 일정에서 **장소**에 해당하는 단어, 특히 **역명, 공공기관명, 지역명, 건물명, 주소** 등을 찾아주세요.
- Google Maps에서 검색 가능한 형태로 (되도록 단어 그대로) 반환해주세요.
- 만약 장소를 찾을 수 없다면 **정확히 "없음"** 이라고만 답변해주세요.

예시:
- "홍대 만나기" → "홍대입구역"
- "강남역 스타벅스" → "강남역 스타벅스"
- "강남역 약속" -> "강남역"
- "서울시청에서 회의" → "서울시청"
- "국회의사당 견학" → "국회의사당"
- "집에서 휴식" → "없음"

일정: "$fullText"

장소만 답변:
        """.trimIndent()
        
        return try {
            val response = model.generateContent(prompt)
            val rawGeminiOutput = response.text
            Logger.d("Gemini raw output for '$fullText': '$rawGeminiOutput'") // Added logging
            val location = rawGeminiOutput?.trim()

            if (location.isNullOrBlank() || location.contains("없음")) {
                Logger.d("Gemini extracted location is null, blank, or '없음' for '$fullText'. Final: null") // Added logging
                null
            } else {
                Logger.d("Gemini extracted location successfully for '$fullText'. Final: '$location'") // Added logging
                location
            }

        } catch (e: Exception) {
            Logger.e("Error extracting location with AI: ${e.message}", e) // Added logging
            null
        }
    }
}
