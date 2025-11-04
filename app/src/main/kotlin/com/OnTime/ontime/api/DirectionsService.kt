package com.OnTime.ontime.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsService {
    
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String,
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>
}

data class DirectionsResponse(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val legs: List<Leg>
)

data class Leg(
    val duration: Duration,
    val distance: Distance
)

data class Duration(
    val value: Int,
    val text: String
)

data class Distance(
    val value: Int,
    val text: String
)
