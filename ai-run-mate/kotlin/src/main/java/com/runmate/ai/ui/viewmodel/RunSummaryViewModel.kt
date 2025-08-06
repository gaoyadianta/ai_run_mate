package com.runmate.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runmate.ai.data.model.GpsPoint
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.data.repository.RunningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 跑步总结ViewModel
 */
class RunSummaryViewModel(
    private val runningRepository: RunningRepository,
    private val sessionId: String
) : ViewModel() {
    
    private val _session = MutableStateFlow<RunningSession?>(null)
    val session: StateFlow<RunningSession?> = _session.asStateFlow()
    
    private val _gpsPoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val gpsPoints: StateFlow<List<GpsPoint>> = _gpsPoints.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadSessionData()
    }
    
    /**
     * 加载跑步会话数据
     */
    private fun loadSessionData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 加载跑步会话
                val sessionData = runningRepository.getSessionById(sessionId)
                _session.value = sessionData
                
                if (sessionData != null) {
                    // 加载GPS轨迹点
                    val points = runningRepository.getGpsPointsBySessionIdSync(sessionId)
                    _gpsPoints.value = points
                } else {
                    _error.value = "未找到跑步记录"
                }
                
            } catch (e: Exception) {
                _error.value = "加载数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        loadSessionData()
    }
    
    /**
     * 获取跑步统计信息
     */
    fun getRunningStats(): RunningStats? {
        val currentSession = _session.value ?: return null
        val points = _gpsPoints.value
        
        return RunningStats(
            totalDistance = currentSession.totalDistance,
            totalDuration = currentSession.totalDuration,
            averagePace = currentSession.averagePace,
            maxSpeed = currentSession.maxSpeed.toDouble(),
            calories = currentSession.calories,
            stepCount = currentSession.stepCount,
            elevationGain = currentSession.elevationGain,
            gpsPointsCount = points.size,
            startTime = currentSession.startTime,
            endTime = currentSession.endTime ?: System.currentTimeMillis()
        )
    }
    
    /**
     * 获取配速分析数据
     */
    fun getPaceAnalysis(): List<PaceDataPoint> {
        val points = _gpsPoints.value
        if (points.size < 2) return emptyList()
        
        val paceData = mutableListOf<PaceDataPoint>()
        var cumulativeDistance = 0.0
        
        for (i in 1 until points.size) {
            val prevPoint = points[i - 1]
            val currPoint = points[i]
            
            // 计算这一段的距离
            val segmentDistance = com.runmate.ai.service.LocationService.calculateDistance(
                prevPoint.latitude, prevPoint.longitude,
                currPoint.latitude, currPoint.longitude
            )
            
            cumulativeDistance += segmentDistance
            
            // 计算这一段的时间
            val segmentTime = currPoint.timestamp - prevPoint.timestamp
            
            // 计算配速
            val pace = if (segmentDistance > 0 && segmentTime > 0) {
                com.runmate.ai.service.LocationService.calculatePace(segmentDistance, segmentTime)
            } else 0.0
            
            paceData.add(
                PaceDataPoint(
                    distanceKm = cumulativeDistance / 1000.0,
                    paceMinPerKm = pace,
                    timestamp = currPoint.timestamp
                )
            )
        }
        
        return paceData
    }
    
    /**
     * 获取路线边界
     */
    fun getRouteBounds(): RouteBounds? {
        val points = _gpsPoints.value
        if (points.isEmpty()) return null
        
        val bounds = com.runmate.ai.utils.DistanceUtils.calculateBounds(points) ?: return null
        val center = com.runmate.ai.utils.DistanceUtils.calculateCenter(points) ?: return null
        
        return RouteBounds(
            minLat = bounds[0],
            minLon = bounds[1],
            maxLat = bounds[2],
            maxLon = bounds[3],
            centerLat = center[0],
            centerLon = center[1]
        )
    }
}

/**
 * 跑步统计信息
 */
data class RunningStats(
    val totalDistance: Double,
    val totalDuration: Long,
    val averagePace: Double,
    val maxSpeed: Double,
    val calories: Int,
    val stepCount: Int,
    val elevationGain: Double,
    val gpsPointsCount: Int,
    val startTime: Long,
    val endTime: Long
)

/**
 * 配速数据点
 */
data class PaceDataPoint(
    val distanceKm: Double,
    val paceMinPerKm: Double,
    val timestamp: Long
)

/**
 * 路线边界
 */
data class RouteBounds(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double
)

/**
 * RunSummaryViewModel工厂类
 */
class RunSummaryViewModelFactory(
    private val runningRepository: RunningRepository,
    private val sessionId: String
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunSummaryViewModel::class.java)) {
            return RunSummaryViewModel(runningRepository, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}