package com.OnTime.ontime.service

import android.content.Context
import android.content.SharedPreferences
import com.google.ai.client.generativeai.GenerativeModel
import com.OnTime.ontime.BuildConfig
import com.OnTime.ontime.data.models.CalendarEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RAGService(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("rag_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    // 사용자 패턴 저장
    fun saveUserPattern(event: CalendarEvent, actualDepartureTime: Long, wasLate: Boolean) {
        val patterns = getUserPatterns().toMutableList()
        patterns.add(UserPattern(
            location = event.location ?: "",
            scheduledTime = event.startTime,
            actualDepartureTime = actualDepartureTime,
            wasLate = wasLate,
            dayOfWeek = java.util.Calendar.getInstance().apply { 
                timeInMillis = event.startTime 
            }.get(java.util.Calendar.DAY_OF_WEEK),
            timestamp = System.currentTimeMillis()
        ))
        
        // 최근 50개만 유지
        if (patterns.size > 50) {
            patterns.removeAt(0)
        }
        
        prefs.edit().putString("user_patterns", gson.toJson(patterns)).apply()
    }
    
    // 사용자 패턴 조회
    private fun getUserPatterns(): List<UserPattern> {
        val json = prefs.getString("user_patterns", "[]")
        val type = object : TypeToken<List<UserPattern>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    // RAG 기반 스마트 알림 생성
    suspend fun generateSmartNotification(
        event: CalendarEvent,
        travelTime: String,
        weatherInfo: String?
    ): String {
        val patterns = getUserPatterns()
        val relevantPatterns = patterns.filter { 
            it.location.contains(event.location ?: "", ignoreCase = true) ||
            it.dayOfWeek == java.util.Calendar.getInstance().apply { 
                timeInMillis = event.startTime 
            }.get(java.util.Calendar.DAY_OF_WEEK)
        }
        
        val contextInfo = buildContextInfo(relevantPatterns, weatherInfo)
        
        val prompt = """
사용자의 과거 패턴을 분석해서 개인화된 알림을 만들어주세요.

일정 정보:
- 제목: ${event.title}
- 장소: ${event.location ?: "미정"}
- 예상 이동시간: $travelTime
- 날씨: ${weatherInfo ?: "정보 없음"}

사용자 패턴 분석:
$contextInfo

이 정보를 바탕으로 사용자에게 도움이 되는 개인화된 출발 알림을 50자 이내로 생성해주세요.
        """.trimIndent()
        
        return try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: "출발할 시간입니다!"
        } catch (e: Exception) {
            "출발할 시간입니다!"
        }
    }
    
    private fun buildContextInfo(patterns: List<UserPattern>, weatherInfo: String?): String {
        if (patterns.isEmpty()) return "신규 사용자 - 패턴 데이터 없음"
        
        val lateCount = patterns.count { it.wasLate }
        val totalCount = patterns.size
        val lateRate = (lateCount.toFloat() / totalCount * 100).toInt()
        
        val avgExtraTime = patterns.filter { it.wasLate }
            .map { (it.actualDepartureTime - it.scheduledTime) / (60 * 1000) }
            .average().takeIf { !it.isNaN() }?.toInt() ?: 0
        
        return """
- 지각률: $lateRate% ($lateCount/$totalCount)
- 평균 지연시간: ${avgExtraTime}분
- 이 장소/요일 방문 횟수: ${patterns.size}회
- 최근 패턴: ${if (patterns.takeLast(3).all { it.wasLate }) "자주 늦음" else "대체로 정시"}
        """.trimIndent()
    }
}

data class UserPattern(
    val location: String,
    val scheduledTime: Long,
    val actualDepartureTime: Long,
    val wasLate: Boolean,
    val dayOfWeek: Int,
    val timestamp: Long
)
