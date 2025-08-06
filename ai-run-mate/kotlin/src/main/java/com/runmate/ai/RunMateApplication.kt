package com.runmate.ai

import android.app.Application
import com.runmate.ai.data.database.RunMateDatabase
import com.runmate.ai.data.repository.RunningRepository
import com.runmate.ai.data.repository.UserPreferencesRepository
import com.coze.kotlin_example.config.Config

/**
 * RunMate应用程序类
 */
class RunMateApplication : Application() {
    
    // 数据库实例
    val database by lazy { RunMateDatabase.getDatabase(this) }
    
    // 仓库实例
    val runningRepository by lazy { 
        RunningRepository(
            database.runningSessionDao(), 
            database.gpsPointDao()
        ) 
    }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化Coze配置
        try {
            Config.init(this)
        } catch (e: Exception) {
            // 如果Coze配置初始化失败，记录错误但不影响应用启动
            e.printStackTrace()
        }
    }
}