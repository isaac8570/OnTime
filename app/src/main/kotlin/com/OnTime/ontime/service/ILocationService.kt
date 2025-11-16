package com.OnTime.ontime.service

import android.location.Location
import com.OnTime.ontime.data.models.TravelInfo

interface ILocationService {
    fun startLocationUpdatesService()
    fun stopLocationUpdatesService()
}

