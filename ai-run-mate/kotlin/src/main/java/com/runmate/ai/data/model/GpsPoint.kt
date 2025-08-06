package com.runmate.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * GPS坐标点数据模型
 */
@Entity(tableName = "gps_points")
data class GpsPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long,
    val bearing: Float = 0f
)