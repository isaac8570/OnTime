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
        modelName = "gemini-pro", // 또는 다른 적절한 모델
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /**
     * 주어진 정보를 바탕으로 Gemini 모델을 호출하여 알림 메시지를 생성합니다.
     * 이 함수는 비동기적으로 작동하므로 코루틴 내에서 호출해야 합니다.
     *
     * @param userPreferences 사용자가 설정한 기본값 (메시지 톤 등)
     * @param eventName 일정 이름 (예: "강남역 회의")
     * @param eventTime 일정 시작 시간 (예: LocalTime.of(14, 0))
     * @param travelTime 예상 이동 시간 (분 단위, 예: 30)
     * @param weatherInfo 현재 또는 예보된 날씨 (예: "비, 기온 15도")
     * @param userPattern 사용자의 과거 행동 패턴 (예: "이 장소에 10분씩 늦는 경향이 있음")
     * @return 생성된 알림 메시지 문자열 또는 오류 시 null
     */
    suspend fun generateNotificationMessage(
        userPreferences: UserPreferences,
        eventName: String,
        eventTime: LocalTime,
        travelTime: Int,
        weatherInfo: String,
        userPattern: String? = null
    ): String? {
        return try {
            // 1. Gemini API에 전달할 프롬프트 구성
            val prompt = createPrompt(userPreferences, eventName, eventTime, travelTime, weatherInfo, userPattern)

            // 2. Gemini API 호출
            val response = generativeModel.generateContent(prompt)

            // 3. API 응답 반환
            response.text
        } catch (e: Exception) {
            // API 호출 실패 시 로그를 남기고 null을 반환합니다.
            println("Error generating notification: ${e.message}")
            null
        }
    }

    /**
     * Gemini 모델에 전달할 프롬프트를 생성합니다.
     */
    private fun createPrompt(
        userPreferences: UserPreferences,
        eventName: String,
        eventTime: LocalTime,
        travelTime: Int,
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
fun main() = runBlocking {
    // 로컬에서 직접 실행 시 BuildConfig.GEMINI_API_KEY가 비어있을 수 있으므로,
    // 실행 전 local.properties 파일에 GEMINI_API_KEY가 설정되었는지 확인하세요.
    if (BuildConfig.GEMINI_API_KEY.isBlank()) {
        println("경고: GEMINI_API_KEY가 설정되지 않았습니다.")
        println("프로젝트 루트의 local.properties 파일에 'GEMINI_API_KEY=당신의_API_키'를 추가해주세요.")
        return@runBlocking
    }

    val notificationBuilder = NotificationBuilder()

    // 1. 시나리오 1: 기본 설정 (친근한 톤)
    println("--- 시나리오 1: 친근한 톤으로 생성 중... ---")
    val preferences1 = UserPreferences() // 기본값 사용
    val message1 = notificationBuilder.generateNotificationMessage(
        userPreferences = preferences1,
        eventName = "판교역에서 친구 만나기",
        eventTime = LocalTime.of(19, 0),
        travelTime = 45,
        weatherInfo = "맑음, 22도"
    )
    println(message1 ?: "메시지 생성 실패")
    println()

    // 2. 시나리오 2: 격식있는 톤, 늦는 경향, 좋지 않은 날씨
    println("--- 시나리오 2: 격식있는 톤으로 생성 중... ---")
    val preferences2 = UserPreferences(notificationTone = NotificationTone.FORMAL)
    val message2 = notificationBuilder.generateNotificationMessage(
        userPreferences = preferences2,
        eventName = "중요한 비즈니스 미팅",
        eventTime = LocalTime.of(10, 0),
        travelTime = 25,
        weatherInfo = "비 예보, 출근길 정체 예상",
        userPattern = "과거 이 시간대 미팅에 두 번 지각한 기록이 있음"
    )
    println(message2 ?: "메시지 생성 실패")
    println()
    
    // 3. 시나리오 3: 재치있는 톤
    println("--- 시나리오 3: 재치있는 톤으로 생성 중... ---")
    val preferences3 = UserPreferences(notificationTone = NotificationTone.HUMOROUS)
    val message3 = notificationBuilder.generateNotificationMessage(
        userPreferences = preferences3,
        eventName = "저녁 요가 클래스",
        eventTime = LocalTime.of(20, 0),
        travelTime = 15,
        weatherInfo = "선선한 저녁 공기"
    )
    println(message3 ?: "메시지 생성 실패")
}
