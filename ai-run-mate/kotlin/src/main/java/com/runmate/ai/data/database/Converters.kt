package com.runmate.ai.data.database

import androidx.room.TypeConverter
import com.runmate.ai.data.model.RunningStatus

/**
 * Room数据库类型转换器
 */
class Converters {
    
    @TypeConverter
    fun fromRunningStatus(status: RunningStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toRunningStatus(status: String): RunningStatus {
        return RunningStatus.valueOf(status)
    }
}