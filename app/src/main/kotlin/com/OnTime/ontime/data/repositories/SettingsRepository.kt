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
        
        val savedNotificationToneName = prefs.getString("notification_tone", NotificationTone.FRIENDLY.name)
        val notificationTone = try {
            NotificationTone.valueOf(savedNotificationToneName ?: NotificationTone.FRIENDLY.name)
        } catch (e: IllegalArgumentException) {
            // 레거시 값이거나 유효하지 않은 값이 저장되어 있을 경우 기본값으로 설정
            NotificationTone.FRIENDLY
        }

        val notificationCount = prefs.getInt("notification_count", 2) // notificationCount 로드
        
        return UserPreferences(transportMode, notificationTone, notificationCount) // notificationCount 포함
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        prefs.edit()
            .putString("transport_mode", preferences.transportMode.name)
            .putString("notification_tone", preferences.notificationTone.name)
            .putInt("notification_count", preferences.notificationCount) // notificationCount 저장
            .apply()
    }
}