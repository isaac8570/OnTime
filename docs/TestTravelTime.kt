// 테스트용 - 거리 계산 확인

// 1. 장소 추출 테스트
val locationExtractor = LocationExtractorService()
val extractedLocation = locationExtractor.extractLocationFromText("홍대 만나기", null)
println("추출된 장소: $extractedLocation")

// 2. 현재 위치 확인
val locationService = LocationService(context)
val currentLocation = locationService.getCurrentLocation()
println("현재 위치: ${currentLocation?.latitude}, ${currentLocation?.longitude}")

// 3. 거리 계산 테스트
if (currentLocation != null && extractedLocation != null) {
    val travelInfo = locationService.calculateTravelTime(
        currentLocation = currentLocation,
        destinationAddress = extractedLocation,
        mode = Constants.MODE_TRANSIT
    )
    println("이동 시간: ${travelInfo?.durationText}")
    println("거리: ${travelInfo?.distanceText}")
}

// 4. 권한 확인
val hasLocationPermission = ContextCompat.checkSelfPermission(
    context, 
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED
println("위치 권한: $hasLocationPermission")

// 5. API 키 확인
println("Maps API Key: ${Constants.GOOGLE_MAPS_API_KEY}")
println("Gemini API Key: ${Constants.GEMINI_API_KEY}")
