package com.OnTime.ontime.data.repositories

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.TravelInfo
import com.OnTime.ontime.service.LocationExtractorService
import com.OnTime.ontime.util.Constants
import com.OnTime.ontime.util.CustomLocationManager
import com.OnTime.ontime.util.LocationConverter // Import LocationConverter
import com.OnTime.ontime.util.Logger

class TravelTimeRepository(
    private val context: Context, // Keep context for LocationConverter
    private val locationRepository: LocationRepository, // Inject LocationRepository
    private val customLocationManager: CustomLocationManager
) {

    private val locationExtractor = LocationExtractorService()

    suspend fun calculateTravelTimeForEvent(
        event: CalendarEvent,
        transportMode: String = Constants.MODE_TRANSIT
    ): TravelInfo? {
        Logger.d("Processing event: '${event.title}' (ID: ${event.id})")
        val currentLocation = locationRepository.getCurrentLocation() ?: run {
            Logger.d("Could not get current location for event '${event.title}'. Returning null.")
            return null
        }
        Logger.d("Current location retrieved: ${currentLocation.latitude}, ${currentLocation.longitude}")

        var destinationAddress: String? = null
        var locationSource: String = "None"

        // Priority 1: Explicit event.location
        if (!event.location.isNullOrBlank()) {
            destinationAddress = event.location
            locationSource = "Explicit Event Location"
            Logger.d("Using explicit event location: '$destinationAddress'")
        }

        // Priority 2: CustomLocationManager
        if (destinationAddress.isNullOrBlank()) {
            val customLoc = customLocationManager.findLocationFromTitle(event.title)
            if (customLoc != null) {
                destinationAddress = customLoc
                locationSource = "Custom Location Manager"
                Logger.d("Found location from CustomLocationManager for '${event.title}': '$destinationAddress'")
            } else {
                Logger.d("CustomLocationManager did not find a match for '${event.title}'.")
            }
        }

        // Priority 3: LocationExtractorService (AI-based)
        if (destinationAddress.isNullOrBlank()) {
            val aiLoc = locationExtractor.extractLocationFromText(eventTitle = event.title, eventDescription = event.description)
            if (aiLoc != null) {
                destinationAddress = aiLoc
                locationSource = "AI Location Extractor"
                Logger.d("Extracted location with AI for '${event.title}': '$destinationAddress'")
            } else {
                Logger.d("AI Location Extractor did not find a match for '${event.title}'.")
            }
        }

        if (destinationAddress.isNullOrBlank()) {
            Logger.d("No destination address found for event '${event.title}' after checking all sources. Returning null.")
            return null
        }
        Logger.i("Final destination address for '${event.title}' (Source: $locationSource): '$destinationAddress'")

        val destinationLatLng = LocationConverter.addressToLatLng(context, destinationAddress)
        if (destinationLatLng == null) {
            Logger.e("Geocoding failed for address: '$destinationAddress'. Returning null.")
            return null
        }
        Logger.d("Geocoding successful for '$destinationAddress': $destinationLatLng")

        return locationRepository.calculateTravelTime(
            originLatLng = Pair(currentLocation.latitude, currentLocation.longitude),
            destinationLatLng = destinationLatLng,
            mode = transportMode
        )
    }

    suspend fun calculateTravelTimeForEvents(
        events: List<CalendarEvent>,
        transportMode: String = Constants.MODE_TRANSIT
    ): Map<String, TravelInfo> {
        val results = mutableMapOf<String, TravelInfo>()

        events.forEach { event ->
            calculateTravelTimeForEvent(event, transportMode)?.let { travelInfo ->
                results[event.id] = travelInfo
            }
        }

        return results
    }
}
