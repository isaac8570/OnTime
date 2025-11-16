package com.OnTime.ontime.service

import com.google.ai.client.generativeai.GenerativeModel
import com.OnTime.ontime.BuildConfig

class LocationExtractorService {
    
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
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
다음 일정에서 구체적인 장소나 위치를 찾아주세요.
- 역명, 지역명, 건물명, 주소 등을 추출
- Google Maps에서 검색 가능한 형태로 반환
- 장소가 명확하지 않으면 "없음"

예시:
- "홍대 만나기" → "홍대입구역"
- "강남역 스타벅스" → "강남역 스타벅스"
- "집에서 휴식" → "없음"

일정: "$fullText"

장소만 답변:
        """.trimIndent()
        
        return try {
            val response = model.generateContent(prompt)
            val location = response.text?.trim()
            
            if (location.isNullOrBlank() || location.contains("없음")) null else location
        } catch (e: Exception) {
            null
        }
    }
}
