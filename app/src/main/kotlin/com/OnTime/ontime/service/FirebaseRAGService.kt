package com.OnTime.ontime.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.OnTime.ontime.BuildConfig
import com.OnTime.ontime.data.models.CalendarEvent
import kotlinx.coroutines.tasks.await

class FirebaseRAGService {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val embeddingService = GeminiEmbeddingService()
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp", 
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    // 1. 사용자 경험을 벡터로 저장
    suspend fun storeUserExperience(
        event: CalendarEvent,
        travelTime: String,
        actualDepartureTime: Long,
        wasLate: Boolean,
        weatherCondition: String?
    ) {
        val userId = auth.currentUser?.uid ?: return
        
        // 경험을 텍스트로 구조화
        val experienceText = """
        일정: ${event.title}
        장소: ${event.location}
        이동시간: $travelTime
        결과: ${if (wasLate) "지각" else "정시"}
        날씨: ${weatherCondition ?: "정보없음"}
        """.trimIndent()
        
        // 진짜 Gemini Embedding API로 벡터 생성
        val embedding = embeddingService.getEmbedding(experienceText) ?: return
        
        val experienceData = mapOf(
            "userId" to userId,
            "text" to experienceText,
            "embedding" to embedding,
            "location" to (event.location ?: ""),
            "wasLate" to wasLate,
            "timestamp" to System.currentTimeMillis()
        )
        
        firestore.collection("user_experiences")
            .add(experienceData)
            .await()
    }
    
    // 2. 벡터 유사도로 관련 경험 검색
    suspend fun findSimilarExperiences(
        currentEvent: CalendarEvent,
        weatherCondition: String?
    ): List<String> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        
        // 현재 상황을 텍스트로 구조화
        val queryText = """
        일정: ${currentEvent.title}
        장소: ${currentEvent.location}
        날씨: ${weatherCondition ?: "정보없음"}
        """.trimIndent()
        
        // 쿼리 임베딩 생성
        val queryEmbedding = embeddingService.getEmbedding(queryText) ?: return emptyList()
        
        // Firestore에서 사용자의 모든 경험 가져오기
        val experiences = firestore.collection("user_experiences")
            .whereEqualTo("userId", userId)
            .limit(20)
            .get()
            .await()
        
        // 벡터 유사도 계산 및 정렬
        val similarExperiences = experiences.documents.mapNotNull { doc ->
            val embedding = doc.get("embedding") as? List<Double> ?: return@mapNotNull null
            val text = doc.getString("text") ?: return@mapNotNull null
            val similarity = embeddingService.cosineSimilarity(queryEmbedding, embedding)
            
            Pair(similarity, text)
        }.sortedByDescending { it.first }
            .take(3) // 상위 3개만
            .map { it.second }
        
        return similarExperiences
    }
    
    // 3. RAG 기반 스마트 알림 생성
    suspend fun generateRAGNotification(
        event: CalendarEvent,
        travelTime: String,
        weatherCondition: String?
    ): String {
        val similarExperiences = findSimilarExperiences(event, weatherCondition)
        
        val contextPrompt = if (similarExperiences.isNotEmpty()) {
            """
과거 유사한 경험들:
${similarExperiences.joinToString("\n\n")}

현재 상황:
- 일정: ${event.title}
- 장소: ${event.location}
- 이동시간: $travelTime
- 날씨: ${weatherCondition ?: "정보없음"}

위 과거 경험을 바탕으로 개인화된 출발 알림을 50자 이내로 생성해주세요.
            """.trimIndent()
        } else {
            """
현재 상황:
- 일정: ${event.title}  
- 장소: ${event.location}
- 이동시간: $travelTime
- 날씨: ${weatherCondition ?: "정보없음"}

친근하고 도움이 되는 출발 알림을 50자 이내로 생성해주세요.
            """.trimIndent()
        }
        
        return try {
            val response = generativeModel.generateContent(contextPrompt)
            response.text?.trim() ?: "출발할 시간입니다!"
        } catch (e: Exception) {
            "출발할 시간입니다!"
        }
    }
}
