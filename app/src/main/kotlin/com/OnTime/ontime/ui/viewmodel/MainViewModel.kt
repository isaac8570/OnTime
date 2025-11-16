package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.service.LocationService
import com.OnTime.ontime.util.LocationConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainViewModel(
    application: Application,
    private val weatherRepository: WeatherRepository,
    private val locationService: LocationService
) : AndroidViewModel(application) {

    private val _notificationMessage = MutableLiveData<String>()
    val notificationMessage: LiveData<String> = _notificationMessage

    private val _weatherStatus = MutableLiveData<String>()
    val weatherStatus: LiveData<String> = _weatherStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * 실시간 위치 및 날씨 데이터를 기반으로 알림 메시지를 생성합니다.
     */
    fun generateRealtimeNotificationMessage() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val location = locationService.getCurrentLocation() ?: run {
                    _notificationMessage.value = "현재 위치를 가져올 수 없습니다. 위치 권한을 확인해주세요."
                    _isLoading.value = false
                    return@launch
                }

                val gridCoords = LocationConverter.latLngToKmaGrid(location.latitude, location.longitude) ?: run {
                    _notificationMessage.value = "위치 좌표를 변환할 수 없습니다."
                    _isLoading.value = false
                    return@launch
                }

                val weatherItems = weatherRepository.getShortTermForecast(gridCoords.first, gridCoords.second)
                val ptyItem = weatherItems.find { it.category == "PTY" }?.fcstValue
                val weatherCondition = when (ptyItem) {
                    "1", "2", "4" -> "rain"
                    "3" -> "snow"
                    else -> "clear"
                }

                val personalizedEta = 25.5
                val googleEta = 20.0
                generateMessage(weatherCondition, personalizedEta, googleEta)

            } catch (e: Exception) {
                _notificationMessage.value = "오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
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
                val location = locationService.getCurrentLocation() ?: run {
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

    private fun generateMessage(weatherCondition: String, personalizedEta: Double, googleEta: Double) {
        val etaDifference = personalizedEta - googleEta

        val weatherText = when (weatherCondition) {
            "rain" -> "비가 오는 날씨입니다."
            "snow" -> "눈이 오는 날씨입니다."
            else -> "날씨가 맑습니다."
        }

        val message = when {
            weatherCondition == "rain" && etaDifference > 5 ->
                "☔️ $weatherText 평소보다 늦어요! ${personalizedEta.roundToInt()}분 예상됩니다."
            weatherCondition == "snow" && etaDifference > 5 ->
                "☃️ $weatherText 평소보다 늦어요! ${personalizedEta.roundToInt()}분 예상됩니다."
            etaDifference > 10 ->
                "🚗 교통량이 많네요. ${personalizedEta.roundToInt()}분 예상됩니다."
            etaDifference < -5 ->
                "🚀 길이 한산해서 평소보다 일찍 도착해요! ${personalizedEta.roundToInt()}분 예상됩니다."
            else ->
                "✅ $weatherText 평소와 비슷하게 ${googleEta.roundToInt()}분 걸려요."
        }
        _notificationMessage.postValue(message)
    }
}
