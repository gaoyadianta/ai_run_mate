package com.runmate.ai.ui.running

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runmate.ai.RunMateApplication
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.data.model.RunningStatus
import com.runmate.ai.service.RunningTrackingService
import com.runmate.ai.ui.theme.RunMateTheme
import com.runmate.ai.ui.viewmodel.RunningViewModel
import com.runmate.ai.ui.viewmodel.RunningViewModelFactory

/**
 * 跑步Activity
 */
class RunningActivity : ComponentActivity() {
    
    private var trackingService: RunningTrackingService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RunningTrackingService.RunningTrackingBinder
            trackingService = binder.getService()
            isServiceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            RunMateTheme {
                RunningScreen(
                    onStartRunning = { startRunning() },
                    onPauseRunning = { pauseRunning() },
                    onResumeRunning = { resumeRunning() },
                    onStopRunning = { stopRunning() },
                    onFinish = { finish() }
                )
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        bindTrackingService()
    }
    
    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
    
    /**
     * 绑定跑步追踪服务
     */
    private fun bindTrackingService() {
        val intent = Intent(this, RunningTrackingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * 开始跑步
     */
    private fun startRunning() {
        val intent = Intent(this, RunningTrackingService::class.java).apply {
            action = RunningTrackingService.ACTION_START_TRACKING
        }
        startForegroundService(intent)
    }
    
    /**
     * 暂停跑步
     */
    private fun pauseRunning() {
        trackingService?.pauseTracking()
    }
    
    /**
     * 恢复跑步
     */
    private fun resumeRunning() {
        trackingService?.resumeTracking()
    }
    
    /**
     * 停止跑步
     */
    private fun stopRunning() {
        trackingService?.stopTracking()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningScreen(
    onStartRunning: () -> Unit,
    onPauseRunning: () -> Unit,
    onResumeRunning: () -> Unit,
    onStopRunning: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RunMateApplication
    
    // 主要的跑步ViewModel
    val runningViewModel: RunningViewModel = viewModel(
        factory = RunningViewModelFactory(application.runningRepository)
    )
    
    // 专门处理实时数据显示的ViewModel
    val dataViewModel: com.runmate.ai.ui.viewmodel.RunningDataViewModel = viewModel(
        factory = com.runmate.ai.ui.viewmodel.RunningDataViewModelFactory(application.runningRepository)
    )
    
    val currentSession by runningViewModel.currentSession.collectAsState()
    val gpsStatus by dataViewModel.gpsStatus.collectAsState()
    val sensorStatus by dataViewModel.sensorStatus.collectAsState()
    val realTimeData by dataViewModel.realTimeData.collectAsState()
    
    val isRunning = currentSession?.status == RunningStatus.RUNNING
    val isPaused = currentSession?.status == RunningStatus.PAUSED
    val hasStarted = currentSession != null && currentSession?.status != RunningStatus.NOT_STARTED
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跑步中") },
                navigationIcon = {
                    if (!hasStarted) {
                        IconButton(onClick = onFinish) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 跑步数据显示 - 连接真实的GPS和传感器数据
            RunningDataDisplay(
                session = currentSession,
                stepFrequency = sensorStatus.stepFrequency,
                gpsSignalStrength = gpsStatus.signalStrength,
                isGpsActive = gpsStatus.isActive,
                isSensorActive = sensorStatus.isActive
            )
            
            // 调试信息视图 (开发阶段显示，用于验证数据获取)
            com.runmate.ai.ui.debug.DataSourceDebugView(
                session = currentSession,
                gpsStatus = gpsStatus,
                sensorStatus = sensorStatus,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // 控制按钮
            RunningControls(
                isRunning = isRunning,
                isPaused = isPaused,
                hasStarted = hasStarted,
                onStart = onStartRunning,
                onPause = onPauseRunning,
                onResume = onResumeRunning,
                onStop = onStopRunning
            )
        }
    }
}

@Composable
fun RunningDataDisplay(session: RunningSession?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 距离
        DataItem(
            label = "距离",
            value = String.format("%.2f", (session?.totalDistance ?: 0.0) / 1000.0),
            unit = "km",
            isLarge = true
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 时间
            DataItem(
                label = "时间",
                value = formatDuration(session?.totalDuration ?: 0L),
                unit = ""
            )
            
            // 配速
            DataItem(
                label = "配速",
                value = String.format("%.1f", session?.currentPace ?: 0.0),
                unit = "'/km"
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 步数
            DataItem(
                label = "步数",
                value = "${session?.stepCount ?: 0}",
                unit = "步"
            )
            
            // 卡路里
            DataItem(
                label = "卡路里",
                value = "${session?.calories ?: 0}",
                unit = "kcal"
            )
        }
    }
}

@Composable
fun DataItem(
    label: String,
    value: String,
    unit: String,
    isLarge: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = if (isLarge) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RunningControls(
    isRunning: Boolean,
    isPaused: Boolean,
    hasStarted: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hasStarted) {
            // 开始按钮
            FloatingActionButton(
                onClick = onStart,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "开始",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        } else {
            // 暂停/恢复按钮
            FloatingActionButton(
                onClick = if (isRunning) onPause else onResume,
                modifier = Modifier.size(64.dp),
                containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (isRunning) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "暂停" else "继续",
                    tint = Color.White
                )
            }
            
            // 停止按钮
            FloatingActionButton(
                onClick = onStop,
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "停止",
                    tint = Color.White
                )
            }
        }
    }
}

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