// 사용 예시: AI를 통한 장소 자동 추출

// 예시 1: 일정 제목에서 장소 추출
val event1 = CalendarEvent(
    id = "1",
    title = "강남역 스타벅스에서 회의",
    location = null, // 위치 필드가 비어있음
    description = "프로젝트 논의",
    startTime = System.currentTimeMillis(),
    endTime = System.currentTimeMillis()
)

// AI가 "강남역 스타벅스"를 추출하여 Google Maps에서 검색

// 예시 2: 설명에서 장소 추출  
val event2 = CalendarEvent(
    id = "2", 
    title = "병원 진료",
    location = null,
    description = "서울대학교병원 내과 2층",
    startTime = System.currentTimeMillis(),
    endTime = System.currentTimeMillis()
)

// AI가 "서울대학교병원"을 추출

// 예시 3: 복합 정보에서 장소 추출
val event3 = CalendarEvent(
    id = "3",
    title = "친구 만나기", 
    location = null,
    description = "홍대 걷고싶은거리 근처 카페에서",
    startTime = System.currentTimeMillis(),
    endTime = System.currentTimeMillis()
)

// AI가 "홍대 걷고싶은거리"를 추출

// 자동으로 이동 시간 계산됨
val travelInfo = travelTimeRepository.calculateTravelTimeForEvent(event1)
println("이동 시간: ${travelInfo?.durationText}")
