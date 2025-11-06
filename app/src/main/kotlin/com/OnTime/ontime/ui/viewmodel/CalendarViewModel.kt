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

class CalendarViewModel(application: Application, private val calendarRepository: CalendarRepository, private val weatherRepository: WeatherRepository, private val locationConverter: LocationConverter) : AndroidViewModel(application) {
    
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentLocationLatLng = MutableLiveData<Pair<Double, Double>?>()
    val currentLocationLatLng: LiveData<Pair<Double, Double>?> = _currentLocationLatLng

    private val workManager = WorkManager.getInstance(application)
    
    fun updateCurrentLocation(latitude: Double, longitude: Double) {
        _currentLocationLatLng.value = Pair(latitude, longitude)
        // Re-calculate travel times and geocode destinations when current location changes
        _events.value?.forEach { event ->
            geocodeAndCalculateTravelTime(event)
        }
    }

    private fun calculateTravelTime(event: CalendarEvent, origin: Pair<Double, Double>, destination: Pair<Double, Double>) {
        val workRequest = OneTimeWorkRequestBuilder<RouteCalculationWorker>()
            .setInputData(workDataOf(
                "originLat" to origin.first,
                "originLng" to origin.second,
                "destinationLat" to destination.first,
                "destinationLng" to destination.second,
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

    private fun updateEventInList(updatedEvent: CalendarEvent) {
        val currentEvents = _events.value.orEmpty().toMutableList()
        val index = currentEvents.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            currentEvents[index] = updatedEvent
            _events.postValue(currentEvents)
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
    
    fun loadEvents() {
        _isLoading.value = true
        viewModelScope.launch {
            val fetchedEvents = calendarRepository.getEvents(getApplication())
            val eventsWithGeocodedDestinations = fetchedEvents.map { event ->
                event.location?.let { location ->
                    locationConverter.addressToLatLng(getApplication(), location)?.let { latLng ->
                        event.copy(destinationLatLng = latLng)
                    }
                } ?: event
            }
            _events.postValue(eventsWithGeocodedDestinations)
            _isLoading.postValue(false)

            // After loading events and geocoding destinations, trigger travel time calculation for each
            eventsWithGeocodedDestinations.forEach { event ->
                geocodeAndCalculateTravelTime(event)
            }
        }
    }

    private fun geocodeAndCalculateTravelTime(event: CalendarEvent) {
        viewModelScope.launch {
            val origin = _currentLocationLatLng.value
            val destination = event.destinationLatLng

            if (origin != null && destination != null) {
                calculateTravelTime(event, origin, destination)
            } else if (event.location != null) {
                // Try to geocode destination if not already done
                val geocodedDestination = locationConverter.addressToLatLng(getApplication(), event.location)
                if (geocodedDestination != null && origin != null) {
                    val updatedEvent = event.copy(destinationLatLng = geocodedDestination)
                    updateEventInList(updatedEvent)
                    calculateTravelTime(updatedEvent, origin, geocodedDestination)
                }
            }
        }
    }

    fun refreshEvents() {
        loadEvents()
    }
}

