package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.WeatherRepository

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            // These repositories could be singletons if needed, but creating new instances here for simplicity
            val calendarRepository = CalendarRepository()
            val weatherRepository = WeatherRepository()
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(application, calendarRepository, weatherRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
