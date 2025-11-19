// 사용 예시: Google Maps API를 통한 이동 시간 계산

// 1. 현재 위치에서 캘린더 일정 위치까지의 이동 시간 계산
val locationService = LocationService(context)
val currentLocation = locationService.getCurrentLocation()

if (currentLocation != null && event.location != null) {
    val travelInfo = locationService.calculateTravelTime(
        currentLocation = currentLocation,
        destinationAddress = event.location,
        mode = Constants.MODE_TRANSIT // 대중교통
    )
    
    travelInfo?.let {
        println("이동 시간: ${it.durationText}")
        println("거리: ${it.distanceText}")
        println("소요 시간(분): ${it.durationMinutes}")
    }
}

// 2. 여러 일정에 대한 이동 시간 일괄 계산
val travelTimeRepository = TravelTimeRepository(context)
val events = calendarRepository.getEvents(context)

val travelTimes = travelTimeRepository.calculateTravelTimeForEvents(
    events = events,
    transportMode = Constants.MODE_DRIVING // 자가용
)

events.forEach { event ->
    val travelInfo = travelTimes[event.id]
    println("${event.title}: ${travelInfo?.durationText ?: "계산 불가"}")
}

// 3. UI에서 이동 수단 변경
calendarViewModel.setTravelMode(Constants.MODE_WALKING) // 도보로 변경
// 자동으로 모든 일정의 이동 시간이 재계산됩니다
