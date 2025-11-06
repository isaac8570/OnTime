package com.OnTime.ontime.service

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.OnTime.ontime.api.GeminiApiService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.data.models.WeatherData
import com.OnTime.ontime.util.Constants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RouteCalculationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val geminiService = GeminiApiService() // Assuming GeminiApiService can be instantiated like this

    override suspend fun doWork(): Result {
        return try {
            // 1. Get user's current location
            val locationProvider = LocationProvider(context)
            val currentLocation = awaitCurrentLocation(locationProvider)

            if (currentLocation == null) {
                Log.e("RouteWorker", "Failed to get user location.")
                return Result.failure()
            }
            val origin = "${currentLocation.latitude},${currentLocation.longitude}"

            // 2. Get event data from input
            val destination = inputData.getString("destination") ?: return Result.failure()
            val eventTitle = inputData.getString("eventTitle") ?: "다음 일정"

            // 3. Get travel time from Directions API
            val directionsService = RetrofitClient.directionsService
            val response = directionsService.getDirections(
                origin = origin,
                destination = destination,
                mode = Constants.MODE_TRANSIT, // Or get from user preferences
                apiKey = Constants.GOOGLE_MAPS_API_KEY
            )

            if (!response.isSuccessful || response.body() == null || response.body()!!.routes.isEmpty()) {
                Log.e("RouteWorker", "Failed to get directions: ${response.errorBody()?.string()}")
                return Result.failure()
            }

            val leg = response.body()!!.routes[0].legs[0]
            val travelDuration = leg.duration.text // e.g., "35 mins"

            // 4. Get weather data (currently hardcoded)
            val weatherData = WeatherData(
                temperature = "12°C",
                condition = "첫눈"
            )
            val weatherInfo = "${weatherData.temperature}, ${weatherData.condition}"

            // 5. Get user tone from input and convert to Enum
            val userToneString = inputData.getString("userTone") ?: "SOFT"
            val userTone = try {
                NotificationTone.valueOf(userToneString.uppercase())
            } catch (e: IllegalArgumentException) {
                NotificationTone.SOFT
            }

            // 6. Generate notification text with Gemini
            val notificationText = geminiService.generateNotificationText(
                eventTitle = eventTitle,
                destination = destination,
                transportMode = Constants.MODE_TRANSIT,
                estimatedDuration = travelDuration,
                userTone = userTone,
                weatherInfo = weatherInfo
            )

            // 7. Show notification
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showNotification(
                title = "출발 시간 알림",
                message = notificationText
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("RouteWorker", "Error in doWork", e)
            Result.failure()
        }
    }

    private suspend fun awaitCurrentLocation(locationProvider: LocationProvider): Location? {
        return suspendCancellableCoroutine { continuation ->
            locationProvider.getCurrentLocation(object : LocationProvider.LocationCallbackResult {
                override fun onLocationResult(location: Location?) {
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            })
        }
    }
}
