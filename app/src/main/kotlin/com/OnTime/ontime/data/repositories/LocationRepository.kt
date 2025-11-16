package com.OnTime.ontime.data.repositories

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.OnTime.ontime.api.DirectionsService
import com.OnTime.ontime.api.RetrofitClient
import com.OnTime.ontime.data.models.TravelInfo
import com.OnTime.ontime.util.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val directionsService: DirectionsService =
        RetrofitClient.directionsService

    private val _currentLocationUpdates = MutableSharedFlow<Location>(replay = 1)
    val currentLocationUpdates: SharedFlow<Location> = _currentLocationUpdates

    // Method for LocationService to push updates to
    fun pushLocationUpdate(location: Location) {
        _currentLocationUpdates.tryEmit(location)
    }

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
        originLatLng: Pair<Double, Double>,
        destinationLatLng: Pair<Double, Double>,
        mode: String = Constants.MODE_TRANSIT
    ): TravelInfo? {
        val origin = "${originLatLng.first},${originLatLng.second}"
        val destination = "${destinationLatLng.first},${destinationLatLng.second}"

        return try {
            val encodedDestination = java.net.URLEncoder.encode(destination, "UTF-8")

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
                    println("Google Maps API Error: ${body?.status}")
                    null
                }
            } else {
                println("HTTP Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            null
        }
    }
}
