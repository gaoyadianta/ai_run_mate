package com.runmate.ai.data.dao

import androidx.room.*
import com.runmate.ai.data.model.GpsPoint
import kotlinx.coroutines.flow.Flow

/**
 * GPS坐标点数据访问对象
 */
@Dao
interface GpsPointDao {
    
    @Query("SELECT * FROM gps_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsBySessionId(sessionId: String): Flow<List<GpsPoint>>
    
    @Query("SELECT * FROM gps_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsBySessionIdSync(sessionId: String): List<GpsPoint>
    
    @Query("SELECT * FROM gps_points WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPointBySessionId(sessionId: String): GpsPoint?
    
    @Query("SELECT COUNT(*) FROM gps_points WHERE sessionId = :sessionId")
    suspend fun getPointsCountBySessionId(sessionId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: GpsPoint)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<GpsPoint>)
    
    @Delete
    suspend fun deletePoint(point: GpsPoint)
    
    @Query("DELETE FROM gps_points WHERE sessionId = :sessionId")
    suspend fun deletePointsBySessionId(sessionId: String)
    
    @Query("DELETE FROM gps_points WHERE timestamp < :timestamp")
    suspend fun deleteOldPoints(timestamp: Long)
}