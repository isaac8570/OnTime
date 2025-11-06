package com.OnTime.ontime.api

import com.OnTime.ontime.data.models.WeatherData
import retrofit2.http.GET
import retrofit2.http.Query

interface KmaWeatherApiService {
    @GET("getVilageFcst")
    suspend fun getShortTermForecast(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON",
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): KmaWeatherResponse
}
