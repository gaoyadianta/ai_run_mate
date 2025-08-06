package com.runmate.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.data.repository.RunningRepository
import com.runmate.ai.service.RunningTrackingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 跑步数据ViewModel - 专门处理实时数据显示
 */
class RunningDataViewModel(
    private val runningRepository: RunningRepository
) : ViewModel() {
    
    // 跑步会话数据
    private val _currentSession = MutableStateFlow<RunningSession?>(null)
    val currentSession: StateFlow<RunningSession?> = _currentSession.asStateFlow()
    
    // GPS状态
    private val _gpsStatus = MutableStateFlow(GpsStatus())
    val gpsStatus: StateFlow<GpsStatus> = _gpsStatus.asStateFlow()
    
    // 传感器状态
    private val _sensorStatus = MutableStateFlow(SensorStatus())
    val sensorStatus: StateFlow<SensorStatus> = _sensorStatus.asStateFlow()
    
    // 实时数据
    private val _realTimeData = MutableStateFlow(RealTimeData())
    val realTimeData: StateFlow<RealTimeData> = _realTimeData.asStateFlow()
    
    // 服务连接状态
    private var trackingService: RunningTrackingService? = null
    
    init {
        observeRunningData()
    }
    
    /**
     * 观察跑步数据变化
     */
    private fun observeRunningData() {
        viewModelScope.launch {
            // 监听活跃的跑步会话
            runningRepository.getAllSessions()
                .map { sessions -> 
                    sessions.find { it.status == com.runmate.ai.data.model.RunningStatus.RUNNING || 
                                   it.status == com.runmate.ai.data.model.RunningStatus.PAUSED }
                }
                .collect { activeSession ->
                    _currentSession.value = activeSession
                    updateRealTimeData(activeSession)
                }
        }
    }
    
    /**
     * 连接跑步追踪服务
     */
    fun connectToTrackingService(service: RunningTrackingService) {
        trackingService = service
        
        // 监听服务的数据流
        viewModelScope.launch {
            service.runningData.collect { session ->
                _currentSession.value = session
                updateRealTimeData(session)
            }
        }
        
        // TODO: 监听播报触发事件（用于显示GPS和传感器状态）
        // 暂时注释掉，等待服务端实现
        /*
        viewModelScope.launch {
            service.broadcastTrigger.collect { event ->
                updateDataSourceStatus(event.payload)
            }
        }
        */
    }
    
    /**
     * 更新实时数据
     */
    private fun updateRealTimeData(session: RunningSession?) {
        if (session == null) {
            _realTimeData.value = RealTimeData()
            return
        }
        
        _realTimeData.value = RealTimeData(
            distance = session.totalDistance,
            duration = session.totalDuration,
            currentPace = session.currentPace,
            averagePace = session.averagePace,
            stepCount = session.stepCount,
            calories = session.calories,
            speed = if (session.currentPace > 0) 60.0 / session.currentPace else 0.0
        )
    }
    
    /**
     * 更新数据源状态
     */
    private fun updateDataSourceStatus(payload: com.runmate.ai.data.model.RunningDataPayload) {
        // 更新GPS状态
        _gpsStatus.value = _gpsStatus.value.copy(
            isActive = true,
            signalStrength = if (payload.currentDistance > 0) "强" else "弱",
            lastUpdateTime = System.currentTimeMillis()
        )
        
        // 更新传感器状态
        _sensorStatus.value = _sensorStatus.value.copy(
            isActive = true,
            stepFrequency = payload.stepFrequency,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 手动更新GPS状态
     */
    fun updateGpsStatus(isActive: Boolean, signalStrength: String) {
        _gpsStatus.value = _gpsStatus.value.copy(
            isActive = isActive,
            signalStrength = signalStrength,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 手动更新传感器状态
     */
    fun updateSensorStatus(isActive: Boolean, stepFrequency: Int) {
        _sensorStatus.value = _sensorStatus.value.copy(
            isActive = isActive,
            stepFrequency = stepFrequency,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 检查数据源是否超时
     */
    fun checkDataSourceTimeout() {
        val currentTime = System.currentTimeMillis()
        val timeout = 30000L // 30秒超时
        
        // 检查GPS超时
        if (currentTime - _gpsStatus.value.lastUpdateTime > timeout) {
            _gpsStatus.value = _gpsStatus.value.copy(
                isActive = false,
                signalStrength = "无信号"
            )
        }
        
        // 检查传感器超时
        if (currentTime - _sensorStatus.value.lastUpdateTime > timeout) {
            _sensorStatus.value = _sensorStatus.value.copy(
                isActive = false,
                stepFrequency = 0
            )
        }
    }
}

/**
 * GPS状态数据类
 */
data class GpsStatus(
    val isActive: Boolean = false,
    val signalStrength: String = "未知",
    val lastUpdateTime: Long = 0L,
    val accuracy: Float = 0f,
    val pointsCount: Int = 0
)

/**
 * 传感器状态数据类
 */
data class SensorStatus(
    val isActive: Boolean = false,
    val stepFrequency: Int = 0,
    val lastUpdateTime: Long = 0L,
    val accelerometerAvailable: Boolean = true,
    val stepCounterAvailable: Boolean = true
)

/**
 * 实时数据类
 */
data class RealTimeData(
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val currentPace: Double = 0.0,
    val averagePace: Double = 0.0,
    val stepCount: Int = 0,
    val calories: Int = 0,
    val speed: Double = 0.0
)

/**
 * RunningDataViewModel工厂类
 */
class RunningDataViewModelFactory(
    private val runningRepository: RunningRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunningDataViewModel::class.java)) {
            return RunningDataViewModel(runningRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}