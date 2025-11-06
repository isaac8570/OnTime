package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.OnTime.ontime.data.models.CalendarEvent
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.service.RouteCalculationWorker
import com.OnTime.ontime.util.LocationConverter
import kotlinx.coroutines.launch

class CalendarViewModel(application: Application, private val calendarRepository: CalendarRepository, private val weatherRepository: WeatherRepository) : AndroidViewModel(application) {
    
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val workManager = WorkManager.getInstance(application)
    
    fun loadEvents() {
        _isLoading.value = true
        viewModelScope.launch {
            val fetchedEvents = calendarRepository.getEvents(getApplication())
            _events.postValue(fetchedEvents)
            _isLoading.postValue(false)

            // After loading events, trigger travel time calculation for each
            fetchedEvents.forEach { event ->
                calculateTravelTime(event)
            }
        }
    }

    private fun calculateTravelTime(event: CalendarEvent) {
        event.location ?: return // Don't calculate if there is no location

        val workRequest = OneTimeWorkRequestBuilder<RouteCalculationWorker>()
            .setInputData(workDataOf(
                "destination" to event.location,
                "eventTitle" to event.title
            ))
            .build()

        workManager.enqueue(workRequest)

        // Observe the result of the work
        workManager.getWorkInfoByIdLiveData(workRequest.id)
            .observeForever { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        val travelDuration = workInfo.outputData.getString(RouteCalculationWorker.KEY_TRAVEL_DURATION)
                        updateEventWithTravelTime(event.id, travelDuration)
                    }
                }
            }
    }

    private fun updateEventWithTravelTime(eventId: String, travelDuration: String?) {
        val currentEvents = _events.value ?: return
        val updatedEvents = currentEvents.map {
            if (it.id == eventId) {
                it.copy(travelDuration = travelDuration)
            } else {
                it
            }
        }
        _events.postValue(updatedEvents)
    }
    
    fun refreshEvents() {
        loadEvents()
    }
}

