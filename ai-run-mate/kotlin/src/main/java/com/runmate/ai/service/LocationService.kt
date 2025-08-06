package com.runmate.ai.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.runmate.ai.data.model.GpsPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.*

/**
 * GPS定位服务
 */
class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        2000L // 2秒更新一次
    ).apply {
        setMinUpdateDistanceMeters(5f) // 最小5米距离更新
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        setWaitForAccurateLocation(false)
    }.build()
    
    /**
     * 获取位置更新流
     */
    fun getLocationUpdates(sessionId: String): Flow<GpsPoint> = callbackFlow {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.forEach { location ->
                    val gpsPoint = GpsPoint(
                        sessionId = sessionId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        accuracy = location.accuracy,
                        speed = location.speed,
                        timestamp = location.time,
                        bearing = location.bearing
                    )
                    trySend(gpsPoint)
                }
            }
        }
        
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    /**
     * 获取最后已知位置
     */
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            fusedLocationClient.lastLocation.result
        } catch (e: SecurityException) {
            null
        }
    }
    
    /**
     * 检查是否有位置权限
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    companion object {
        /**
         * 计算两个GPS点之间的距离（米）
         */
        fun calculateDistance(point1: GpsPoint, point2: GpsPoint): Double {
            return calculateDistance(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude
            )
        }
        
        /**
         * 计算两个坐标之间的距离（米）
         */
        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371000.0 // 地球半径（米）
            
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            
            val a = sin(dLat / 2).pow(2) + 
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                    sin(dLon / 2).pow(2)
            
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            
            return earthRadius * c
        }
        
        /**
         * 计算配速（分钟/公里）
         */
        fun calculatePace(distanceMeters: Double, durationMillis: Long): Double {
            if (distanceMeters <= 0 || durationMillis <= 0) return 0.0
            
            val distanceKm = distanceMeters / 1000.0
            val durationMinutes = durationMillis / 60000.0
            
            return durationMinutes / distanceKm
        }
        
        /**
         * 计算速度（公里/小时）
         */
        fun calculateSpeed(distanceMeters: Double, durationMillis: Long): Double {
            if (distanceMeters <= 0 || durationMillis <= 0) return 0.0
            
            val distanceKm = distanceMeters / 1000.0
            val durationHours = durationMillis / 3600000.0
            
            return distanceKm / durationHours
        }
    }
}