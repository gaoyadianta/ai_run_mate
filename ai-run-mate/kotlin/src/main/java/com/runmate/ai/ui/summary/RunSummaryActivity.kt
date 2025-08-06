package com.runmate.ai.ui.summary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runmate.ai.RunMateApplication
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.ui.theme.RunMateTheme
import com.runmate.ai.ui.viewmodel.RunSummaryViewModel
import com.runmate.ai.ui.viewmodel.RunSummaryViewModelFactory
import com.runmate.ai.utils.DistanceUtils
import com.runmate.ai.utils.TimeUtils

/**
 * 跑步总结Activity
 */
class RunSummaryActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        
        fun createIntent(context: Context, sessionId: String): Intent {
            return Intent(context, RunSummaryActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId == null) {
            finish()
            return
        }
        
        setContent {
            RunMateTheme {
                RunSummaryScreen(
                    sessionId = sessionId,
                    onFinish = { finish() },
                    onShareResult = { shareRunningResult(it) }
                )
            }
        }
    }
    
    private fun shareRunningResult(session: RunningSession) {
        val distance = DistanceUtils.formatDistance(session.totalDistance)
        val duration = TimeUtils.formatDuration(session.totalDuration)
        val pace = TimeUtils.formatPace(session.averagePace)
        
        val shareText = """
            🏃‍♂️ 刚完成了一次跑步！
            
            📏 距离: $distance
            ⏱️ 时间: $duration  
            🏃 平均配速: $pace
            🔥 卡路里: ${session.calories} kcal
            👟 步数: ${session.stepCount} 步
            
            #AI跑伴 #跑步记录
        """.trimIndent()
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "分享跑步成果"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(
    sessionId: String,
    onFinish: () -> Unit,
    onShareResult: (RunningSession) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RunMateApplication
    val viewModel: RunSummaryViewModel = viewModel(
        factory = RunSummaryViewModelFactory(application.runningRepository, sessionId)
    )
    
    val session by viewModel.session.collectAsState()
    val gpsPoints by viewModel.gpsPoints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跑步总结") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    session?.let { runningSession ->
                        IconButton(onClick = { onShareResult(runningSession) }) {
                            Icon(Icons.Default.Share, contentDescription = "分享")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            session?.let { runningSession ->
                RunSummaryContent(
                    session = runningSession,
                    gpsPoints = gpsPoints,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到跑步记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RunSummaryContent(
    session: RunningSession,
    gpsPoints: List<com.runmate.ai.data.model.GpsPoint>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 完成祝贺卡片
        CompletionCard(session)
        
        // 主要数据展示
        MainStatsCard(session)
        
        // 详细数据展示
        DetailedStatsCard(session)
        
        // 路线地图（如果有GPS数据）
        if (gpsPoints.isNotEmpty()) {
            RouteMapCard(gpsPoints)
        }
        
        // AI总结和建议
        AISummaryCard(session)
        
        // 成就和记录
        AchievementCard(session)
    }
}

@Composable
fun CompletionCard(session: RunningSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "跑步完成！",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = TimeUtils.formatTimestamp(session.endTime ?: System.currentTimeMillis()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MainStatsCard(session: RunningSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "运动数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MainStatItem(
                    label = "距离",
                    value = DistanceUtils.formatDistance(session.totalDistance),
                    icon = "📏"
                )
                
                MainStatItem(
                    label = "时间",
                    value = TimeUtils.formatDuration(session.totalDuration),
                    icon = "⏱️"
                )
                
                MainStatItem(
                    label = "配速",
                    value = TimeUtils.formatPace(session.averagePace),
                    icon = "🏃"
                )
            }
        }
    }
}

@Composable
fun MainStatItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DetailedStatsCard(session: RunningSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "详细数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            DetailStatRow("卡路里", "${session.calories} kcal")
            DetailStatRow("步数", "${session.stepCount} 步")
            DetailStatRow("平均速度", TimeUtils.formatSpeed(DistanceUtils.paceToSpeed(session.averagePace)))
            DetailStatRow("最大速度", TimeUtils.formatSpeed(session.maxSpeed.toDouble()))
            
            if (session.elevationGain > 0) {
                DetailStatRow("累计爬升", "${session.elevationGain.toInt()} m")
            }
        }
    }
}

@Composable
fun DetailStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RouteMapCard(gpsPoints: List<com.runmate.ai.data.model.GpsPoint>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "跑步路线",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 这里应该显示地图，暂时用占位符
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "地图",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "路线地图",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${gpsPoints.size} 个GPS点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AISummaryCard(session: RunningSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "AI分析",
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "AI分析与建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // AI生成的总结内容
            Text(
                text = generateAISummary(session),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
            )
        }
    }
}

@Composable
fun AchievementCard(session: RunningSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🏆 成就与记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 这里可以显示各种成就
            val achievements = generateAchievements(session)
            
            if (achievements.isNotEmpty()) {
                achievements.forEach { achievement ->
                    AchievementItem(achievement)
                }
            } else {
                Text(
                    text = "继续努力，更多成就等你解锁！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🎖️",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = achievement,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 生成AI总结内容
private fun generateAISummary(session: RunningSession): String {
    val distance = session.totalDistance / 1000.0
    val pace = session.averagePace
    
    return when {
        distance >= 10 -> "太棒了！完成了${String.format("%.1f", distance)}公里的长距离跑步。你的耐力表现非常出色，配速控制也很稳定。建议明天进行轻松的恢复跑，让身体充分休息。"
        
        distance >= 5 -> "很好的中距离跑步！${String.format("%.1f", distance)}公里的距离对提升有氧能力很有帮助。你的平均配速为${String.format("%.1f", pace)}分钟/公里，表现不错。"
        
        pace < 5.0 -> "哇！你的配速非常快，平均${String.format("%.1f", pace)}分钟/公里。这是一次高强度的训练，记得做好拉伸和恢复。"
        
        else -> "完成了一次很好的跑步训练！坚持就是胜利，每一步都在让你变得更强。建议保持这个节奏，逐步提升距离和强度。"
    }
}

// 生成成就列表
private fun generateAchievements(session: RunningSession): List<String> {
    val achievements = mutableListOf<String>()
    
    val distance = session.totalDistance / 1000.0
    val duration = session.totalDuration / 60000.0 // 分钟
    
    if (distance >= 5.0) achievements.add("完成5公里挑战")
    if (distance >= 10.0) achievements.add("完成10公里长跑")
    if (duration >= 30.0) achievements.add("坚持跑步30分钟")
    if (duration >= 60.0) achievements.add("坚持跑步1小时")
    if (session.averagePace < 5.0) achievements.add("配速达人（5分钟内/公里）")
    if (session.stepCount >= 10000) achievements.add("万步达人")
    
    return achievements
}