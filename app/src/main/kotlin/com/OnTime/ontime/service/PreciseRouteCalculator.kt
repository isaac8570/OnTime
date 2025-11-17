package com.OnTime.ontime.service

import android.content.Context
import android.location.Location
import com.OnTime.ontime.api.DirectionsService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class PreciseRouteCalculator(private val context: Context) {
    
    private val directionsService = RetrofitClient.directionsService
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    data class RouteResult(
        val durationMinutes: Int,
        val distanceKm: Double,
        val departureTime: Long,
        val routePolyline: String? = null
    )

    suspend fun calculatePreciseRoute(
        destinationAddress: String,
        eventStartTime: Long,
        transportMode: String = "transit" // transit, driving, walking
    ): RouteResult? {
        return try {
            val currentLocation = getCurrentPreciseLocation()
            val destination = destinationAddress
            
            val response = directionsService.getDirections(
                origin = "${currentLocation.latitude},${currentLocation.longitude}",
                destination = destination,
                mode = transportMode,
                apiKey = Constants.GOOGLE_MAPS_API_KEY
            )

            if (response.isSuccessful && response.body()?.routes?.isNotEmpty() == true) {
                val route = response.body()!!.routes[0]
                val leg = route.legs[0]
                
                val durationSeconds = leg.duration.value
                val durationMinutes = durationSeconds / 60
                val distanceMeters = leg.distance.value
                val distanceKm = distanceMeters / 1000.0
                
                // 여유시간 15분 추가
                val bufferMinutes = 15
                val totalTravelTime = durationMinutes + bufferMinutes
                val departureTime = eventStartTime - (totalTravelTime * 60 * 1000)
                
                RouteResult(
                    durationMinutes = durationMinutes,
                    distanceKm = distanceKm,
                    departureTime = departureTime
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun getCurrentPreciseLocation(): Location {
        return try {
            // This call requires location permission, which is handled at the activity level.
            // Suppressing the lint check here as the permission is checked before this function is called.
            locationClient.lastLocation.await() ?: Location("default").apply {
                latitude = 37.5665
                longitude = 126.9780
            }
        } catch (e: Exception) {
            Location("default").apply {
                latitude = 37.5665
                longitude = 126.9780
            }
        }
    }

    suspend fun getMultipleRouteOptions(
        destinationAddress: String,
        eventStartTime: Long
    ): List<RouteResult> {
        val modes = listOf("transit", "driving", "walking")
        val results = mutableListOf<RouteResult>()
        
        for (mode in modes) {
            calculatePreciseRoute(destinationAddress, eventStartTime, mode)?.let {
                results.add(it)
            }
        }
        
        return results.sortedBy { it.durationMinutes }
    }
}
