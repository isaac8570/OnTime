package com.OnTime.ontime.manager

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.TravelLogRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the overall travel monitoring process, coordinating between calendar events,
 * location updates, travel detection, and data storage.
 */
class TravelMonitorManager private constructor( // Private constructor for Singleton
    private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val travelLogRepository: TravelLogRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default) // Use Default for main management
) {
    // Companion object for Singleton instance
    companion object {
        @Volatile private var INSTANCE: TravelMonitorManager? = null

        fun getInstance(
            context: Context,
            calendarRepository: CalendarRepository,
            locationRepository: LocationRepository,
            weatherRepository: WeatherRepository,
            travelLogRepository: TravelLogRepository
        ): TravelMonitorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TravelMonitorManager(
                    context,
                    calendarRepository,
                    locationRepository,
                    weatherRepository,
                    travelLogRepository
                ).also { INSTANCE = it }
            }
        }
    }

    private val travelDetectionManager = TravelDetectionManager(
        context, locationRepository, weatherRepository, travelLogRepository, scope
    )

    // Method to be called by CalendarSyncWorker
    fun onCalendarEventsFetched(events: List<CalendarEvent>) {
        // Filter events that are relevant for monitoring
        val upcomingEvents = events.filter { event ->
            // Event should have a location and start within a reasonable timeframe (e.g., next few hours)
            event.location != null && event.destinationLatLng != null &&
            (event.startTime - System.currentTimeMillis()) > 0 &&
            (event.startTime - System.currentTimeMillis()) < (4 * 60 * 60 * 1000) // Within next 4 hours
        }

        if (upcomingEvents.isNotEmpty()) {
            // For simplicity, let's monitor the first upcoming event found
            // In a real app, you might have more sophisticated selection logic
            travelDetectionManager.monitorEvent(upcomingEvents.first())
        } else {
            travelDetectionManager.cancelMonitoring() // No relevant events, stop monitoring
        }
    }

    // Call this to start listening for location updates (if not already handled by MainViewModel)
    fun startMonitoring() {
        // MainViewModel should handle starting LocationService
        // This manager will observe location updates through locationRepository.currentLocationUpdates
        // which is already done in TravelDetectionManager init block.
    }

    fun stopMonitoring() {
        travelDetectionManager.cancelMonitoring()
    }
}
