package com.runmate.ai.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.ui.viewmodel.GpsStatus
import com.runmate.ai.ui.viewmodel.SensorStatus
import com.runmate.ai.utils.TimeUtils

/**
 * 数据源调试视图 - 用于验证GPS和传感器数据是否正常获取
 */
@Composable
fun DataSourceDebugView(
    session: RunningSession?,
    gpsStatus: GpsStatus,
    sensorStatus: SensorStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔍 数据源调试信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            // GPS状态调试
            DebugSection(
                title = "📡 GPS状态",
                items = listOf(
                    "状态" to if (gpsStatus.isActive) "✅ 活跃" else "❌ 非活跃",
                    "信号强度" to gpsStatus.signalStrength,
                    "精度" to "${gpsStatus.accuracy}m",
                    "GPS点数" to "${gpsStatus.pointsCount}",
                    "最后更新" to if (gpsStatus.lastUpdateTime > 0) {
                        TimeUtils.formatRelativeTime(gpsStatus.lastUpdateTime)
                    } else "从未更新"
                )
            )
            
            Divider()
            
            // 传感器状态调试
            DebugSection(
                title = "📱 传感器状态",
                items = listOf(
                    "状态" to if (sensorStatus.isActive) "✅ 活跃" else "❌ 非活跃",
                    "步频" to "${sensorStatus.stepFrequency} 步/分",
                    "加速度计" to if (sensorStatus.accelerometerAvailable) "✅ 可用" else "❌ 不可用",
                    "步数计" to if (sensorStatus.stepCounterAvailable) "✅ 可用" else "❌ 不可用",
                    "最后更新" to if (sensorStatus.lastUpdateTime > 0) {
                        TimeUtils.formatRelativeTime(sensorStatus.lastUpdateTime)
                    } else "从未更新"
                )
            )
            
            Divider()
            
            // 跑步数据调试
            if (session != null) {
                DebugSection(
                    title = "🏃 跑步数据",
                    items = listOf(
                        "会话ID" to session.sessionId.take(8) + "...",
                        "状态" to session.status.name,
                        "距离" to String.format("%.2f m", session.totalDistance),
                        "时长" to TimeUtils.formatDuration(session.totalDuration),
                        "当前配速" to String.format("%.2f min/km", session.currentPace),
                        "平均配速" to String.format("%.2f min/km", session.averagePace),
                        "步数" to "${session.stepCount}",
                        "卡路里" to "${session.calories}",
                        "开始时间" to TimeUtils.formatTimestamp(session.startTime)
                    )
                )
            } else {
                DebugSection(
                    title = "🏃 跑步数据",
                    items = listOf("状态" to "❌ 无活跃会话")
                )
            }
            
            Divider()
            
            // 系统信息
            DebugSection(
                title = "⚙️ 系统信息",
                items = listOf(
                    "当前时间" to TimeUtils.formatTimestamp(System.currentTimeMillis()),
                    "应用状态" to "运行中",
                    "内存使用" to "正常", // 可以添加实际的内存监控
                    "网络状态" to "已连接" // 可以添加实际的网络状态检查
                )
            )
        }
    }
}

@Composable
fun DebugSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        items.forEach { (key, value) ->
            DebugItem(key = key, value = value)
        }
    }
}

@Composable
fun DebugItem(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * 简化的数据状态指示器
 */
@Composable
fun DataStatusIndicator(
    label: String,
    isActive: Boolean,
    value: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 状态指示灯
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isActive) Color.Green else Color.Red,
                    shape = MaterialTheme.shapes.small
                )
        )
        
        // 标签
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        
        // 值
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}