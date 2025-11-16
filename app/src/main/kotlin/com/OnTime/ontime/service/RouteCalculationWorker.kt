package com.OnTime.ontime.service

import android.content.Context
import android.location.Location
import androidx.work.*
import com.OnTime.ontime.NotificationBuilder // Import NotificationBuilder
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.NotificationRecord // Import NotificationRecord
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.NotificationRepository // Import NotificationRepository
import com.OnTime.ontime.data.repositories.SettingsRepository // Import SettingsRepository
import com.OnTime.ontime.data.repositories.WeatherRepository // Import WeatherRepository
import com.OnTime.ontime.api.DirectionsService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.util.Constants
import com.OnTime.ontime.util.LocationConverter // Import LocationConverter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import java.util.Calendar // For getting day of week
import java.util.Date // For NotificationRecord timestamp

class RouteCalculationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val directionsService = RetrofitClient.directionsService

    private val calendarRepository = CalendarRepository(applicationContext)
    private val settingsRepository = SettingsRepository(applicationContext)
    private val weatherRepository = WeatherRepository()
    private val notificationBuilder = NotificationBuilder()
    private val notificationRepository = NotificationRepository() // Initialize NotificationRepository

    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val notificationScheduler: NotificationScheduler

    init {
        notificationScheduler = NotificationScheduler(applicationContext, notificationBuilder, settingsRepository)
    }

    override suspend fun doWork(): Result {
        return try {
            val upcomingEvents = calendarRepository.getEvents()

            for (event in upcomingEvents) {
                if (!event.location.isNullOrEmpty()) {
                    calculateAndScheduleNotification(event)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun calculateAndScheduleNotification(event: CalendarEvent) {
        try {
            val currentLocationString = getCurrentLocation()
            event.location?.let { destinationAddress ->
                val estimatedTravelTimeSeconds = calculateTravelTime(currentLocationString, destinationAddress)
                val estimatedTravelTimeMinutes = estimatedTravelTimeSeconds / 60

                if (estimatedTravelTimeMinutes > 0) {
                    // --- 모델 예측 플레이스홀더: predictedActualTravelTime 계산 ---
                    val predictedRatio = 1.0 + (Random.nextDouble(-0.1, 0.1)) // -10% ~ +10%
                    val predictedActualTravelTimeMinutes = (estimatedTravelTimeMinutes * predictedRatio).toInt()

                    // --- 날씨 정보 fetch ---
                    var weatherInfoString: String = "날씨 정보 없음"
                    val destinationLatLng = LocationConverter.addressToLatLng(applicationContext, destinationAddress)
                    if (destinationLatLng != null) {
                        val gridCoords = LocationConverter.latLngToKmaGrid(destinationLatLng.first, destinationLatLng.second)
                        weatherInfoString = weatherRepository.getWeatherCondition(gridCoords)
                    }

                    val travelTimeMillis = estimatedTravelTimeSeconds * 1000L
                    val bufferMillis = 15 * 60 * 1000L // 15분 여유 시간
                    val departureTime = event.startTime - travelTimeMillis - bufferMillis

                    if (departureTime > System.currentTimeMillis()) {
                        val generatedNotificationMessage = notificationScheduler.scheduleNotification( // Get message from scheduler
                            event = event,
                            departureTime = departureTime,
                            estimatedTravelTimeMinutes = estimatedTravelTimeMinutes,
                            predictedActualTravelTimeMinutes = predictedActualTravelTimeMinutes,
                            weatherInfo = weatherInfoString
                        )

                        // Save NotificationRecord to Firebase
                        generatedNotificationMessage?.let { msg ->
                            val userPreferences = settingsRepository.getUserPreferences()
                            val notificationRecord = NotificationRecord(
                                eventId = event.id,
                                eventTitle = event.title,
                                notificationMessage = msg,
                                timestamp = Date(), // Current time
                                estimatedTravelTimeMinutes = estimatedTravelTimeMinutes,
                                actualTravelTimeMinutes = predictedActualTravelTimeMinutes ?: 0, // Use 0 if null
                                weatherInfo = weatherInfoString,
                                messageTone = userPreferences.notificationTone.description
                            )
                            notificationRepository.saveNotificationRecord(notificationRecord)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getCurrentLocation(): String {
        return try {
            val location: Location? = locationClient.lastLocation.await()
            location?.let { "${it.latitude},${it.longitude}" } ?: "37.5665,126.9780" // 위치 정보 없을 시 서울을 기본값으로 사용
        } catch (e: SecurityException) {
            "37.5665,126.9780" // 서울 기본값
        } catch (e: Exception) {
            "37.5665,126.9780" // 서울 기본값
        }
    }

    private suspend fun calculateTravelTime(origin: String, destination: String): Int {
        return try {
            val response = directionsService.getDirections(
                origin = origin,
                destination = destination,
                mode = "transit", // 대중교통
                apiKey = Constants.GOOGLE_MAPS_API_KEY
            )

            response.body()?.routes?.firstOrNull()?.legs?.firstOrNull()?.duration?.value ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    companion object {
        const val KEY_TRAVEL_DURATION = "travel_duration"

        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RouteCalculationWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "route_calculation",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}