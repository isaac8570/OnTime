package com.OnTime.ontime.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    
    private const val GOOGLE_MAPS_BASE_URL = "https://maps.googleapis.com/"
    private const val KMA_WEATHER_BASE_URL = "https://apis.data.go.kr/"
    
    private fun getRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val directionsService: DirectionsService by lazy {
        getRetrofit(GOOGLE_MAPS_BASE_URL).create(DirectionsService::class.java)
    }
    
    val placesService: PlacesApiService by lazy {
        getRetrofit(GOOGLE_MAPS_BASE_URL).create(PlacesApiService::class.java)
    }

    val kmaWeatherService: KmaWeatherApiService by lazy {
        getRetrofit(KMA_WEATHER_BASE_URL).create(KmaWeatherApiService::class.java)
    }
}
