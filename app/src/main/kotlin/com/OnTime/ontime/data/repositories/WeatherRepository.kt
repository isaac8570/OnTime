package com.OnTime.ontime.data.repositories

import com.OnTime.ontime.BuildConfig
import com.OnTime.ontime.api.KmaWeatherApiService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.api.WeatherItem
import com.OnTime.ontime.util.DateUtil
import com.OnTime.ontime.util.Logger

class WeatherRepository(
    private val kmaWeatherApiService: KmaWeatherApiService = RetrofitClient.kmaWeatherService
) {

    suspend fun getShortTermForecast(
        nx: Int, // 예보지점 X 좌표
        ny: Int  // 예보지점 Y 좌표
    ): List<WeatherItem> {
        val (baseDate, baseTime) = DateUtil.getKmaApiBaseTime()
        Logger.d("WeatherRepository: Fetching weather for nx=$nx, ny=$ny, baseDate=$baseDate, baseTime=$baseTime")

        return try {
            val response = kmaWeatherApiService.getShortTermForecast(
                serviceKey = BuildConfig.KMA_WEATHER_API_KEY,
                baseDate = baseDate,
                baseTime = baseTime,
                nx = nx,
                ny = ny
            )
            if (response.response.header.resultCode == "00") {
                response.response.body.items.item
            } else {
                Logger.e("WeatherRepository: KMA API Error: ${response.response.header.resultMsg}")
                emptyList()
            }
        } catch (e: Exception) {
            Logger.e("WeatherRepository: Error fetching KMA weather: ${e.message}", e)
            emptyList()
        }
    }
}
