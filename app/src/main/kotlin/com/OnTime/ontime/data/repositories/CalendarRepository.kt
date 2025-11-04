package com.OnTime.ontime.data.repositories

import com.OnTime.ontime.data.models.CalendarEvent

class CalendarRepository {
    
    suspend fun getEvents(): List<CalendarEvent> {
        // TODO: Implement Google Calendar API call
        return emptyList()
    }
    
    suspend fun getEventById(eventId: String): CalendarEvent? {
        // TODO: Implement event retrieval
        return null
    }
}
