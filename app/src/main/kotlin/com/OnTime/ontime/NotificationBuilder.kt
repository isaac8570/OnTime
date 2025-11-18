package com.OnTime.ontime

import com.google.ai.client.generativeai.GenerativeModel
import com.OnTime.ontime.BuildConfig
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.data.models.UserPreferences

/**
 * Gemini API를 사용하여 사용자 맞춤형 알림 메시지를 생성하는 클래스
 */
class NotificationBuilder {

    // BuildConfig에서 API 키를 가져와 GenerativeModel을 초기화합니다.
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash", // 또는 다른 적절한 모델
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /**
     * 주어진 정보를 바탕으로 Gemini 모델을 호출하여 알림 메시지를 생성합니다.
     * 이 함수는 비동기적으로 작동하므로 코루틴 내에서 호출해야 합니다.
     *
     * @param userPreferences 사용자가 설정한 기본값 (메시지 톤 등)
     * @param eventName 일정 이름 (예: "강남역 회의")
     * @param eventTime 일정 시작 시간 (예: LocalTime.of(14, 0))
     * @param travelTime 예상 이동 시간 (분 단위, 예: 30) (Google Maps ETA)
     * @param actualTravelTime 실제 이동 시간 (분 단위, 예: 35). 이 값이 null이면 사용자가 제시간에 출발하도록 독려하는 일반적인 메시지를 생성.
     * @param weatherInfo 현재 또는 예보된 날씨 (예: "비, 기온 15도")
     * @param userPattern 사용자의 과거 행동 패턴 (예: "이 장소에 10분씩 늦는 경향이 있음")
     * @return 생성된 알림 메시지 문자열 또는 오류 시 null
     */
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.S)
    suspend fun generateNotificationMessage(
        userPreferences: UserPreferences,
        eventName: String,
        eventTime: LocalTime,
        travelTime: Int, // Estimated Travel Time (Google Maps ETA)
        actualTravelTime: Int?, // Actual Travel Time (from tracking or model prediction)
        weatherInfo: String,
        userPattern: String? = null
    ): String? {
        return try {
            // 1. Gemini API에 전달할 프롬프트 구성
            val prompt = createPrompt(userPreferences, eventName, eventTime, travelTime, actualTravelTime, weatherInfo, userPattern)

            // 2. Gemini API 호출
            val response = generativeModel.generateContent(prompt)

            // 3. API 응답 반환
            response.text
        } catch (e: Exception) {
            // API 호출 실패 시 로그를 남기고 null을 반환합니다.
            com.OnTime.ontime.util.Logger.e("Error generating notification", e)
            null
        }
    }

    /**
     * Gemini 모델에 전달할 프롬프트를 생성합니다.
     */
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.S)
    private fun createPrompt(
        userPreferences: UserPreferences,
        eventName: String,
        eventTime: LocalTime,
        travelTime: Int, // Estimated Travel Time (Google Maps ETA)
        actualTravelTime: Int?, // Actual Travel Time (from tracking or model prediction)
        weatherInfo: String,
        userPattern: String?
    ): String {
        val estimatedDepartureTime = eventTime.minusMinutes(travelTime.toLong())

        var prompt = """
            너는 사용자의 일정을 관리하고 제시간에 도착하도록 돕는 유능한 비서야.
            아래 정보를 바탕으로, 사용자가 제시간에 준비하고 출발하도록 독려하는 친절하고 효과적인 알림 메시지를 생성해 줘.
            메시지는 항상 한국어로 작성해 줘.

            [메시지 톤]
            - 전체적인 메시지 톤은 '${userPreferences.notificationTone.description}'에 맞춰서 작성해 줘.

            [일정 정보]
            - 이름: $eventName
            - 시간: $eventTime
            - 예상 이동 시간: ${travelTime}분
            - 예상 출발 시간: $estimatedDepartureTime
            - 날씨: $weatherInfo
        """.trimIndent()

        // Actual vs Estimated Travel Time Comparison
        if (actualTravelTime != null) {
            val timeDifference = actualTravelTime - travelTime // Positive means late, negative means early
            if (timeDifference > 0) {
                prompt += """
                
                [현재 상황]
                - 현재 예상 도착 시간은 구글 지도의 예상 경로보다 ${timeDifference}분 늦을 것으로 예상됩니다.
                사용자가 늦지 않도록 긴급성을 강조하는 메시지를 작성해 줘. 필요하다면 빨리 출발하도록 재촉해 줘.
                """.trimIndent()
            } else if (timeDifference < 0) {
                val earlyMinutes = -timeDifference
                prompt += """
                
                [현재 상황]
                - 현재 예상 도착 시간은 구글 지도의 예상 경로보다 ${earlyMinutes}분 빠를 것으로 예상됩니다.
                사용자가 너무 일찍 도착하지 않도록, 여유를 주면서도 제시간에 도착할 수 있도록 조언하는 메시지를 작성해 줘.
                """.trimIndent()
            } else {
                prompt += """
                
                [현재 상황]
                - 현재 예상 도착 시간은 구글 지도의 예상 경로와 거의 일치할 것으로 예상됩니다.
                사용자가 현재처럼 잘 준비하여 제시간에 출발할 수 있도록 독려하는 메시지를 작성해 줘.
                """.trimIndent()
            }
        } else {
            prompt += """
                
                [현재 상황]
                - 실제 이동 시간 정보가 아직 없습니다.
                사용자가 예상 출발 시간에 맞춰 제시간에 준비하고 출발하도록 독려하는 일반적인 메시지를 작성해 줘.
                """.trimIndent()
        }


        if (userPattern != null && userPattern.isNotBlank()) {
            prompt += """

            [사용자 과거 행동 패턴]
            - $userPattern
            
            이 패턴을 고려해서, 좀 더 강조하거나 개인화된 조언을 추가해 줘. 예를 들어, 자주 늦는 장소라면 조금 더 서두르라고 재치있게 알려줄 수 있어.
            """.trimIndent()
        }

        prompt += """

            [요청]
            위 모든 정보를 종합하여, 사용자가 지금 바로 준비해야 할 이유를 명확히 알 수 있는 알림 메시지를 한두 문장으로 생성해 줘.
        """.trimIndent()
        
        return prompt
    }
}

// --- 사용 예시 ---
@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.S)
fun main() = runBlocking {
    // 로컬에서 직접 실행 시 BuildConfig.GEMINI_API_KEY가 비어있을 수 있으므로,
    // 실행 전 local.properties 파일에 GEMINI_API_KEY가 설정되었는지 확인하세요.
    if (BuildConfig.GEMINI_API_KEY.isBlank()) {
        println("경고: GEMINI_API_KEY가 설정되지 않았습니다.")
        println("프로젝트 루트의 local.properties 파일에 'GEMINI_API_KEY=당신의_API_키'를 추가해주세요.")
        return@runBlocking
    }

    val notificationBuilder = NotificationBuilder()

    val userPreferences = UserPreferences() // Default friendly tone

    // Scenario 1: User is late
    println("--- 시나리오 1: 사용자가 늦을 경우 (긴급성 강조) ---")
    val message1 = notificationBuilder.generateNotificationMessage(
        userPreferences = userPreferences,
        eventName = "팀 회의",
        eventTime = LocalTime.of(10, 0),
        travelTime = 30, // Estimated ETA
        actualTravelTime = 35, // Currently taking 35 mins (5 mins late)
        weatherInfo = "맑음, 20도",
        userPattern = "자주 지각하는 경향이 있음"
    )
    println(message1 ?: "메시지 생성 실패")
    println()

    // Scenario 2: User is early
    println("--- 시나리오 2: 사용자가 빠를 경우 (여유 조언) ---")
    val message2 = notificationBuilder.generateNotificationMessage(
        userPreferences = userPreferences,
        eventName = "친구와 점심 식사",
        eventTime = LocalTime.of(13, 0),
        travelTime = 20, // Estimated ETA
        actualTravelTime = 15, // Currently taking 15 mins (5 mins early)
        weatherInfo = "흐림, 18도"
    )
    println(message2 ?: "메시지 생성 실패")
    println()

    // Scenario 3: User is on time
    println("--- 시나리오 3: 사용자가 제시간일 경우 (독려) ---")
    val message3 = notificationBuilder.generateNotificationMessage(
        userPreferences = userPreferences,
        eventName = "저녁 약속",
        eventTime = LocalTime.of(19, 30),
        travelTime = 40, // Estimated ETA
        actualTravelTime = 40, // Currently taking 40 mins (on time)
        weatherInfo = "선선함, 15도"
    )
    println(message3 ?: "메시지 생성 실패")
    println()

    // Scenario 4: actualTravelTime is null (general prompt)
    println("--- 시나리오 4: 실제 이동 시간 정보가 없을 경우 (일반적인 독려) ---")
    val message4 = notificationBuilder.generateNotificationMessage(
        userPreferences = userPreferences,
        eventName = "운동 수업",
        eventTime = LocalTime.of(8, 0),
        travelTime = 25, // Estimated ETA
        actualTravelTime = null, // No actual time yet
        weatherInfo = "쌀쌀함, 10도"
    )
    println(message4 ?: "메시지 생성 실패")
}