package com.runmate.ai.service

import com.runmate.ai.data.model.BroadcastTriggerType
import com.runmate.ai.data.model.BroadcastPriority
import com.runmate.ai.data.model.RunningDataPayload
import com.runmate.ai.data.model.UserPreferences
import com.runmate.ai.data.model.BroadcastInterval
import com.runmate.ai.data.model.RunningSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AI播报触发器
 * 负责检测各种播报触发条件并生成播报事件
 */
class AIBroadcastTrigger {
    
    // 播报事件流
    private val _broadcastEvents = MutableSharedFlow<BroadcastEvent>()
    val broadcastEvents: SharedFlow<BroadcastEvent> = _broadcastEvents.asSharedFlow()
    
    // 触发器状态
    private var lastTimeBroadcast = 0L
    private var lastDistanceBroadcast = 0.0
    private var lastPace = 0.0
    private var targetPace = 0.0
    
    // 配置参数
    private var timeBroadcastInterval = 120000L // 2分钟
    private var distanceBroadcastInterval = 1000.0 // 1公里(米)
    private var paceAlertThreshold = 30.0 // 配速偏差阈值(秒)
    
    /**
     * 检查所有触发条件
     */
    suspend fun checkTriggers(
        session: RunningSession,
        userPreferences: UserPreferences
    ) {
        updateConfiguration(userPreferences)
        
        val currentTime = System.currentTimeMillis()
        
        // 检查时间间隔触发
        if (checkTimeTrigger(currentTime)) {
            emitBroadcastEvent(
                BroadcastTriggerType.TIME_INTERVAL,
                session,
                "定时播报"
            )
        }
        
        // 检查距离间隔触发
        if (checkDistanceTrigger(session.totalDistance)) {
            emitBroadcastEvent(
                BroadcastTriggerType.DISTANCE_INTERVAL,
                session,
                "距离播报"
            )
        }
        
        // 检查配速变化触发
        if (checkPaceTrigger(session.currentPace)) {
            emitBroadcastEvent(
                BroadcastTriggerType.PACE_CHANGE,
                session,
                "配速变化提醒"
            )
        }
        
        // 检查目标达成触发
        if (checkTargetAchievedTrigger(session)) {
            emitBroadcastEvent(
                BroadcastTriggerType.TARGET_ACHIEVED,
                session,
                "目标达成"
            )
        }
        
        // 检查异常情况触发
        if (checkAnomalyTrigger(session)) {
            emitBroadcastEvent(
                BroadcastTriggerType.ANOMALY_DETECTED,
                session,
                "异常检测"
            )
        }
    }
    
    /**
     * 检查时间间隔触发
     */
    private fun checkTimeTrigger(currentTime: Long): Boolean {
        return if (currentTime - lastTimeBroadcast >= timeBroadcastInterval) {
            lastTimeBroadcast = currentTime
            true
        } else {
            false
        }
    }
    
    /**
     * 检查距离间隔触发
     */
    private fun checkDistanceTrigger(currentDistance: Double): Boolean {
        val currentDistanceKm = currentDistance / 1000.0
        val lastDistanceKm = lastDistanceBroadcast / 1000.0
        
        return if (currentDistanceKm - lastDistanceKm >= distanceBroadcastInterval / 1000.0) {
            lastDistanceBroadcast = kotlin.math.floor(currentDistanceKm) * 1000.0
            true
        } else {
            false
        }
    }
    
    /**
     * 检查配速变化触发
     */
    private fun checkPaceTrigger(currentPace: Double): Boolean {
        if (lastPace == 0.0) {
            lastPace = currentPace
            return false
        }
        
        val paceChange = kotlin.math.abs(currentPace - lastPace)
        val isSignificantChange = paceChange > paceAlertThreshold
        
        if (isSignificantChange) {
            lastPace = currentPace
            return true
        }
        
        return false
    }
    
    /**
     * 检查目标达成触发
     */
    private fun checkTargetAchievedTrigger(session: RunningSession): Boolean {
        // 检查距离目标
        session.targetDistance?.let { target ->
            if (session.totalDistance >= target && session.totalDistance < target + 100) {
                return@checkTargetAchievedTrigger true
            }
        }
        
        // 检查时间目标
        session.targetDuration?.let { target ->
            if (session.totalDuration >= target && session.totalDuration < target + 10000) {
                return@checkTargetAchievedTrigger true
            }
        }
        
        return false
    }
    
    /**
     * 检查异常情况触发
     */
    private fun checkAnomalyTrigger(session: RunningSession): Boolean {
        // 检查配速异常（过快或过慢）
        if (session.currentPace > 0) {
            // 配速过慢（超过10分钟/公里）
            if (session.currentPace > 10.0) {
                return true
            }
            
            // 配速过快（少于3分钟/公里，可能是数据异常）
            if (session.currentPace < 3.0) {
                return true
            }
        }
        
        // 检查长时间无GPS信号（可以通过最后GPS点时间判断）
        // 这里简化处理，实际可以根据GPS点时间戳判断
        
        return false
    }
    
    /**
     * 手动触发播报（用户主动请求）
     */
    suspend fun triggerManualBroadcast(
        session: RunningSession,
        userMessage: String? = null
    ) {
        emitBroadcastEvent(
            BroadcastTriggerType.USER_INPUT,
            session,
            userMessage ?: "用户主动请求"
        )
    }
    
    /**
     * 触发会话开始播报
     */
    suspend fun triggerSessionStart(session: RunningSession) {
        emitBroadcastEvent(
            BroadcastTriggerType.SESSION_START,
            session,
            "开始跑步"
        )
    }
    
    /**
     * 触发会话结束播报
     */
    suspend fun triggerSessionEnd(session: RunningSession) {
        emitBroadcastEvent(
            BroadcastTriggerType.SESSION_END,
            session,
            "结束跑步"
        )
    }
    
    /**
     * 发送播报事件
     */
    private suspend fun emitBroadcastEvent(
        triggerType: BroadcastTriggerType,
        session: RunningSession,
        message: String
    ) {
        val payload = RunningDataPayload(
            sessionId = session.sessionId,
            currentDistance = session.totalDistance / 1000.0,
            currentPace = session.currentPace,
            averagePace = session.averagePace,
            duration = session.totalDuration / 60000L,
            stepFrequency = 0, // 需要从传感器服务获取
            targetDistance = session.targetDistance?.let { it / 1000.0 },
            targetDuration = session.targetDuration?.let { it / 60000L },
            userMessage = message,
            triggerType = triggerType
        )
        
        val event = BroadcastEvent(
            triggerType = triggerType,
            payload = payload,
            priority = getBroadcastPriority(triggerType),
            timestamp = System.currentTimeMillis()
        )
        
        _broadcastEvents.emit(event)
    }
    
    /**
     * 获取播报优先级
     */
    private fun getBroadcastPriority(triggerType: BroadcastTriggerType): BroadcastPriority {
        return when (triggerType) {
            BroadcastTriggerType.ANOMALY_DETECTION, BroadcastTriggerType.ANOMALY_DETECTED -> BroadcastPriority.HIGH
            BroadcastTriggerType.GOAL_ACHIEVEMENT, BroadcastTriggerType.TARGET_ACHIEVED -> BroadcastPriority.HIGH
            BroadcastTriggerType.MANUAL_TRIGGER, BroadcastTriggerType.USER_INPUT -> BroadcastPriority.HIGH
            BroadcastTriggerType.START_RUNNING, BroadcastTriggerType.SESSION_START -> BroadcastPriority.MEDIUM
            BroadcastTriggerType.END_RUNNING, BroadcastTriggerType.SESSION_END -> BroadcastPriority.MEDIUM
            BroadcastTriggerType.PAUSE_RUNNING, BroadcastTriggerType.RESUME_RUNNING -> BroadcastPriority.MEDIUM
            BroadcastTriggerType.PACE_CHANGE -> BroadcastPriority.MEDIUM
            BroadcastTriggerType.TIME_INTERVAL -> BroadcastPriority.LOW
            BroadcastTriggerType.DISTANCE_MILESTONE, BroadcastTriggerType.DISTANCE_INTERVAL -> BroadcastPriority.LOW
            BroadcastTriggerType.GPS_SIGNAL_LOST, BroadcastTriggerType.GPS_SIGNAL_RECOVERED -> BroadcastPriority.URGENT
            BroadcastTriggerType.WEATHER_ALERT, BroadcastTriggerType.SAFETY_ALERT -> BroadcastPriority.URGENT
        }
    }
    
    /**
     * 更新配置参数
     */
    private fun updateConfiguration(userPreferences: UserPreferences) {
        timeBroadcastInterval = when (userPreferences.broadcastInterval) {
            BroadcastInterval.EVERY_1_MINUTE -> 60000L
            BroadcastInterval.EVERY_2_MINUTES -> 120000L
            BroadcastInterval.EVERY_5_MINUTES -> 300000L
            BroadcastInterval.EVERY_10_MINUTES -> 600000L
            BroadcastInterval.DISTANCE_BASED -> Long.MAX_VALUE
        }
        
        distanceBroadcastInterval = userPreferences.distanceBroadcastInterval * 1000.0
        paceAlertThreshold = userPreferences.paceAlertThreshold
    }
    
    /**
     * 重置触发器状态
     */
    fun reset() {
        lastTimeBroadcast = 0L
        lastDistanceBroadcast = 0.0
        lastPace = 0.0
        targetPace = 0.0
    }
}

/**
 * 播报事件
 */
data class BroadcastEvent(
    val triggerType: BroadcastTriggerType,
    val payload: RunningDataPayload,
    val priority: BroadcastPriority,
    val timestamp: Long
)

/**
 * 播报优先级
 */
enum class BroadcastPriority {
    LOW,    // 低优先级：定时播报
    MEDIUM, // 中优先级：状态变化
    HIGH    // 高优先级：异常、目标达成、用户请求
}