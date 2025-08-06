package com.runmate.ai.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * 时间工具类
 */
object TimeUtils {
    
    /**
     * 格式化时长为可读字符串
     * @param durationMillis 时长（毫秒）
     * @return 格式化后的字符串，如 "1:23:45" 或 "23:45"
     */
    fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 格式化配速为可读字符串
     * @param paceMinutesPerKm 配速（分钟/公里）
     * @return 格式化后的字符串，如 "5'30''/km"
     */
    fun formatPace(paceMinutesPerKm: Double): String {
        if (paceMinutesPerKm <= 0) return "--'--''/km"
        
        val minutes = paceMinutesPerKm.toInt()
        val seconds = ((paceMinutesPerKm - minutes) * 60).toInt()
        
        return String.format("%d'%02d''/km", minutes, seconds)
    }
    
    /**
     * 格式化速度为可读字符串
     * @param speedKmh 速度（公里/小时）
     * @return 格式化后的字符串，如 "12.5 km/h"
     */
    fun formatSpeed(speedKmh: Double): String {
        return String.format("%.1f km/h", speedKmh)
    }
    
    /**
     * 格式化时间戳为日期字符串
     * @param timestamp 时间戳（毫秒）
     * @param pattern 日期格式，默认为 "yyyy-MM-dd HH:mm"
     * @return 格式化后的日期字符串
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm"): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    /**
     * 格式化为相对时间字符串
     * @param timestamp 时间戳（毫秒）
     * @return 相对时间字符串，如 "2小时前"、"昨天"、"3天前"
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days == 1L -> "昨天"
            days < 7 -> "${days}天前"
            else -> formatTimestamp(timestamp, "MM-dd")
        }
    }
    
    /**
     * 计算两个时间戳之间的时长
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 时长（毫秒）
     */
    fun calculateDuration(startTime: Long, endTime: Long): Long {
        return endTime - startTime
    }
    
    /**
     * 获取今天的开始时间戳（00:00:00）
     */
    fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本周的开始时间戳（周一00:00:00）
     */
    fun getWeekStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本月的开始时间戳（1号00:00:00）
     */
    fun getMonthStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

/**
 * Long类型扩展函数：格式化时长
 */
fun Long.toDurationString(): String = TimeUtils.formatDuration(this)

/**
 * Long类型扩展函数：格式化时间戳
 */
fun Long.toDateString(pattern: String = "yyyy-MM-dd HH:mm"): String = 
    TimeUtils.formatTimestamp(this, pattern)

/**
 * Long类型扩展函数：格式化相对时间
 */
fun Long.toRelativeTimeString(): String = TimeUtils.formatRelativeTime(this)