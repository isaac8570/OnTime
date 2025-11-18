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
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.TravelTimeRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.util.Constants
import com.OnTime.ontime.util.LocationConverter
import com.OnTime.ontime.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarViewModel(
    application: Application,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val travelTimeRepository: TravelTimeRepository // Injected from Factory
) : AndroidViewModel(application) {

    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _nextPageToken = MutableLiveData<String?>(null)

    private val _travelMode = MutableLiveData<String>(Constants.MODE_TRANSIT)
    val travelMode: LiveData<String> = _travelMode

    private val autoLearningService = com.OnTime.ontime.service.AutoLearningService(application)

    fun loadEventsWithTravelTime() {
        _isLoading.value = true
        Logger.d("Starting to load events.")
        viewModelScope.launch {
            try {
                val (events, nextPageToken) = calendarRepository.getEvents(10)
                _nextPageToken.postValue(nextPageToken)

                if (events.isNotEmpty()) {
                    Logger.d("Loaded ${events.size} events. Starting travel time calculation.")
                    val processedEvents = calculateTravelTimesFor(events)
                    _events.postValue(processedEvents)
                } else {
                    Logger.d("No events found from repository.")
                    _events.postValue(emptyList())
                }
            } catch (e: Exception) {
                Logger.e("Error loading events", e)
                _events.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
                Logger.d("Finished loading events process.")
            }
        }
    }

    fun loadMoreEvents() {
        if (_isLoadingMore.value == true || _nextPageToken.value == null) {
            return
        }

        _isLoadingMore.value = true
        Logger.d("Loading more events.")
        viewModelScope.launch {
            try {
                val (newEvents, nextPageToken) = calendarRepository.getEvents(10, _nextPageToken.value)
                _nextPageToken.postValue(nextPageToken)

                if (newEvents.isNotEmpty()) {
                    Logger.d("Loaded ${newEvents.size} more events. Processing them.")
                    val processedNewEvents = calculateTravelTimesFor(newEvents)
                    val currentEvents = _events.value.orEmpty()
                    _events.postValue(currentEvents + processedNewEvents)
                }
            } catch (e: Exception) {
                Logger.e("Error loading more events", e)
            } finally {
                _isLoadingMore.postValue(false)
            }
        }
    }

    private suspend fun calculateTravelTimesFor(events: List<CalendarEvent>): List<CalendarEvent> = withContext(Dispatchers.Default) {
        val currentMode = _travelMode.value ?: Constants.MODE_TRANSIT
        Logger.d("Calculating travel times for ${events.size} events with mode '$currentMode' in parallel.")

        // Run all calculations in parallel and wait for them to complete.
        val deferreds = events.map { event ->
            async {
                val eventWithStatus = event.copy(debugStatus = "계산 시작...")
                try {
                    val travelInfo = travelTimeRepository.calculateTravelTimeForEvent(eventWithStatus, currentMode)
                    if (travelInfo != null) {
                        Logger.d("Event '${event.title}': Success. Duration: ${travelInfo.durationText}")
                        eventWithStatus.copy(
                            travelDuration = travelInfo.durationText,
                            debugStatus = "성공: ${travelInfo.durationText} - 경로: ${travelInfo.routeDetails ?: "상세 정보 없음"}"
                        )
                    } else {
                        Logger.d("Event '${event.title}': Failed to calculate travel time.")
                        eventWithStatus.copy(
                            location = "장소모름",
                            debugStatus = "실패: 이동시간 계산 불가"
                        )
                    }
                } catch (e: Exception) {
                    Logger.e("Event '${event.title}': Exception during calculation.", e)
                    eventWithStatus.copy(debugStatus = "오류: ${e.message}")
                }
            }
        }
        deferreds.awaitAll()
    }

    fun setTravelMode(mode: String) {
        _travelMode.value = mode
        viewModelScope.launch {
            _events.value?.let { events ->
                Logger.d("Travel mode changed to $mode. Recalculating for ${events.size} events.")
                val processedEvents = calculateTravelTimesFor(events)
                _events.postValue(processedEvents)
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
                val currentLocation = locationRepository.getCurrentLocation()

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
                val destinationLatLng = com.OnTime.ontime.util.LocationConverter.addressToLatLng(getApplication(), destination)
                if (destinationLatLng == null) {
                    notificationManager.showDepartureNotification(
                        title = "테스트 실패",
                        message = "목적지 주소 변환 실패",
                        eventId = "error_geocode"
                    )
                    return@launch
                }

                val travelInfo = locationRepository.calculateTravelTime(
                    originLatLng = Pair(currentLocation.latitude, currentLocation.longitude),
                    destinationLatLng = destinationLatLng,
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
                val locationExtractor = com.OnTime.ontime.service.LocationExtractorService()
                val notificationManager = com.OnTime.ontime.service.NotificationManager(getApplication())
                // val historyService = com.OnTime.ontime.service.NotificationHistoryService(getApplication()) // REMOVED

                val realEvents = calendarRepository.getEvents(15)

                if (realEvents.first.isNotEmpty()) {
                    val firstEvent = realEvents.first.first()

                    val currentLocation = locationRepository.getCurrentLocation()
                    val myLocationText = if (currentLocation != null) {
                        "위도: ${String.format("%.4f", currentLocation.latitude)}, 경도: ${String.format("%.4f", currentLocation.longitude)}"
                    } else {
                        "위치 정보 없음"
                    }

                    val extractedLocation = locationExtractor.extractLocationFromText(
                        eventTitle = firstEvent.title,
                        eventDescription = firstEvent.description
                    )

                    val travelTime = if (extractedLocation != null) {
                        val extractedLatLng = com.OnTime.ontime.util.LocationConverter.addressToLatLng(getApplication(), extractedLocation)
                        if (extractedLatLng == null) {
                            "위치 주소 변환 실패"
                        } else if (currentLocation == null) {
                            "현재 위치를 가져올 수 없습니다"
                        } else {
                            val travelInfo = locationRepository.calculateTravelTime(
                                originLatLng = Pair(currentLocation.latitude, currentLocation.longitude),
                                destinationLatLng = extractedLatLng,
                                mode = Constants.MODE_TRANSIT
                            )
                            travelInfo?.durationText ?: "계산 실패"
                        }
                    } else {
                        "위치 추출 실패"
                    }

                    val ragMessage = ragService.generateRAGNotification(
                        event = firstEvent.copy(location = extractedLocation),
                        travelTime = travelTime,
                        weatherCondition = "맑음"
                    )

                    val fullMessage = "일정: ${firstEvent.title}\n내 위치: $myLocationText\n목적지: ${extractedLocation ?: "없음"}\n이동시간: $travelTime\n알림: $ragMessage"

                    // historyService.saveNotification(...) // REMOVED

                    notificationManager.showDepartureNotification(
                        title = "🧠 실제 일정 RAG 테스트",
                        message = fullMessage,
                        eventId = "rag_test_real"
                    )
                } else {
                    val message = "캘린더에 일정이 없습니다. 일정을 추가해보세요!"

                    // historyService.saveNotification(...) // REMOVED

                    notificationManager.showDepartureNotification(
                        title = "RAG 테스트",
                        message = message,
                        eventId = "no_events"
                    )
                }

            } catch (e: Exception) {
                val notificationManager = com.OnTime.ontime.service.NotificationManager(getApplication())
                // val historyService = com.OnTime.ontime.service.NotificationHistoryService(getApplication()) // REMOVED
                val errorMessage = "오류: ${e.message}"

                // historyService.saveNotification(...) // REMOVED

                notificationManager.showDepartureNotification(
                    title = "RAG 오류",
                    message = errorMessage,
                    eventId = "rag_error"
                )
            }
        }
    }
}