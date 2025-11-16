package com.OnTime.ontime.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.OnTime.ontime.api.DirectionsService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.util.Constants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val directionsService: DirectionsService = 
        RetrofitClient.directionsService
    
    suspend fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
    
    suspend fun calculateTravelTime(
        currentLocation: Location,
        destinationAddress: String,
        mode: String = Constants.MODE_TRANSIT
    ): TravelInfo? {
        val origin = "${currentLocation.latitude},${currentLocation.longitude}"
        
        return try {
            // URL 인코딩 추가
            val encodedDestination = java.net.URLEncoder.encode(destinationAddress, "UTF-8")
            
            val response = directionsService.getDirections(
                origin = origin,
                destination = encodedDestination,
                mode = mode,
                apiKey = Constants.GOOGLE_MAPS_API_KEY
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "OK") {
                    val route = body.routes?.firstOrNull()
                    val leg = route?.legs?.firstOrNull()
                    
                    leg?.let {
                        TravelInfo(
                            durationMinutes = it.duration.value / 60,
                            durationText = it.duration.text,
                            distanceText = it.distance.text
                        )
                    }
                } else {
                    // API 응답 상태 오류 로깅
                    println("Google Maps API Error: ${body?.status}")
                    null
                }
            } else {
                // HTTP 오류 로깅
                println("HTTP Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            null
        }
    }
}

data class TravelInfo(
    val durationMinutes: Int,
    val durationText: String,
    val distanceText: String
)
