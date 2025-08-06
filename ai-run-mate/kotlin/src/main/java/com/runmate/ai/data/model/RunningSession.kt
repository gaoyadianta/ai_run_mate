package com.runmate.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 跑步会话数据模型
 */
@Entity(tableName = "running_sessions")
data class RunningSession(
    @PrimaryKey
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistance: Double = 0.0, // 总距离(米)
    val totalDuration: Long = 0L, // 总时长(毫秒)
    val averagePace: Double = 0.0, // 平均配速(分钟/公里)
    val currentPace: Double = 0.0, // 当前配速(分钟/公里)
    val stepCount: Int = 0, // 步数
    val calories: Int = 0, // 卡路里
    val maxSpeed: Float = 0f, // 最大速度(m/s)
    val averageSpeed: Float = 0f, // 平均速度(m/s)
    val elevationGain: Double = 0.0, // 累计爬升(米)
    val status: RunningStatus = RunningStatus.NOT_STARTED,
    val targetDistance: Double? = null, // 目标距离(米)
    val targetDuration: Long? = null, // 目标时长(毫秒)
    val weatherCondition: String? = null, // 天气状况
    val notes: String? = null // 备注
)

/**
 * 跑步状态枚举
 */
enum class RunningStatus {
    NOT_STARTED,    // 未开始
    RUNNING,        // 跑步中
    PAUSED,         // 暂停
    COMPLETED,      // 已完成
    CANCELLED       // 已取消
}