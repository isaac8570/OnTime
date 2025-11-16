package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.NotificationBuilder // Import NotificationBuilder
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.CalendarRepository // Import CalendarRepository
import com.OnTime.ontime.data.repositories.SettingsRepository // Import SettingsRepository
import com.OnTime.ontime.service.ILocationService
import com.OnTime.ontime.util.Constants
import com.OnTime.ontime.util.LocationConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random

class MainViewModel(
    application: Application,
    private val weatherRepository: WeatherRepository,
    private val locationService: ILocationService,
    private val locationRepository: LocationRepository,
    private val calendarRepository: CalendarRepository, // Add CalendarRepository
    private val notificationBuilder: NotificationBuilder, // Add NotificationBuilder
    private val settingsRepository: SettingsRepository // Add SettingsRepository
) : AndroidViewModel(application) {

    // private val _notificationMessage = MutableLiveData<String>() // Removed, replaced by preDepartureNotificationMessage
    // val notificationMessage: LiveData<String> = _notificationMessage

    private val _preDepartureNotificationMessage = MutableLiveData<String>()
    val preDepartureNotificationMessage: LiveData<String> = _preDepartureNotificationMessage

    private val _weatherStatus = MutableLiveData<String>()
    val weatherStatus: LiveData<String> = _weatherStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Function to start the location tracking service
    fun startLocationTracking() {
        locationService.startLocationUpdatesService()
    }

    // Function to stop the location tracking service
    fun stopLocationTracking() {
        locationService.stopLocationUpdatesService()
    }

    override fun onCleared() {
        super.onCleared()
        // Stop location tracking when the ViewModel is cleared (e.g., activity destroyed)
        stopLocationTracking()
    }

    /**
     * 다음 예정된 이벤트를 기반으로 출발 전 맞춤 알림 메시지를 생성합니다.
     */
    fun generatePreDepartureNotification() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. 다음 예정된 이벤트 가져오기
                val upcomingEvents = calendarRepository.getEvents()
                val nextEvent = upcomingEvents.firstOrNull { it.startTime > System.currentTimeMillis() } // 가장 가까운 미래 이벤트

                if (nextEvent == null || nextEvent.location.isNullOrBlank()) {
                    _preDepartureNotificationMessage.postValue("다음 예정된 이벤트가 없거나 위치 정보가 없습니다.")
                    _isLoading.value = false
                    return@launch
                }

                // 2. 현재 위치 가져오기
                val currentLocation = locationRepository.getCurrentLocation()
                if (currentLocation == null) {
                    _preDepartureNotificationMessage.postValue("현재 위치를 가져올 수 없습니다. 위치 권한을 확인해주세요.")
                    _isLoading.value = false
                    return@launch
                }
                val originLatLng = Pair(currentLocation.latitude, currentLocation.longitude)

                // 3. 목적지 좌표 변환
                val destinationLatLng = LocationConverter.addressToLatLng(getApplication(), nextEvent.location!!)
                if (destinationLatLng == null) {
                    _preDepartureNotificationMessage.postValue("목적지 주소 변환에 실패했습니다.")
                    _isLoading.value = false
                    return@launch
                }

                // 4. 예상 이동 시간 (Google Maps ETA) 계산
                val travelInfo = locationRepository.calculateTravelTime(originLatLng, destinationLatLng)
                val estimatedTravelTimeMinutes = travelInfo?.durationMinutes
                if (estimatedTravelTimeMinutes == null) {
                    _preDepartureNotificationMessage.postValue("예상 이동 시간 계산에 실패했습니다.")
                    _isLoading.value = false
                    return@launch
                }

                // 5. 날씨 정보 가져오기 (목적지 기준)
                val gridCoords = LocationConverter.latLngToKmaGrid(destinationLatLng.first, destinationLatLng.second)
                val weatherInfo = weatherRepository.getWeatherCondition(gridCoords)

                // 6. 모델 예측 실제 이동 시간 시뮬레이션 (플레이스홀더)
                val predictedRatio = 1.0 + (Random.nextDouble(-0.1, 0.1)) // -10% ~ +10%
                val predictedActualTravelTimeMinutes = (estimatedTravelTimeMinutes * predictedRatio).toInt()

                // 7. 사용자 환경설정 가져오기
                val userPreferences = settingsRepository.getUserPreferences()

                // 8. Gemini API를 통해 알림 메시지 생성
                val generatedMessage = notificationBuilder.generateNotificationMessage(
                    userPreferences = userPreferences,
                    eventName = nextEvent.title,
                    eventTime = LocalTime.ofInstant(Instant.ofEpochMilli(nextEvent.startTime), ZoneId.systemDefault()),
                    travelTime = estimatedTravelTimeMinutes,
                    actualTravelTime = predictedActualTravelTimeMinutes,
                    weatherInfo = weatherInfo,
                    userPattern = null // TODO: 실제 사용자 패턴 데이터 통합
                )

                _preDepartureNotificationMessage.postValue(generatedMessage ?: "알림 메시지 생성에 실패했습니다.")

            } catch (e: Exception) {
                _preDepartureNotificationMessage.postValue("알림 생성 중 오류 발생: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * 현재 위치의 날씨 정보를 가져와 UI에 표시합니다.
     */
    fun checkCurrentWeather() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation() ?: run { // Use locationRepository
                    _weatherStatus.value = "현재 위치를 가져올 수 없습니다. 위치 권한을 확인해주세요."
                    _isLoading.value = false
                    return@launch
                }

                val address = withContext(Dispatchers.IO) {
                    LocationConverter.latLngToAddress(getApplication(), location.latitude, location.longitude)
                }

                val gridCoords = LocationConverter.latLngToKmaGrid(location.latitude, location.longitude) ?: run {
                    _weatherStatus.value = "위치 좌표를 변환할 수 없습니다."
                    _isLoading.value = false
                    return@launch
                }

                val weatherItems = weatherRepository.getShortTermForecast(gridCoords.first, gridCoords.second)
                val temp = weatherItems.find { it.category == "TMP" }?.fcstValue
                val sky = when (weatherItems.find { it.category == "SKY" }?.fcstValue) {
                    "1" -> "맑음 ☀️"
                    "3" -> "구름많음 ☁️"
                    "4" -> "흐림 🌥️"
                    else -> "알 수 없음"
                }
                val precipitation = when (weatherItems.find { it.category == "PTY" }?.fcstValue) {
                    "0" -> "없음"
                    "1" -> "비"
                    "2" -> "비/눈"
                    "3" -> "눈"
                    "4" -> "소나기"
                    "5" -> "빗방울"
                    "6" -> "빗방울/눈날림"
                    "7" -> "눈날림"
                    else -> "알 수 없음"
                }

                val statusText = "📍 현재 위치: $address\n" +
                                 "🌡️ 기온: ${temp}°C\n" +
                                 " 하늘: $sky\n" +
                                 "💧 강수: $precipitation"
                _weatherStatus.value = statusText

            } catch (e: Exception) {
                _weatherStatus.value = "오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}