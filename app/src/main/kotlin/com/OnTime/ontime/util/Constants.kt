package com.OnTime.ontime.util
import com.OnTime.ontime.BuildConfig


object Constants {
    
    // API Keys
    const val GOOGLE_MAPS_API_KEY = BuildConfig.MAPS_API_KEY

    const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    
    // Transport Modes
    const val MODE_TRANSIT = "transit"
    const val MODE_DRIVING = "driving"
    const val MODE_WALKING = "walking"
    
    // Notification
    const val NOTIFICATION_ADVANCE_TIME_MINUTES = 10
}
