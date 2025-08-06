package com.runmate.ai.utils

import kotlin.math.*

/**
 * 距离和位置工具类
 */
object DistanceUtils {
    
    private const val EARTH_RADIUS_METERS = 6371000.0 // 地球半径（米）
    
    /**
     * 格式化距离为可读字符串
     * @param distanceMeters 距离（米）
     * @param unit 单位系统
     * @return 格式化后的字符串，如 "5.23 km" 或 "1.2 mi"
     */
    fun formatDistance(distanceMeters: Double, unit: com.runmate.ai.data.model.UnitSystem = com.runmate.ai.data.model.UnitSystem.METRIC): String {
        return when (unit) {
            com.runmate.ai.data.model.UnitSystem.METRIC -> {
                when {
                    distanceMeters < 1000 -> String.format("%.0f m", distanceMeters)
                    distanceMeters < 10000 -> String.format("%.2f km", distanceMeters / 1000.0)
                    else -> String.format("%.1f km", distanceMeters / 1000.0)
                }
            }
            com.runmate.ai.data.model.UnitSystem.IMPERIAL -> {
                val miles = distanceMeters * 0.000621371
                when {
                    miles < 0.1 -> String.format("%.0f ft", distanceMeters * 3.28084)
                    miles < 10 -> String.format("%.2f mi", miles)
                    else -> String.format("%.1f mi", miles)
                }
            }
        }
    }
    
    /**
     * 使用Haversine公式计算两点间距离
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（米）
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_METERS * c
    }
    
    /**
     * 计算配速（分钟/公里）
     * @param distanceMeters 距离（米）
     * @param durationMillis 时长（毫秒）
     * @return 配速（分钟/公里）
     */
    fun calculatePace(distanceMeters: Double, durationMillis: Long): Double {
        if (distanceMeters <= 0 || durationMillis <= 0) return 0.0
        
        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationMillis / 60000.0
        
        return durationMinutes / distanceKm
    }
    
    /**
     * 计算速度（公里/小时）
     * @param distanceMeters 距离（米）
     * @param durationMillis 时长（毫秒）
     * @return 速度（公里/小时）
     */
    fun calculateSpeed(distanceMeters: Double, durationMillis: Long): Double {
        if (distanceMeters <= 0 || durationMillis <= 0) return 0.0
        
        val distanceKm = distanceMeters / 1000.0
        val durationHours = durationMillis / 3600000.0
        
        return distanceKm / durationHours
    }
    
    /**
     * 根据配速计算速度
     * @param paceMinutesPerKm 配速（分钟/公里）
     * @return 速度（公里/小时）
     */
    fun paceToSpeed(paceMinutesPerKm: Double): Double {
        return if (paceMinutesPerKm > 0) 60.0 / paceMinutesPerKm else 0.0
    }
    
    /**
     * 根据速度计算配速
     * @param speedKmh 速度（公里/小时）
     * @return 配速（分钟/公里）
     */
    fun speedToPace(speedKmh: Double): Double {
        return if (speedKmh > 0) 60.0 / speedKmh else 0.0
    }
    
    /**
     * 计算卡路里消耗（简化算法）
     * @param distanceMeters 距离（米）
     * @param durationMillis 时长（毫秒）
     * @param weightKg 体重（公斤），默认70kg
     * @return 卡路里消耗
     */
    fun calculateCalories(
        distanceMeters: Double, 
        durationMillis: Long, 
        weightKg: Double = 70.0
    ): Int {
        val distanceKm = distanceMeters / 1000.0
        val durationHours = durationMillis / 3600000.0
        
        // 简化的卡路里计算公式：MET值 × 体重 × 时间
        val speed = calculateSpeed(distanceMeters, durationMillis)
        val metValue = when {
            speed < 6.0 -> 6.0  // 慢跑
            speed < 8.0 -> 8.3  // 中速跑
            speed < 10.0 -> 10.5 // 快跑
            speed < 12.0 -> 12.3 // 很快跑
            else -> 14.5        // 冲刺
        }
        
        return (metValue * weightKg * durationHours).toInt()
    }
    
    /**
     * 计算路径的总距离
     * @param points GPS坐标点列表
     * @return 总距离（米）
     */
    fun calculateRouteDistance(points: List<com.runmate.ai.data.model.GpsPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            totalDistance += calculateDistance(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude
            )
        }
        
        return totalDistance
    }
    
    /**
     * 计算路径的边界框
     * @param points GPS坐标点列表
     * @return 边界框 [minLat, minLon, maxLat, maxLon]
     */
    fun calculateBounds(points: List<com.runmate.ai.data.model.GpsPoint>): DoubleArray? {
        if (points.isEmpty()) return null
        
        var minLat = points[0].latitude
        var maxLat = points[0].latitude
        var minLon = points[0].longitude
        var maxLon = points[0].longitude
        
        for (point in points) {
            minLat = minOf(minLat, point.latitude)
            maxLat = maxOf(maxLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLon = maxOf(maxLon, point.longitude)
        }
        
        return doubleArrayOf(minLat, minLon, maxLat, maxLon)
    }
    
    /**
     * 计算路径的中心点
     * @param points GPS坐标点列表
     * @return 中心点坐标 [lat, lon]
     */
    fun calculateCenter(points: List<com.runmate.ai.data.model.GpsPoint>): DoubleArray? {
        if (points.isEmpty()) return null
        
        val bounds = calculateBounds(points) ?: return null
        val centerLat = (bounds[0] + bounds[2]) / 2
        val centerLon = (bounds[1] + bounds[3]) / 2
        
        return doubleArrayOf(centerLat, centerLon)
    }
    
    /**
     * 计算累计爬升
     * @param points GPS坐标点列表
     * @return 累计爬升（米）
     */
    fun calculateElevationGain(points: List<com.runmate.ai.data.model.GpsPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalGain = 0.0
        for (i in 1 until points.size) {
            val elevationDiff = points[i].altitude - points[i - 1].altitude
            if (elevationDiff > 0) {
                totalGain += elevationDiff
            }
        }
        
        return totalGain
    }
}

/**
 * Double类型扩展函数：格式化距离
 */
fun Double.toDistanceString(unit: com.runmate.ai.data.model.UnitSystem = com.runmate.ai.data.model.UnitSystem.METRIC): String = 
    DistanceUtils.formatDistance(this, unit)

/**
 * Double类型扩展函数：格式化配速
 */
fun Double.toPaceString(): String = com.runmate.ai.utils.TimeUtils.formatPace(this)

/**
 * Double类型扩展函数：格式化速度
 */
fun Double.toSpeedString(): String = com.runmate.ai.utils.TimeUtils.formatSpeed(this)