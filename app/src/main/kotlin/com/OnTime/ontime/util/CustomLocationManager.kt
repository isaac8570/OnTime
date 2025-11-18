package com.OnTime.ontime.util

object CustomLocationManager {

    // 사용자 정의 위치를 관리하기 위한 맵 (초기 데이터)
    // Key: 키워드 (e.g., "헬스장")
    // Value: 실제 주소 또는 장소 이름 (e.g., "서울시 강남구 테헤란로 427 위워크타워")
    private val customLocations = mapOf(
        "헬스장" to "서울시 강남구 역삼동 123-45", // 예시 주소
        "회사" to "서울시 종로구 종로 1",       // 예시 주소
        "본가" to "부산시 해운대구 우동 567-89"    // 예시 주소
    )

    /**
     * 제목에서 사용자 정의 키워드를 찾아 매핑된 주소를 반환합니다.
     *
     * @param title 이벤트 제목
     * @return 키워드에 해당하는 주소. 없으면 null.
     */
    fun findLocationFromTitle(title: String): String? {
        for ((keyword, location) in customLocations) {
            if (title.contains(keyword, ignoreCase = true)) {
                return location
            }
        }
        return null
    }
}
