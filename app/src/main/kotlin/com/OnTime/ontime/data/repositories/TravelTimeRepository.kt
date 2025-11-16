package com.OnTime.ontime.data.repositories

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.models.TravelInfo
import com.OnTime.ontime.service.LocationExtractorService
import com.OnTime.ontime.util.Constants
import com.OnTime.ontime.util.LocationConverter // Import LocationConverter

class TravelTimeRepository(
    private val context: Context, // Keep context for LocationConverter
    private val locationRepository: LocationRepository // Inject LocationRepository
) {
    
    private val locationExtractor = LocationExtractorService()
    
    suspend fun calculateTravelTimeForEvent(
        event: CalendarEvent,
        transportMode: String = Constants.MODE_TRANSIT
    ): TravelInfo? {
        val currentLocation = locationRepository.getCurrentLocation() ?: return null
        
        // 1. 기존 location 필드 확인
        var destinationAddress = event.location
        
        // 2. location이 없으면 AI로 텍스트에서 장소 추출
        if (destinationAddress.isNullOrBlank()) {
            destinationAddress = locationExtractor.extractLocationFromText(
                eventTitle = event.title,
                eventDescription = event.description
            )
        }
        
        return if (!destinationAddress.isNullOrBlank()) {
            val destinationLatLng = LocationConverter.addressToLatLng(context, destinationAddress)
            if (destinationLatLng != null) {
                locationRepository.calculateTravelTime(
                    originLatLng = Pair(currentLocation.latitude, currentLocation.longitude),
                    destinationLatLng = destinationLatLng,
                    mode = transportMode
                )
            } else null
        } else null
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
