package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.util.LocationConverter
import kotlinx.coroutines.launch

class CalendarViewModel(application: Application, private val calendarRepository: CalendarRepository, private val weatherRepository: WeatherRepository) : AndroidViewModel(application) {
    
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadEvents() {
        _isLoading.value = true
        viewModelScope.launch {
            val fetchedEvents = calendarRepository.getEvents().toMutableList()
            fetchedEvents.forEachIndexed { index, event ->
                event.location?.let { location ->
                    val latLng = LocationConverter.addressToLatLng(getApplication(), location)
                    latLng?.let { (lat, lng) ->
                        val kmaGrid = LocationConverter.latLngToKmaGrid(lat, lng)
                        kmaGrid?.let { (nx, ny) ->
                            val weather = weatherRepository.getShortTermForecast(nx, ny)
                            fetchedEvents[index] = event.copy(weatherInfo = weather)
                        }
                    }
                }
            }
            _events.postValue(fetchedEvents)
            _isLoading.postValue(false)
        }
    }
    
    fun refreshEvents() {
        loadEvents()
    }
}

