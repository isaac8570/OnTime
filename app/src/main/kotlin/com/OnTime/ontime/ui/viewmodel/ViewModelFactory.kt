package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.OnTime.ontime.data.repositories.CalendarRepository
import com.OnTime.ontime.data.repositories.LocationRepository
import com.OnTime.ontime.data.repositories.WeatherRepository
import com.OnTime.ontime.service.AndroidLocationService

/**
 * ViewModel에 필요한 의존성(Repository 등)을 주입하기 위한 범용 팩토리 클래스입니다.
 */
class ViewModelFactory(
    private val application: Application,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val androidLocationService: AndroidLocationService // MainViewModel을 위해 추가
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // 생성하려는 ViewModel 클래스에 따라 분기하여 적절한 인스턴스를 반환합니다.
        return when {
            // CalendarViewModel을 생성해야 하는 경우
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                CalendarViewModel(
                    application,
                    calendarRepository,
                    weatherRepository,
                    locationRepository
                ) as T
            }
            // MainViewModel을 생성해야 하는 경우
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                MainViewModel(
                    application,
                    weatherRepository,
                    androidLocationService,
                    locationRepository
                ) as T
            }
            // 지원하지 않는 ViewModel 클래스인 경우 예외 발생
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
