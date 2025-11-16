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
                serviceKey = BuildConfig.WEATHER_API_KEY, // Changed to BuildConfig.WEATHER_API_KEY
                baseDate = baseDate,
                baseTime = baseTime,
                nx = nx,
                ny = ny
            )
            Logger.d("WeatherRepository: KMA API Response: $response")
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

    suspend fun getWeatherCondition(gridCoords: Pair<Int, Int>?): String {
        if (gridCoords == null) {
            return "알 수 없음"
        }
        return try {
            val weatherItems = getShortTermForecast(gridCoords.first, gridCoords.second)
            val ptyItem = weatherItems.find { it.category == "PTY" }?.fcstValue // 강수 형태
            val skyItem = weatherItems.find { it.category == "SKY" }?.fcstValue // 하늘 상태

            when (ptyItem) {
                "1", "2", "4" -> "비" // 비, 비/눈, 소나기
                "3" -> "눈" // 눈
                else -> when (skyItem) {
                    "1" -> "맑음"
                    "3" -> "구름많음"
                    "4" -> "흐림"
                    else -> "알 수 없음"
                }
            }
        } catch (e: Exception) {
            Logger.e("WeatherRepository: Error getting simplified weather condition: ${e.message}", e)
            "알 수 없음"
        }
    }
}
