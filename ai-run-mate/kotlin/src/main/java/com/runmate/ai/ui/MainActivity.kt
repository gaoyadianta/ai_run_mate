package com.runmate.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runmate.ai.RunMateApplication
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.ui.running.RunningActivity
import com.runmate.ai.ui.theme.RunMateTheme
import com.runmate.ai.ui.viewmodel.MainViewModel
import com.runmate.ai.ui.viewmodel.MainViewModelFactory

/**
 * 主界面Activity
 */
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要位置权限才能使用跑步功能", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            RunMateTheme {
                MainScreen(
                    onStartRunning = { startRunningActivity() },
                    onOpenCozeDemo = { openCozeDemo() }
                )
            }
        }
    }
    
    /**
     * 检查并请求权限
     */
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    /**
     * 启动跑步Activity
     */
    private fun startRunningActivity() {
        val intent = Intent(this, RunningActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * 打开Coze演示Activity
     */
    private fun openCozeDemo() {
        // TODO: 实现Coze演示功能
        android.widget.Toast.makeText(this, "Coze演示功能开发中...", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartRunning: () -> Unit,
    onOpenCozeDemo: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RunMateApplication
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(application.runningRepository)
    )
    val recentSessions by viewModel.recentSessions.collectAsState()
    val totalDistance by viewModel.totalDistance.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI跑伴") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 统计卡片
            StatsCard(
                totalDistance = totalDistance,
                totalSessions = totalSessions
            )
            
            // 开始跑步按钮
            Button(
                onClick = onStartRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "开始跑步",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Coze演示按钮（开发阶段）
            OutlinedButton(
                onClick = onOpenCozeDemo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Coze语音演示")
            }
            
            // 最近跑步记录
            Text(
                text = "最近跑步",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (recentSessions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "还没有跑步记录\n开始你的第一次跑步吧！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentSessions) { session ->
                        RunningSessionCard(session = session)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    totalDistance: Double,
    totalSessions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "总距离",
                value = String.format("%.1f km", totalDistance / 1000.0)
            )
            StatItem(
                label = "总次数",
                value = "$totalSessions 次"
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RunningSessionCard(session: RunningSession) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.2f km", session.totalDistance / 1000.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDuration(session.totalDuration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "配速: ${String.format("%.1f", session.averagePace)}'/km",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${session.stepCount} 步",
                    style = MaterialTheme.typography.bodyMedium
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