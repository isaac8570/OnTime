package com.OnTime.ontime.data.repositories

import android.content.Context
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.service.LocationService
import com.OnTime.ontime.service.LocationExtractorService
import com.OnTime.ontime.service.TravelInfo
import com.OnTime.ontime.util.Constants

class TravelTimeRepository(context: Context) {
    
    private val locationService = LocationService(context)
    private val locationExtractor = LocationExtractorService()
    
    suspend fun calculateTravelTimeForEvent(
        event: CalendarEvent,
        transportMode: String = Constants.MODE_TRANSIT
    ): TravelInfo? {
        val currentLocation = locationService.getCurrentLocation() ?: return null
        
        // 1. 기존 location 필드 확인
        var destination = event.location
        
        // 2. location이 없으면 AI로 텍스트에서 장소 추출
        if (destination.isNullOrBlank()) {
            destination = locationExtractor.extractLocationFromText(
                eventTitle = event.title,
                eventDescription = event.description
            )
        }
        
        return if (!destination.isNullOrBlank()) {
            locationService.calculateTravelTime(
                currentLocation = currentLocation,
                destinationAddress = destination,
                mode = transportMode
            )
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
