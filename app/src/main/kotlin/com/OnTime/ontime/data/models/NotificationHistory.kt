package com.OnTime.ontime.data.models

data class NotificationHistory(
    val id: String,
    val title: String,
    val message: String,
    val eventTitle: String?,
    val myLocation: String?, // 내 위치
    val extractedLocation: String?, // 목적지
    val travelTime: String?,
    val ragMessage: String?,
    val timestamp: Long,
    val type: String // "RAG", "TRAVEL", "ERROR" 등
)
