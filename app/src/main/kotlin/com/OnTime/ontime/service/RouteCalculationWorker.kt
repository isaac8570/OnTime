package com.OnTime.ontime.service

import android.content.Context
import android.location.Location
import androidx.work.*
import com.OnTime.ontime.api.DirectionsService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class RouteCalculationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val directionsService = RetrofitClient.directionsService

    // ★★★ [수정 1] calendarRepository 생성 시 applicationContext를 전달합니다. ★★★
    private val calendarRepository = CalendarRepository(applicationContext)

    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // 'notificationScheduler'를 선언만 합니다.
    private val notificationScheduler: NotificationScheduler

    // init 블록에서 context를 사용하여 초기화합니다.
    init {
        notificationScheduler = NotificationScheduler(context)
    }

    override suspend fun doWork(): Result {
        return try {
            // ★★★ [수정 2] getEvents()를 호출할 때 context 파라미터를 제거합니다. ★★★
            val upcomingEvents = calendarRepository.getEvents()

            for (event in upcomingEvents) {
                if (!event.location.isNullOrEmpty()) {
                    calculateAndScheduleNotification(event)
                }
            }

            Result.success()
        } catch (e: Exception) {
            // 실패 시 재시도하도록 설정합니다.
            Result.retry()
        }
    }

    private suspend fun calculateAndScheduleNotification(event: CalendarEvent) {
        try {
            val currentLocation = getCurrentLocation()
            // event.location이 null이 아님을 !!로 단언하기보다 안전하게 처리합니다.
            event.location?.let { destination ->
                val travelTime = calculateTravelTime(currentLocation, destination)

                if (travelTime > 0) {
                    val travelTimeMillis = travelTime * 1000L
                    val bufferMillis = 15 * 60 * 1000L // 15분 여유 시간
                    val departureTime = event.startTime - travelTimeMillis - bufferMillis

                    if (departureTime > System.currentTimeMillis()) {
                        notificationScheduler.scheduleNotification(
                            event = event,
                            departureTime = departureTime,
                            travelTimeMinutes = travelTime / 60
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // 에러 로그를 남기는 것이 좋습니다. (예: Log.e("RouteCalculationWorker", "Error calculating notification", e))
        }
    }

    private suspend fun getCurrentLocation(): String {
        // 위치 권한이 거부되었을 때 발생하는 SecurityException을 처리합니다.
        return try {
            val location: Location? = locationClient.lastLocation.await()
            // location이 null일 경우를 대비하여 기본값을 사용합니다.
            location?.let { "${it.latitude},${it.longitude}" } ?: "37.5665,126.9780" // 위치 정보 없을 시 서울을 기본값으로 사용
        } catch (e: SecurityException) {
            // 위치 권한이 없을 경우
            "37.5665,126.9780" // 서울 기본값
        } catch (e: Exception) {
            // 그 외 예외 발생 시
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

            // optional 체이닝으로 더 안전하게 접근합니다.
            response.body()?.routes?.firstOrNull()?.legs?.firstOrNull()?.duration?.value ?: 0
        } catch (e: Exception) {
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
