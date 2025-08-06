package com.runmate.ai.data.model

/**
 * AI播报触发类型枚举
 */
enum class BroadcastTriggerType {
    // 时间相关触发器
    TIME_INTERVAL,          // 时间间隔触发（如每2分钟）
    
    // 距离相关触发器
    DISTANCE_MILESTONE,     // 距离里程碑触发（如每1公里）
    DISTANCE_INTERVAL,      // 距离间隔触发
    
    // 配速相关触发器
    PACE_CHANGE,           // 配速变化触发
    
    // 目标相关触发器
    GOAL_ACHIEVEMENT,      // 目标达成触发
    TARGET_ACHIEVED,       // 目标达成（别名）
    
    // 异常检测触发器
    ANOMALY_DETECTION,     // 异常检测触发
    ANOMALY_DETECTED,      // 异常检测（别名）
    
    // 用户交互触发器
    MANUAL_TRIGGER,        // 手动触发
    USER_INPUT,           // 用户输入触发
    
    // 会话状态触发器
    START_RUNNING,        // 开始跑步
    SESSION_START,        // 会话开始（别名）
    PAUSE_RUNNING,        // 暂停跑步
    RESUME_RUNNING,       // 恢复跑步
    END_RUNNING,          // 结束跑步
    SESSION_END,          // 会话结束（别名）
    
    // 特殊事件触发器
    GPS_SIGNAL_LOST,      // GPS信号丢失
    GPS_SIGNAL_RECOVERED, // GPS信号恢复
    WEATHER_ALERT,        // 天气提醒
    SAFETY_ALERT          // 安全提醒
}

/**
 * 播报优先级
 */
enum class BroadcastPriority {
    LOW,     // 低优先级（如定时播报）
    MEDIUM,  // 中优先级（如配速变化）
    HIGH,    // 高优先级（如异常检测、目标达成）
    URGENT   // 紧急优先级（如安全警告）
}

/**
 * 播报触发事件数据类
 */
data class BroadcastTriggerEvent(
    val triggerType: BroadcastTriggerType,
    val priority: BroadcastPriority,
    val runningData: RunningDataPayload,
    val userMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)