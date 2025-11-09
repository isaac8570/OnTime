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
    private val calendarRepository = CalendarRepository()
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val notificationScheduler = NotificationScheduler(context)

    override suspend fun doWork(): Result {
        return try {
            val upcomingEvents = calendarRepository.getEvents(applicationContext)
            
            for (event in upcomingEvents) {
                if (!event.location.isNullOrEmpty()) {
                    calculateAndScheduleNotification(event)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun calculateAndScheduleNotification(event: CalendarEvent) {
        try {
            val currentLocation = getCurrentLocation()
            val travelTime = calculateTravelTime(currentLocation, event.location!!)
            
            if (travelTime > 0) {
                val departureTime = event.startTime - (travelTime * 1000) - (15 * 60 * 1000) // 15분 여유
                
                if (departureTime > System.currentTimeMillis()) {
                    notificationScheduler.scheduleNotification(
                        event = event,
                        departureTime = departureTime,
                        travelTimeMinutes = travelTime / 60
                    )
                }
            }
        } catch (e: Exception) {
            // 로그 처리
        }
    }

    private suspend fun getCurrentLocation(): String {
        return try {
            val location = locationClient.lastLocation.await()
            "${location.latitude},${location.longitude}"
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
            
            if (response.isSuccessful && response.body()?.routes?.isNotEmpty() == true) {
                response.body()!!.routes[0].legs[0].duration.value
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    companion object {
        const val KEY_TRAVEL_DURATION = "travel_duration"
        
        fun schedulePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RouteCalculationWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "route_calculation",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
