package com.runmate.ai.data.model

/**
 * 跑步数据载荷
 */
data class RunningDataPayload(
    val sessionId: String = "",
    val currentDistance: Double = 0.0,
    val currentPace: Double = 0.0,
    val averagePace: Double = 0.0,
    val duration: Long = 0L,
    val stepFrequency: Int = 0,
    val gpsAccuracy: Float = 0f,
    val targetDistance: Double? = null,
    val targetDuration: Long? = null,
    val userMessage: String? = null,
    val triggerType: BroadcastTriggerType? = null,
    val timestamp: Long = System.currentTimeMillis()
)