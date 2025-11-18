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
import com.OnTime.ontime.util.Logger
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
            Logger.d("Location permission not granted.")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Logger.d("Successfully got last known location: $location")
                    } else {
                        Logger.d("Last known location is null.")
                    }
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->
                    Logger.e("Failed to get last known location", e)
                    continuation.resume(null)
                }
        }
    }

    suspend fun calculateTravelTime(
        originLatLng: Pair<Double, Double>,
        destinationLatLng: Pair<Double, Double>,
        mode: String = Constants.MODE_TRANSIT,
        departureTime: Long? = null
    ): TravelInfo? {
        val origin = "${originLatLng.first},${originLatLng.second}"
        val destination = "${destinationLatLng.first},${destinationLatLng.second}"
        val departureTimeInSeconds = if (mode == Constants.MODE_TRANSIT) departureTime?.div(1000) else null

        Logger.d("Calculating travel time. Origin: $origin, Destination: $destination, Mode: $mode, DepartureTime(sec): $departureTimeInSeconds")

        return try {
            val response = directionsService.getDirections(
                origin = origin,
                destination = destination,
                mode = mode,
                apiKey = Constants.GOOGLE_MAPS_API_KEY,
                departureTime = departureTimeInSeconds
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "OK") {
                    val route = body.routes.firstOrNull()
                    val leg = route?.legs?.firstOrNull()

                    leg?.let {
                        // Build detailed route description
                        val routeDetails = it.steps?.joinToString(separator = " -> ") { step ->
                            val instruction = stripHtmlTags(step.html_instructions ?: "")
                            if (step.travel_mode == "TRANSIT" && step.transit_details?.line != null) {
                                val lineName = step.transit_details.line.short_name ?: step.transit_details.line.name
                                "[${lineName}] $instruction"
                            } else {
                                instruction
                            }
                        }
                        
                        val travelInfo = TravelInfo(
                            durationMinutes = it.duration.value / 60,
                            durationText = it.duration.text,
                            distanceText = it.distance.text,
                            routeDetails = routeDetails,
                            mode = mode
                        )
                        Logger.d("Successfully calculated travel time: ${travelInfo.durationText}")
                        Logger.d("Route Details: $routeDetails")
                        return@let travelInfo
                    }
                } else {
                    Logger.e("Google Maps API Error: ${body?.status}")
                    null
                }
            } else {
                Logger.e("HTTP Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Logger.e("Exception in calculateTravelTime", e)
            null
        }
    }

    private fun stripHtmlTags(html: String): String {
        return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
    }
}
