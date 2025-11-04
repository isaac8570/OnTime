package com.OnTime.ontime.util

import android.content.Context
import android.location.Geocoder
import java.io.IOException

object LocationConverter {
    
    fun addressToLatLng(context: Context, address: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses?.isNotEmpty() == true) {
                val location = addresses[0]
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: IOException) {
            Logger.e("Failed to convert address to coordinates", e)
            null
        }
    }
}
