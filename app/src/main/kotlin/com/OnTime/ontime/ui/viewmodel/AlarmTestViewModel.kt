package com.OnTime.ontime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.service.NotificationScheduler
import kotlinx.coroutines.launch

class AlarmTestViewModel(private val notificationScheduler: NotificationScheduler) : ViewModel() {

    fun sendTestNotification() {
        viewModelScope.launch {
            notificationScheduler.sendInstantTestNotification()
        }
    }
}
