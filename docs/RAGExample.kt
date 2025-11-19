// RAG 시스템 작동 예시

// 1단계: 사용자 패턴 학습
val ragService = RAGService(context)

// 사용자가 홍대 약속에 자주 늦는다면...
ragService.saveUserPattern(
    event = hongdaeEvent,
    actualDepartureTime = scheduledTime + 15 * 60 * 1000, // 15분 늦게 출발
    wasLate = true
)

// 2단계: 학습된 패턴으로 개인화된 알림 생성
val smartNotification = ragService.generateSmartNotification(
    event = hongdaeEvent,
    travelTime = "25분",
    weatherInfo = "비 예보"
)

// 결과 예시:
// "홍대는 항상 늦으시네요! 비도 오니 지금 바로 출발하세요 ⚡"
// "이 시간대 홍대행은 보통 10분 더 걸려요. 여유있게 출발하세요 🚇"
// "지난 3번 모두 늦었어요. 이번엔 정시 도착 도전! 💪"

// 3단계: 사용자 피드백으로 지속 학습
smartNotificationService.recordUserFeedback(
    event = hongdaeEvent,
    departed = true,
    departureTime = System.currentTimeMillis()
)

// RAG의 장점:
// ✅ 개인 패턴 학습 (자주 늦는 장소, 요일별 패턴)
// ✅ 상황별 맞춤 알림 (날씨, 교통상황 고려)
// ✅ 지속적 개선 (사용할수록 더 정확해짐)
// ✅ 감성적 메시지 (딱딱하지 않은 친근한 톤)
