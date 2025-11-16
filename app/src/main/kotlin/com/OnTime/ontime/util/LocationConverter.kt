package com.OnTime.ontime.util

import android.content.Context
import android.location.Geocoder
import java.io.IOException
import kotlin.math.*
import java.util.Locale

object LocationConverter {

    private const val RE = 6371.00877 // 지구 반경 (km)
    private const val GRID = 5.0 // 격자 간격 (km)
    private const val SLAT1 = 30.0 // 표준 위도 1
    private const val SLAT2 = 60.0 // 표준 위도 2
    private const val OLON = 126.0 // 기준점 경도
    private const val OLAT = 38.0 // 기준점 위도
    private const val XO = 43 // 기준점 X 좌표
    private const val YO = 136 // 기준점 Y 좌표

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

    @Suppress("DEPRECATION")
    fun latLngToAddress(context: Context, lat: Double, lng: Double): String {
        val geocoder = Geocoder(context, Locale.KOREA)
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // "서울특별시 중구" 와 같이 간략한 주소만 반환
                listOfNotNull(address.adminArea, address.locality, address.subLocality)
                    .joinToString(" ")
                    .ifEmpty { address.getAddressLine(0) ?: "주소 정보 없음" }
            } else {
                "주소 정보 없음"
            }
        } catch (e: IOException) {
            Logger.e("Failed to convert coordinates to address", e)
            "주소 변환 실패"
        }
    }

    fun latLngToKmaGrid(lat: Double, lng: Double): Pair<Int, Int>? {
        val degrad = PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * degrad
        val slat2 = SLAT2 * degrad
        val olon = OLON * degrad
        val olat = OLAT * degrad

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        var ra = tan(PI * 0.25 + lat * degrad * 0.5)
        ra = re * sf / ra.pow(sn)
        var theta = lng * degrad - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        val nx = floor(ra * sin(theta) + XO + 0.5).toInt()
        val ny = floor(ro - ra * cos(theta) + YO + 0.5).toInt()

        return Pair(nx, ny)
    }
}
