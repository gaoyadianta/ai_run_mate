package com.runmate.ai.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.runmate.ai.R
import com.runmate.ai.data.model.*
import com.runmate.ai.data.repository.RunningRepository
import com.runmate.ai.data.repository.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * 跑步追踪前台服务
 */
class RunningTrackingService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "running_tracking_channel"
        private const val CHANNEL_NAME = "跑步追踪"
        
        // Service Actions
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_PAUSE_TRACKING = "ACTION_PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "ACTION_RESUME_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
    }
    
    private val binder = RunningTrackingBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 服务依赖
    private lateinit var runningRepository: RunningRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var locationService: LocationService
    private lateinit var sensorService: SensorService
    
    // 当前跑步会话
    private var currentSession: RunningSession? = null
    private var isTracking = false
    private var isPaused = false
    
    // 数据收集
    private val gpsPoints = mutableListOf<GpsPoint>()
    private var totalDistance = 0.0
    private var stepCount = 0
    private var currentStepFrequency = 0
    
    // 时间追踪
    private var sessionStartTime = 0L
    private var pausedDuration = 0L
    private var lastPauseTime = 0L
    
    // 播报控制
    private var lastTimeBroadcast = 0L
    private var lastDistanceBroadcast = 0.0
    
    // 数据流
    private val _runningData = MutableStateFlow<RunningSession?>(null)
    val runningData: StateFlow<RunningSession?> = _runningData.asStateFlow()
    
    private val _broadcastTrigger = MutableSharedFlow<RunningDataPayload>()
    val broadcastTrigger: SharedFlow<RunningDataPayload> = _broadcastTrigger.asSharedFlow()
    
    inner class RunningTrackingBinder : Binder() {
        fun getService(): RunningTrackingService = this@RunningTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeServices()
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_PAUSE_TRACKING -> pauseTracking()
            ACTION_RESUME_TRACKING -> resumeTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    /**
     * 初始化服务依赖
     */
    private fun initializeServices() {
        val app = application as com.runmate.ai.RunMateApplication
        runningRepository = app.runningRepository
        userPreferencesRepository = app.userPreferencesRepository
        locationService = LocationService(this)
        sensorService = SensorService(this)
    }
    
    /**
     * 开始跑步追踪
     */
    fun startTracking(targetDistance: Double? = null, targetDuration: Long? = null) {
        if (isTracking) return
        
        val sessionId = UUID.randomUUID().toString()
        sessionStartTime = System.currentTimeMillis()
        
        currentSession = RunningSession(
            sessionId = sessionId,
            startTime = sessionStartTime,
            status = RunningStatus.RUNNING,
            targetDistance = targetDistance,
            targetDuration = targetDuration
        )
        
        isTracking = true
        isPaused = false
        
        startForeground(NOTIFICATION_ID, createNotification())
        startDataCollection(sessionId)
        
        // 触发开始播报
        triggerBroadcast(BroadcastTriggerType.SESSION_START)
    }
    
    /**
     * 暂停跑步追踪
     */
    fun pauseTracking() {
        if (!isTracking || isPaused) return
        
        isPaused = true
        lastPauseTime = System.currentTimeMillis()
        
        currentSession = currentSession?.copy(status = RunningStatus.PAUSED)
        updateNotification()
    }
    
    /**
     * 恢复跑步追踪
     */
    fun resumeTracking() {
        if (!isTracking || !isPaused) return
        
        isPaused = false
        pausedDuration += System.currentTimeMillis() - lastPauseTime
        
        currentSession = currentSession?.copy(status = RunningStatus.RUNNING)
        updateNotification()
    }
    
    /**
     * 停止跑步追踪
     */
    fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        isPaused = false
        
        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - sessionStartTime - pausedDuration
        
        currentSession = currentSession?.copy(
            endTime = endTime,
            totalDuration = totalDuration,
            totalDistance = totalDistance,
            stepCount = stepCount,
            status = RunningStatus.COMPLETED,
            averagePace = LocationService.calculatePace(totalDistance, totalDuration),
            calories = estimateCalories(totalDistance, totalDuration)
        )
        
        // 保存会话数据
        serviceScope.launch {
            currentSession?.let { session ->
                runningRepository.insertSession(session)
                runningRepository.insertGpsPoints(gpsPoints)
            }
        }
        
        // 触发结束播报
        triggerBroadcast(BroadcastTriggerType.SESSION_END)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * 开始数据收集
     */
    private fun startDataCollection(sessionId: String) {
        // GPS数据收集
        serviceScope.launch {
            locationService.getLocationUpdates(sessionId)
                .catch { e ->
                    // GPS错误处理
                    android.util.Log.e("RunningService", "GPS error: ${e.message}")
                }
                .collect { gpsPoint ->
                    if (isTracking && !isPaused) {
                        processGpsPoint(gpsPoint)
                        // 发送GPS状态更新
                        emitGpsStatus(true, gpsPoint.accuracy)
                    }
                }
        }
        
        // 传感器数据收集
        serviceScope.launch {
            sensorService.getStepFrequencyUpdates()
                .catch { e ->
                    // 传感器错误处理
                    android.util.Log.e("RunningService", "Sensor error: ${e.message}")
                    emitSensorStatus(false, 0)
                }
                .collect { stepFreq ->
                    if (isTracking && !isPaused) {
                        currentStepFrequency = stepFreq
                        updateRunningData()
                        // 发送传感器状态更新
                        emitSensorStatus(true, stepFreq)
                    }
                }
        }
        
        serviceScope.launch {
            sensorService.getStepCountUpdates()
                .collect { steps ->
                    if (isTracking && !isPaused) {
                        stepCount = steps
                        updateRunningData()
                    }
                }
        }
        
        // 定时播报检查
        serviceScope.launch {
            while (isTracking) {
                delay(10000) // 每10秒检查一次
                if (!isPaused) {
                    checkBroadcastTriggers()
                }
            }
        }
    }
    
    /**
     * 发送GPS状态更新
     */
    private fun emitGpsStatus(isActive: Boolean, accuracy: Float) {
        val signalStrength = when {
            !isActive -> "无信号"
            accuracy < 5f -> "强"
            accuracy < 10f -> "中"
            else -> "弱"
        }
        
        // 这里可以通过广播或其他方式通知UI
        android.util.Log.d("RunningService", "GPS状态: $isActive, 精度: ${accuracy}m, 信号: $signalStrength")
    }
    
    /**
     * 发送传感器状态更新
     */
    private fun emitSensorStatus(isActive: Boolean, stepFrequency: Int) {
        android.util.Log.d("RunningService", "传感器状态: $isActive, 步频: $stepFrequency")
    }
    
    /**
     * 处理GPS点数据
     */
    private fun processGpsPoint(gpsPoint: GpsPoint) {
        gpsPoints.add(gpsPoint)
        
        // 计算距离
        if (gpsPoints.size > 1) {
            val lastPoint = gpsPoints[gpsPoints.size - 2]
            val distance = LocationService.calculateDistance(lastPoint, gpsPoint)
            totalDistance += distance
        }
        
        updateRunningData()
        updateNotification()
    }
    
    /**
     * 更新跑步数据
     */
    private fun updateRunningData() {
        val currentTime = System.currentTimeMillis()
        val runningDuration = if (isPaused) {
            lastPauseTime - sessionStartTime - pausedDuration
        } else {
            currentTime - sessionStartTime - pausedDuration
        }
        
        val currentPace = if (gpsPoints.size >= 2) {
            val recentPoints = gpsPoints.takeLast(10)
            val recentDistance = calculateDistance(recentPoints)
            val recentDuration = recentPoints.last().timestamp - recentPoints.first().timestamp
            LocationService.calculatePace(recentDistance, recentDuration)
        } else 0.0
        
        currentSession = currentSession?.copy(
            totalDistance = totalDistance,
            totalDuration = runningDuration,
            currentPace = currentPace,
            averagePace = LocationService.calculatePace(totalDistance, runningDuration),
            stepCount = stepCount
        )
        
        _runningData.value = currentSession
    }
    
    /**
     * 检查播报触发条件
     */
    private fun checkBroadcastTriggers() {
        val currentTime = System.currentTimeMillis()
        
        // 时间间隔播报（每2分钟）
        if (currentTime - lastTimeBroadcast >= 120000) { // 2分钟
            lastTimeBroadcast = currentTime
            triggerBroadcast(BroadcastTriggerType.TIME_INTERVAL)
        }
        
        // 距离间隔播报（每1公里）
        val currentDistanceKm = totalDistance / 1000.0
        if (currentDistanceKm - lastDistanceBroadcast >= 1.0) {
            lastDistanceBroadcast = kotlin.math.floor(currentDistanceKm)
            triggerBroadcast(BroadcastTriggerType.DISTANCE_INTERVAL)
        }
    }
    
    /**
     * 触发AI播报
     */
    private fun triggerBroadcast(triggerType: BroadcastTriggerType, userMessage: String? = null) {
        val session = currentSession ?: return
        
        val payload = RunningDataPayload(
            sessionId = session.sessionId,
            currentDistance = totalDistance / 1000.0,
            currentPace = session.currentPace,
            averagePace = session.averagePace,
            duration = session.totalDuration / 60000L,
            stepFrequency = currentStepFrequency,
            targetDistance = session.targetDistance?.let { it / 1000.0 },
            targetDuration = session.targetDuration?.let { it / 60000L },
            userMessage = userMessage,
            triggerType = triggerType
        )
        
        serviceScope.launch {
            _broadcastTrigger.emit(payload)
        }
    }
    
    /**
     * 计算路径距离
     */
    private fun calculateDistance(points: List<GpsPoint>): Double {
        if (points.size < 2) return 0.0
        
        var distance = 0.0
        for (i in 1 until points.size) {
            distance += LocationService.calculateDistance(points[i-1], points[i])
        }
        return distance
    }
    
    /**
     * 估算卡路里消耗
     */
    private fun estimateCalories(distanceMeters: Double, durationMillis: Long): Int {
        // 简化的卡路里计算：假设70kg体重，跑步消耗约0.75卡路里/kg/km
        val distanceKm = distanceMeters / 1000.0
        val weightKg = 70.0 // 默认体重
        val caloriesPerKgKm = 0.75
        
        return (distanceKm * weightKg * caloriesPerKgKm).toInt()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "跑步追踪服务通知"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val session = currentSession
        val distance = String.format("%.2f", totalDistance / 1000.0)
        val duration = formatDuration(session?.totalDuration ?: 0L)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI跑伴 - 跑步中")
            .setContentText("距离: ${distance}km | 时间: $duration")
            .setSmallIcon(R.drawable.ic_running)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 格式化时长显示
     */
    private fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}