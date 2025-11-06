package com.OnTime.ontime.service

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.OnTime.ontime.api.GeminiApiService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.util.Constants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RouteCalculationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TRAVEL_DURATION = "KEY_TRAVEL_DURATION"
    }

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
                mode = Constants.MODE_TRANSIT,
                apiKey = Constants.GOOGLE_MAPS_API_KEY
            )

            if (!response.isSuccessful || response.body() == null || response.body()!!.routes.isEmpty()) {
                Log.e("RouteWorker", "Failed to get directions: ${response.errorBody()?.string()}")
                return Result.failure()
            }

            val leg = response.body()!!.routes[0].legs[0]
            val travelDuration = leg.duration.text // e.g., "35 mins"

            // 4. (Weather data is on hold as requested)

            // 5. Generate notification text with Gemini
            val geminiService = GeminiApiService()
            val notificationText = geminiService.generateNotificationText(
                eventTitle = eventTitle,
                destination = destination,
                transportMode = Constants.MODE_TRANSIT,
                estimatedDuration = travelDuration,
                userTone = NotificationTone.SOFT, // Using a default tone for now
                weatherInfo = null // Weather is on hold
            )

            // 6. Show notification
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showNotification(
                title = "출발 시간 알림",
                message = notificationText
            )

            // 7. Output the calculated travel duration
            val outputData = workDataOf(KEY_TRAVEL_DURATION to travelDuration)
            Result.success(outputData)

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
