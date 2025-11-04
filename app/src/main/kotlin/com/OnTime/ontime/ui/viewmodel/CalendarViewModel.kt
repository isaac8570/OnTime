package com.OnTime.ontime.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.OnTime.ontime.data.models.CalendarEvent

class CalendarViewModel : ViewModel() {
    
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadEvents() {
        _isLoading.value = true
        // TODO: Load events from CalendarRepository
        _isLoading.value = false
    }
    
    fun refreshEvents() {
        loadEvents()
    }
}
