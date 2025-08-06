package com.runmate.ai.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * 传感器服务 - 用于步频检测和步数统计
 */
class SensorService(private val context: Context) {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    // 步频检测相关变量
    private var lastStepTime = 0L
    private var stepCount = 0
    private val stepTimes = mutableListOf<Long>()
    private val maxStepHistory = 20 // 保留最近20步的时间记录
    
    // 加速度检测相关变量
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var lastUpdate = 0L
    
    /**
     * 获取步频数据流（步/分钟）
     */
    fun getStepFrequencyUpdates(): Flow<Int> = callbackFlow {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            val currentTime = System.currentTimeMillis()
                            
                            if (currentTime - lastUpdate > 100) { // 每100ms检测一次
                                val x = it.values[0]
                                val y = it.values[1]
                                val z = it.values[2]
                                
                                lastAcceleration = currentAcceleration
                                currentAcceleration = sqrt(x * x + y * y + z * z)
                                
                                val delta = currentAcceleration - lastAcceleration
                                
                                // 检测步伐（简单的峰值检测）
                                if (delta > 2.0f && currentTime - lastStepTime > 300) { // 最小间隔300ms
                                    detectStep(currentTime)
                                    val stepFrequency = calculateStepFrequency()
                                    trySend(stepFrequency)
                                }
                                
                                lastUpdate = currentTime
                            }
                        }
                        else -> {
                            // 处理其他传感器类型或忽略
                        }
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        // 注册加速度传感器监听器
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        
        awaitClose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
    
    /**
     * 获取步数计数器数据流
     */
    fun getStepCountUpdates(): Flow<Int> = callbackFlow {
        var initialStepCount = -1
        
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_STEP_COUNTER -> {
                            val currentSteps = it.values[0].toInt()
                            
                            if (initialStepCount == -1) {
                                initialStepCount = currentSteps
                            }
                            
                            val sessionSteps = currentSteps - initialStepCount
                            trySend(sessionSteps)
                        }
                        else -> {
                            // 处理其他传感器类型或忽略
                        }
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        // 注册步数计数器监听器
        stepCounter?.let { sensor ->
            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        awaitClose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
    
    /**
     * 检测到步伐
     */
    private fun detectStep(timestamp: Long) {
        stepCount++
        lastStepTime = timestamp
        
        // 记录步伐时间
        stepTimes.add(timestamp)
        
        // 保持最近的步伐记录
        if (stepTimes.size > maxStepHistory) {
            stepTimes.removeAt(0)
        }
    }
    
    /**
     * 计算步频（步/分钟）
     */
    private fun calculateStepFrequency(): Int {
        if (stepTimes.size < 2) return 0
        
        val recentSteps = minOf(stepTimes.size, 10) // 使用最近10步计算
        val timeSpan = stepTimes.last() - stepTimes[stepTimes.size - recentSteps]
        
        if (timeSpan <= 0) return 0
        
        // 计算每分钟步数
        val stepsPerMinute = (recentSteps - 1) * 60000.0 / timeSpan
        return stepsPerMinute.toInt()
    }
    
    /**
     * 重置步数统计
     */
    fun resetStepCount() {
        stepCount = 0
        stepTimes.clear()
        lastStepTime = 0L
    }
    
    /**
     * 检查传感器可用性
     */
    fun isAccelerometerAvailable(): Boolean = accelerometer != null
    
    fun isStepCounterAvailable(): Boolean = stepCounter != null
    
    companion object {
        /**
         * 根据步频和步长估算距离
         */
        fun estimateDistanceFromSteps(steps: Int, strideLength: Float = 0.7f): Double {
            return steps * strideLength.toDouble()
        }
        
        /**
         * 根据身高估算步长（米）
         */
        fun estimateStrideLength(heightCm: Float): Float {
            return heightCm * 0.43f / 100f // 经验公式：身高 × 0.43
        }
    }
}