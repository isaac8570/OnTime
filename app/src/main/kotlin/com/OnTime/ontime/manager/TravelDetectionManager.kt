package com.OnTime.ontime.manager

import android.content.Context
import android.location.Location
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.TravelLog
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.TravelDataRepository // Corrected import
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.util.LocationConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar // Import for Calendar object
import java.util.Date     // Import for Date object (수정됨)
import java.util.TimeZone // Import for TimeZone

class TravelDetectionManager(
    private val context: Context,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val travelDataRepository: TravelDataRepository, // Corrected to TravelDataRepository
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO) // Use IO dispatcher for network/disk ops
) {
    private var activeTrip: ActiveTrip? = null // Use a data class to hold trip state

    // Thresholds for detection
    private val MOVEMENT_THRESHOLD_METERS = 50.0 // How much moved to consider "started"
    private val ARRIVAL_THRESHOLD_METERS = 100.0 // How close to destination to consider "arrived"
    private val TIME_BEFORE_EVENT_MINUTES = 30 // Start monitoring this many minutes before event

    init {
        scope.launch {
            locationRepository.currentLocationUpdates.collect { location ->
                onNewLocation(location)
            }
        }
    }

    fun monitorEvent(event: CalendarEvent) {
        // Only monitor if not already monitoring an event
        if (activeTrip == null) {
            activeTrip = ActiveTrip(
                event = event,
                tripStartLocation = null, // Will be set when travel starts
                tripStartTime = 0L // Will be set when travel starts
            )
            println("Monitoring new event: ${event.title} at ${event.location}")
            // Potentially fetch initial originLatLng for the event here
        } else {
            println("Already monitoring an event. Ignoring: ${event.title}")
        }
    }

    private fun onNewLocation(currentLocation: Location) {
        activeTrip?.let { trip ->
            // If trip hasn't started yet, check if it should
            if (trip.tripStartLocation == null) {
                // Check if within time window to start travel
                val timeUntilEventStart = (trip.event.startTime - System.currentTimeMillis()) / (1000 * 60)
                if (timeUntilEventStart > 0 && timeUntilEventStart <= TIME_BEFORE_EVENT_MINUTES) {
                    // Event is upcoming, check for travel start
                    detectTravelStart(currentLocation, trip.event)
                }
            } else {
                // If trip has started, detect arrival
                detectArrival(currentLocation, trip.event, trip.tripStartLocation!!, trip.tripStartTime)
            }
        }
    }

    private fun detectTravelStart(currentLocation: Location, event: CalendarEvent) {
        event.originLatLng?.let { origin ->
            val distanceMoved = FloatArray(1)
            Location.distanceBetween(
                origin.first, origin.second,
                currentLocation.latitude, currentLocation.longitude,
                distanceMoved
            )

            if (distanceMoved[0] > MOVEMENT_THRESHOLD_METERS && activeTrip?.tripStartLocation == null) {
                // User has moved significantly from origin
                activeTrip = activeTrip?.copy(
                    tripStartLocation = currentLocation,
                    tripStartTime = System.currentTimeMillis()
                )
                println("Travel started for event: ${event.title} from ${origin.first},${origin.second}")
                // TODO: Notify a listener (e.g., MainViewModel) that travel has started
            }
        }
    }

    private fun detectArrival(
        currentLocation: Location,
        event: CalendarEvent,
        tripStartLocation: Location,
        tripStartTime: Long
    ) {
        event.destinationLatLng?.let { destination ->
            val distanceToDestination = FloatArray(1)
            Location.distanceBetween(
                destination.first, destination.second,
                currentLocation.latitude, currentLocation.longitude,
                distanceToDestination
            )

            if (distanceToDestination[0] < ARRIVAL_THRESHOLD_METERS && tripStartLocation != null) {
                // User has arrived at destination
                val actualTravelTimeMinutes = ((System.currentTimeMillis() - tripStartTime) / (1000 * 60)).toInt()
                println("Arrived at destination for event: ${event.title}. Actual time: $actualTravelTimeMinutes minutes")

                // Collect all data and save TravelLog
                scope.launch {
                    try {
                        val googleTravelInfo = locationRepository.calculateTravelTime(
                            originLatLng = Pair(tripStartLocation.latitude, tripStartLocation.longitude),
                            destinationLatLng = Pair(destination.first, destination.second),
                            mode = "driving" // Assuming driving for now, could be passed from event
                        )

                        val weather = weatherRepository.getWeatherCondition(
                            LocationConverter.latLngToKmaGrid(destination.first, destination.second)
                        ) // Get weather at destination

                        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = tripStartTime }
                        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // SUNDAY=1, MONDAY=2, ... SATURDAY=7

                        val travelLog = TravelLog(
                            userId = "test_user", // TODO: Replace with actual user ID
                            eventId = event.id,
                            eventTitle = event.title,
                            originLat = tripStartLocation.latitude,
                            originLng = tripStartLocation.longitude,
                            destinationLat = destination.first,
                            destinationLng = destination.second,
                            googleEtaMin = googleTravelInfo?.durationMinutes ?: 0,
                            actualEtaMin = actualTravelTimeMinutes,
                            weather = weather,
                            hourOfDay = hourOfDay,
                            dayOfWeek = dayOfWeek,
                            distanceKm = googleTravelInfo?.distanceText?.replace("[^0-9.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0,
                            timestamp = Date(System.currentTimeMillis()) // (수정됨) Long을 Date 객체로 변환
                        )
                        travelDataRepository.saveTravelLog(travelLog) // Corrected to travelDataRepository
                        println("TravelLog saved successfully for event: ${event.title}")
                    } catch (e: Exception) {
                        println("Error during data collection or saving TravelLog: ${e.message}")
                    }
                }

                // Reset manager for next event
                resetTripState()
            }
        }
    }

    private fun resetTripState() {
        activeTrip = null
        println("Trip state reset.")
    }

    // Call this if event is cancelled or passed without travel
    fun cancelMonitoring() {
        resetTripState()
    }
}

data class ActiveTrip(
    val event: CalendarEvent,
    val tripStartLocation: Location?,
    val tripStartTime: Long
)