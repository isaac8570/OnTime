package com.OnTime.ontime.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AndroidLocationService(private val context: Context) : ILocationService {

    override fun startLocationUpdatesService() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START_FOREGROUND_SERVICE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopLocationUpdatesService() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP_FOREGROUND_SERVICE
        }
        // 이미 실행 중인 서비스를 중지할 때는 startService를 사용할 수 있습니다.
        context.startService(intent)
    }
}
