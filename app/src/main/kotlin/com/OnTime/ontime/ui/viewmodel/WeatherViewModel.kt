package com.OnTime.ontime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.api.WeatherItem
import com.OnTime.ontime.data.repositories.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(private val weatherRepository: WeatherRepository) : ViewModel() {

    private val _weatherData = MutableStateFlow<Map<Pair<Int, Int>, List<WeatherItem>>>(emptyMap())
    val weatherData: StateFlow<Map<Pair<Int, Int>, List<WeatherItem>>> = _weatherData

    fun fetchWeatherForLocation(nx: Int, ny: Int) {
        viewModelScope.launch {
            val weatherItems = weatherRepository.getShortTermForecast(nx, ny)
            _weatherData.value = _weatherData.value.toMutableMap().apply {
                this[Pair(nx, ny)] = weatherItems
            }
        }
    }
}
