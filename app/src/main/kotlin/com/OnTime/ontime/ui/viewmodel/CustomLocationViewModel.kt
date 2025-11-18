package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.data.models.CustomLocation
import com.OnTime.ontime.data.repositories.SettingsRepository
import kotlinx.coroutines.launch

class CustomLocationViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _customLocations = MutableLiveData<List<CustomLocation>>()
    val customLocations: LiveData<List<CustomLocation>> = _customLocations

    init {
        loadCustomLocations()
    }

    fun loadCustomLocations() {
        viewModelScope.launch {
            _customLocations.value = settingsRepository.getCustomLocations()
        }
    }

    fun saveCustomLocation(location: CustomLocation) {
        viewModelScope.launch {
            try {
                settingsRepository.saveCustomLocation(location)
                loadCustomLocations() // Refresh the list after saving
            } catch (e: Exception) {
                println("Error saving custom location: ${e.message}")
                // Optionally, handle error UI here
            }
        }
    }
}
