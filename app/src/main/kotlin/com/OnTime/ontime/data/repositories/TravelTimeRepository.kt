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
    private val locationRepository: LocationRepository // Inject LocationRepository
) {

    private val locationExtractor = LocationExtractorService()

    suspend fun calculateTravelTimeForEvent(
        event: CalendarEvent,
        transportMode: String = Constants.MODE_TRANSIT
    ): TravelInfo? {
        Logger.d("Processing event: '${event.title}'")
        val currentLocation = locationRepository.getCurrentLocation() ?: run {
            Logger.d("Could not get current location for event '${event.title}'")
            return null
        }

        // Location extraction with priority
        val destinationAddress = event.location.takeIf { !it.isNullOrBlank() }?.also {
            Logger.d("Using explicit location: '$it'")
        } ?: CustomLocationManager.findLocationFromTitle(event.title)?.also {
            Logger.d("Found location from CustomLocationManager: '$it'")
        } ?: locationExtractor.extractLocationFromText(
            eventTitle = event.title,
            eventDescription = event.description
        )?.also {
            Logger.d("Extracted location with AI: '$it'")
        }

        if (destinationAddress.isNullOrBlank()) {
            Logger.d("No destination address found for event '${event.title}'")
            return null
        }
        Logger.i("Final destination address for '${event.title}': '$destinationAddress'")

        val destinationLatLng = LocationConverter.addressToLatLng(context, destinationAddress)
        if (destinationLatLng == null) {
            Logger.e("Geocoding failed for address: '$destinationAddress'")
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
