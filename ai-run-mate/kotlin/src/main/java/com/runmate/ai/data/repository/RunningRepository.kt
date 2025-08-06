package com.runmate.ai.data.repository

import com.runmate.ai.data.dao.GpsPointDao
import com.runmate.ai.data.dao.RunningSessionDao
import com.runmate.ai.data.model.GpsPoint
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.data.model.RunningStatus
import kotlinx.coroutines.flow.Flow

/**
 * 跑步数据仓库
 */
class RunningRepository(
    private val sessionDao: RunningSessionDao,
    private val gpsPointDao: GpsPointDao
) {
    
    // 跑步会话相关操作
    fun getAllSessions(): Flow<List<RunningSession>> = sessionDao.getAllSessions()
    
    suspend fun getSessionById(sessionId: String): RunningSession? = sessionDao.getSessionById(sessionId)
    
    suspend fun getActiveSession(): RunningSession? = sessionDao.getActiveSession()
    
    suspend fun getRecentCompletedSessions(limit: Int = 10): List<RunningSession> = 
        sessionDao.getRecentCompletedSessions(limit)
    
    suspend fun insertSession(session: RunningSession) = sessionDao.insertSession(session)
    
    suspend fun updateSession(session: RunningSession) = sessionDao.updateSession(session)
    
    suspend fun updateSessionStatus(sessionId: String, status: RunningStatus) = 
        sessionDao.updateSessionStatus(sessionId, status)
    
    suspend fun deleteSession(session: RunningSession) = sessionDao.deleteSession(session)
    
    // GPS点相关操作
    fun getGpsPointsBySessionId(sessionId: String): Flow<List<GpsPoint>> = 
        gpsPointDao.getPointsBySessionId(sessionId)
    
    suspend fun getGpsPointsBySessionIdSync(sessionId: String): List<GpsPoint> = 
        gpsPointDao.getPointsBySessionIdSync(sessionId)
    
    suspend fun getLatestGpsPoint(sessionId: String): GpsPoint? = 
        gpsPointDao.getLatestPointBySessionId(sessionId)
    
    suspend fun insertGpsPoint(point: GpsPoint) = gpsPointDao.insertPoint(point)
    
    suspend fun insertGpsPoints(points: List<GpsPoint>) = gpsPointDao.insertPoints(points)
    
    suspend fun deleteGpsPointsBySessionId(sessionId: String) = 
        gpsPointDao.deletePointsBySessionId(sessionId)
    
    // 统计数据
    suspend fun getCompletedSessionsCount(): Int = sessionDao.getCompletedSessionsCount()
    
    suspend fun getTotalDistance(): Double = sessionDao.getTotalDistance() ?: 0.0
    
    suspend fun getTotalDuration(): Long = sessionDao.getTotalDuration() ?: 0L
}