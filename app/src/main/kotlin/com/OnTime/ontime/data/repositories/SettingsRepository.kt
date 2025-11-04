package com.OnTime.ontime.data.repositories

import android.content.Context
import android.content.SharedPreferences
import com.OnTime.ontime.data.models.NotificationTone
import com.OnTime.ontime.data.models.TransportMode
import com.OnTime.ontime.data.models.UserPreferences

class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("ontime_prefs", Context.MODE_PRIVATE)
    
    fun getUserPreferences(): UserPreferences {
        val transportMode = TransportMode.valueOf(
            prefs.getString("transport_mode", TransportMode.TRANSIT.name) ?: TransportMode.TRANSIT.name
        )
        val notificationTone = NotificationTone.valueOf(
            prefs.getString("notification_tone", NotificationTone.SOFT.name) ?: NotificationTone.SOFT.name
        )
        return UserPreferences(transportMode, notificationTone)
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        prefs.edit()
            .putString("transport_mode", preferences.transportMode.name)
            .putString("notification_tone", preferences.notificationTone.name)
            .apply()
    }
}
