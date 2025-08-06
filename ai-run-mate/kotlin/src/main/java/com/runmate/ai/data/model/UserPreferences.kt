package com.runmate.ai.data.model

/** 用户偏好设置数据模型 */
data class UserPreferences(
        val aiVoiceGender: VoiceGender = VoiceGender.FEMALE,
        val broadcastInterval: BroadcastInterval = BroadcastInterval.EVERY_2_MINUTES,
        val distanceBroadcastInterval: Double = 1.0, // 每公里播报
        val enablePaceAlerts: Boolean = true,
        val paceAlertThreshold: Double = 30.0, // 配速偏差阈值（秒）
        val enableEncouragement: Boolean = true,
        val enableProfessionalAdvice: Boolean = true,
        val defaultTargetDistance: Double? = null, // 默认目标距离（米）
        val defaultTargetDuration: Long? = null, // 默认目标时长（毫秒）
        val unitSystem: UnitSystem = UnitSystem.METRIC
)

/** AI语音性别 */
enum class VoiceGender {
    MALE, // 男声
    FEMALE // 女声
}

/** 播报间隔 */
enum class BroadcastInterval {
    EVERY_1_MINUTE, // 每1分钟
    EVERY_2_MINUTES, // 每2分钟
    EVERY_5_MINUTES, // 每5分钟
    EVERY_10_MINUTES, // 每10分钟
    DISTANCE_BASED // 基于距离
}
