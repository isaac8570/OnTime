package com.OnTime.ontime.service

import android.content.Context
import android.content.SharedPreferences
import com.OnTime.ontime.data.models.NotificationHistory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificationHistoryService(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveNotification(
        title: String,
        message: String,
        eventTitle: String? = null,
        myLocation: String? = null,
        extractedLocation: String? = null,
        travelTime: String? = null,
        ragMessage: String? = null,
        type: String = "GENERAL"
    ) {
        val history = getNotificationHistory().toMutableList()
        
        val notification = NotificationHistory(
            id = System.currentTimeMillis().toString(),
            title = title,
            message = message,
            eventTitle = eventTitle,
            myLocation = myLocation,
            extractedLocation = extractedLocation,
            travelTime = travelTime,
            ragMessage = ragMessage,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        
        history.add(0, notification) // 최신 순으로 추가
        
        // 최근 50개만 유지
        if (history.size > 50) {
            history.removeAt(history.size - 1)
        }
        
        prefs.edit().putString("notifications", gson.toJson(history)).apply()
    }
    
    fun getNotificationHistory(): List<NotificationHistory> {
        val json = prefs.getString("notifications", "[]")
        val type = object : TypeToken<List<NotificationHistory>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun clearHistory() {
        prefs.edit().remove("notifications").apply()
    }
}
