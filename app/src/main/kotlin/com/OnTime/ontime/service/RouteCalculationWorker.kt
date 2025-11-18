package com.OnTime.ontime.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.OnTime.ontime.NotificationBuilder
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.NotificationRecord
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.NotificationRepository
import com.OnTime.ontime.data.repositories.SettingsRepository
import com.OnTime.ontime.data.repositories.TravelTimeRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.util.Constants
import com.OnTime.ontime.util.CustomLocationManager
import com.OnTime.ontime.util.Logger
import java.util.Date
import java.util.concurrent.TimeUnit

class RouteCalculationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // --- Dependency Injection Setup ---
    private val settingsRepository: SettingsRepository
    private val locationRepository: LocationRepository
    private val customLocationManager: CustomLocationManager
    private val travelTimeRepository: TravelTimeRepository
    private val calendarRepository: CalendarRepository
    private val notificationScheduler: NotificationScheduler
    private val notificationRepository: NotificationRepository
    private val weatherRepository: WeatherRepository

    init {
        // Manually create the dependency chain, similar to the ViewModelFactory
        settingsRepository = SettingsRepository(applicationContext)
        locationRepository = LocationRepository(applicationContext)
        customLocationManager = CustomLocationManager(settingsRepository)
        travelTimeRepository = TravelTimeRepository(applicationContext, locationRepository, customLocationManager)
        calendarRepository = CalendarRepository(applicationContext)
        val notificationBuilder = NotificationBuilder()
        notificationScheduler = NotificationScheduler(applicationContext, notificationBuilder, settingsRepository)
        notificationRepository = NotificationRepository()
        weatherRepository = WeatherRepository()
    }

    override suspend fun doWork(): Result {
        Logger.d("[Worker] Starting background work.")
        return try {
            // Fetch upcoming events (e.g., in the next 24 hours)
            val (upcomingEvents, _) = calendarRepository.getEvents(10) // Limit to 10 for now
            Logger.d("[Worker] Found ${upcomingEvents.size} upcoming events.")

            for (event in upcomingEvents) {
                // Only process future events
                if (event.startTime > System.currentTimeMillis()) {
                    calculateAndScheduleNotification(event)
                }
            }
            Logger.d("[Worker] Finished background work.")
            Result.success()
        } catch (e: Exception) {
            Logger.e("[Worker] Error during doWork", e)
            Result.retry()
        }
    }

    private suspend fun calculateAndScheduleNotification(event: CalendarEvent) {
        Logger.d("[Worker] Calculating notification for event: '${event.title}'")
        try {
            // This logic is now identical to the ViewModel's for consistency
            // 1. Initial Guess (2 hours before event)
            val initialDepartureGuess = event.startTime - TimeUnit.HOURS.toMillis(2)
            val travelInfo1 = travelTimeRepository.calculateTravelTimeForEvent(event, Constants.MODE_TRANSIT, initialDepartureGuess)

            // 2. Refined Guess
            val refinedDepartureTime = if (travelInfo1 != null) event.startTime - TimeUnit.MINUTES.toMillis(travelInfo1.durationMinutes.toLong()) else null
            val finalTravelInfo = if (refinedDepartureTime != null) {
                travelTimeRepository.calculateTravelTimeForEvent(event, Constants.MODE_TRANSIT, refinedDepartureTime)
            } else {
                travelInfo1
            }

            // 3. Schedule Notification
            if (finalTravelInfo != null) {
                val finalDepartureTime = event.startTime - TimeUnit.MINUTES.toMillis(finalTravelInfo.durationMinutes.toLong())

                // Ensure we don't schedule notifications for the past
                if (finalDepartureTime > System.currentTimeMillis()) {
                    Logger.i("[Worker] Scheduling notification for '${event.title}' at $finalDepartureTime")
                    notificationScheduler.scheduleNotification(
                        event = event,
                        departureTime = finalDepartureTime,
                        estimatedTravelTimeMinutes = finalTravelInfo.durationMinutes,
                        predictedActualTravelTimeMinutes = null, // Placeholder
                        weatherInfo = "맑음" // Placeholder
                    )
                } else {
                    Logger.d("[Worker] Calculated departure time for '${event.title}' is in the past. Skipping notification.")
                }
            } else {
                Logger.e("[Worker] Could not calculate travel time for event '${event.title}'.")
            }
        } catch (e: Exception) {
            Logger.e("[Worker] Failed to process and schedule notification for event '${event.title}'", e)
        }
    }
}
