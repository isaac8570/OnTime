package com.OnTime.ontime.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.TravelTimeRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.util.Constants
import kotlinx.coroutines.launch

class CalendarViewModel(
    application: Application,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository
) : AndroidViewModel(application) {
    
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _travelMode = MutableLiveData<String>(Constants.MODE_TRANSIT)
    val travelMode: LiveData<String> = _travelMode
    
    private val travelTimeRepository = TravelTimeRepository(application)
    private val autoLearningService = com.OnTime.ontime.service.AutoLearningService(application)
    
    fun loadEventsWithTravelTime() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val events = calendarRepository.getEvents(getApplication())
                _events.postValue(events)
                
                // Calculate travel times for all events
                calculateTravelTimesForEvents(events)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    private suspend fun calculateTravelTimesForEvents(events: List<CalendarEvent>) {
        val currentMode = _travelMode.value ?: Constants.MODE_TRANSIT
        
        events.forEach { event ->
            try {
                val travelInfo = travelTimeRepository.calculateTravelTimeForEvent(event, currentMode)
                if (travelInfo != null) {
                    // 자동 학습 예약
                    autoLearningService.scheduleAutoLearning(event, travelInfo.durationText)
                    
                    val updatedEvents = _events.value?.map { 
                        if (it.id == event.id) {
                            it.copy(travelDuration = travelInfo.durationText)
                        } else it
                    }
                    _events.postValue(updatedEvents ?: emptyList())
                }
            } catch (e: Exception) {
                // 개별 이벤트 오류 처리
            }
        }
    }
    
    fun setTravelMode(mode: String) {
        _travelMode.value = mode
        _events.value?.let { events ->
            viewModelScope.launch {
                calculateTravelTimesForEvents(events)
            }
        }
    }
    
    fun refreshEvents() {
        loadEventsWithTravelTime()
    }
    
    fun testTravelCalculation() {
        viewModelScope.launch {
            try {
                val notificationManager = com.OnTime.ontime.service.NotificationManager(getApplication())
                
                // 1단계: 위치 권한 확인
                val hasPermission = ContextCompat.checkSelfPermission(
                    getApplication(), 
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (!hasPermission) {
                    notificationManager.showDepartureNotification(
                        title = "테스트 실패",
                        message = "위치 권한이 없습니다",
                        eventId = "error_permission"
                    )
                    return@launch
                }
                
                // 2단계: 현재 위치 확인
                val locationService = com.OnTime.ontime.service.LocationService(getApplication())
                val currentLocation = locationService.getCurrentLocation()
                
                if (currentLocation == null) {
                    notificationManager.showDepartureNotification(
                        title = "테스트 실패", 
                        message = "현재 위치를 가져올 수 없습니다",
                        eventId = "error_location"
                    )
                    return@launch
                }
                
                // 3단계: 영문 주소로 테스트
                val destination = "Hongik University Station, Seoul"
                
                notificationManager.showDepartureNotification(
                    title = "API 키 확인",
                    message = "API Key: ${Constants.GOOGLE_MAPS_API_KEY.take(10)}...",
                    eventId = "api_check"
                )
                
                // 4단계: Google Maps API 호출
                val travelInfo = locationService.calculateTravelTime(
                    currentLocation = currentLocation,
                    destinationAddress = destination,
                    mode = Constants.MODE_TRANSIT
                )
                
                if (travelInfo != null) {
                    notificationManager.showDepartureNotification(
                        title = "테스트 성공!",
                        message = "홍대까지: ${travelInfo.durationText} (${travelInfo.distanceText})",
                        eventId = "success"
                    )
                } else {
                    notificationManager.showDepartureNotification(
                        title = "테스트 실패",
                        message = "Google Maps API 호출 실패 - API 키 확인 필요",
                        eventId = "error_api"
                    )
                }
                
            } catch (e: Exception) {
                val notificationManager = com.OnTime.ontime.service.NotificationManager(getApplication())
                notificationManager.showDepartureNotification(
                    title = "테스트 오류",
                    message = "오류: ${e.message}",
                    eventId = "error_exception"
                )
            }
        }
    }
    
    fun testRAGSystem() {
        viewModelScope.launch {
            try {
                val ragService = com.OnTime.ontime.service.FirebaseRAGService()
                val notificationManager = com.OnTime.ontime.service.NotificationManager(getApplication())
                
                val testEvent = CalendarEvent(
                    id = "rag_test",
                    title = "홍대 만나기", 
                    location = "홍대입구역",
                    description = null,
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis()
                )
                
                val ragMessage = ragService.generateRAGNotification(
                    event = testEvent,
                    travelTime = "25분",
                    weatherCondition = "맑음"
                )
                
                notificationManager.showDepartureNotification(
                    title = "🧠 RAG 테스트 성공!",
                    message = ragMessage,
                    eventId = "rag_test"
                )
                
            } catch (e: Exception) {
                val notificationManager = com.OnTime.ontime.service.NotificationManager(getApplication())
                notificationManager.showDepartureNotification(
                    title = "RAG 오류",
                    message = "오류: ${e.message}",
                    eventId = "rag_error"
                )
            }
        }
    }
}
