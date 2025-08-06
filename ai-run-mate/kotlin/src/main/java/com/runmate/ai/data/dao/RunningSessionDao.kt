package com.runmate.ai.data.dao

import androidx.room.*
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.data.model.RunningStatus
import kotlinx.coroutines.flow.Flow

/**
 * 跑步会话数据访问对象
 */
@Dao
interface RunningSessionDao {
    
    @Query("SELECT * FROM running_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RunningSession>>
    
    @Query("SELECT * FROM running_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): RunningSession?
    
    @Query("SELECT * FROM running_sessions WHERE status = :status LIMIT 1")
    suspend fun getSessionByStatus(status: RunningStatus): RunningSession?
    
    @Query("SELECT * FROM running_sessions WHERE status = 'RUNNING' OR status = 'PAUSED' LIMIT 1")
    suspend fun getActiveSession(): RunningSession?
    
    @Query("SELECT * FROM running_sessions WHERE status = 'COMPLETED' ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentCompletedSessions(limit: Int = 10): List<RunningSession>
    
    @Query("SELECT COUNT(*) FROM running_sessions WHERE status = 'COMPLETED'")
    suspend fun getCompletedSessionsCount(): Int
    
    @Query("SELECT SUM(totalDistance) FROM running_sessions WHERE status = 'COMPLETED'")
    suspend fun getTotalDistance(): Double?
    
    @Query("SELECT SUM(totalDuration) FROM running_sessions WHERE status = 'COMPLETED'")
    suspend fun getTotalDuration(): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RunningSession)
    
    @Update
    suspend fun updateSession(session: RunningSession)
    
    @Delete
    suspend fun deleteSession(session: RunningSession)
    
    @Query("DELETE FROM running_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
    
    @Query("UPDATE running_sessions SET status = :status WHERE sessionId = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: RunningStatus)
}