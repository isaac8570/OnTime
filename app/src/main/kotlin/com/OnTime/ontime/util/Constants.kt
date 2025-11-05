package com.OnTime.ontime.util

object Constants {
    
    // API Keys
    const val GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY"
    import com.OnTime.ontime.BuildConfig

    const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    
    // Transport Modes
    const val MODE_TRANSIT = "transit"
    const val MODE_DRIVING = "driving"
    const val MODE_WALKING = "walking"
    
    // Notification
    const val NOTIFICATION_ADVANCE_TIME_MINUTES = 10
}
