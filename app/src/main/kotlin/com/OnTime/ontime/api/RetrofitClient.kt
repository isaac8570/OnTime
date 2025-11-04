package com.OnTime.ontime.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    
    private const val GOOGLE_MAPS_BASE_URL = "https://maps.googleapis.com/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    
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
    
    val geminiService: GeminiApiService by lazy {
        getRetrofit(GEMINI_BASE_URL).create(GeminiApiService::class.java)
    }
}
