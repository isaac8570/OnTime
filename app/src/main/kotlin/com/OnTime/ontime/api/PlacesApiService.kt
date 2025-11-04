package com.OnTime.ontime.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    
    @GET("maps/api/place/autocomplete/json")
    suspend fun getPlaceAutocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String
    ): Response<PlaceAutocompleteResponse>
}

data class PlaceAutocompleteResponse(
    val predictions: List<Prediction>,
    val status: String
)

data class Prediction(
    val description: String,
    val place_id: String
)
