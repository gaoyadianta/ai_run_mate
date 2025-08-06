package com.runmate.ai.ui.running

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.utils.DistanceUtils
import com.runmate.ai.utils.TimeUtils

/**
 * 跑步数据实时显示组件
 * 显示GPS和传感器采集的实时数据
 */
@Composable
fun RunningDataDisplay(
    session: RunningSession?,
    stepFrequency: Int = 0,
    gpsSignalStrength: String = "强",
    isGpsActive: Boolean = true,
    isSensorActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // GPS和传感器状态指示器
        DataSourceIndicators(
            isGpsActive = isGpsActive,
            isSensorActive = isSensorActive,
            gpsSignalStrength = gpsSignalStrength
        )
        
        // 主要数据显示
        MainDataSection(session)
        
        // 次要数据显示
        SecondaryDataSection(session, stepFrequency)
        
        // 实时状态信息
        if (session != null) {
            RealTimeStatusInfo(session)
        }
    }
}

@Composable
fun DataSourceIndicators(
    isGpsActive: Boolean,
    isSensorActive: Boolean,
    gpsSignalStrength: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPS状态
            StatusIndicator(
                label = "GPS",
                isActive = isGpsActive,
                detail = gpsSignalStrength,
                icon = "🛰️"
            )
            
            // 传感器状态
            StatusIndicator(
                label = "传感器",
                isActive = isSensorActive,
                detail = if (isSensorActive) "正常" else "异常",
                icon = "📱"
            )
        }
    }
}

@Composable
fun StatusIndicator(
    label: String,
    isActive: Boolean,
    detail: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MainDataSection(session: RunningSession?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 距离 - 最重要的数据，显示最大
        MainDataItem(
            label = "距离",
            value = if (session != null) {
                String.format("%.2f", session.totalDistance / 1000.0)
            } else "0.00",
            unit = "km",
            isLarge = true
        )
        
        // 时间
        MainDataItem(
            label = "时间",
            value = TimeUtils.formatDuration(session?.totalDuration ?: 0L),
            unit = "",
            isLarge = false
        )
    }
}

@Composable
fun SecondaryDataSection(session: RunningSession?, stepFrequency: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 当前配速
        SecondaryDataItem(
            label = "当前配速",
            value = if (session?.currentPace != null && session.currentPace > 0) {
                String.format("%.1f", session.currentPace)
            } else "--",
            unit = "'/km"
        )
        
        // 平均配速
        SecondaryDataItem(
            label = "平均配速",
            value = if (session?.averagePace != null && session.averagePace > 0) {
                String.format("%.1f", session.averagePace)
            } else "--",
            unit = "'/km"
        )
        
        // 步频
        SecondaryDataItem(
            label = "步频",
            value = stepFrequency.toString(),
            unit = "步/分"
        )
    }
}

@Composable
fun RealTimeStatusInfo(session: RunningSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "实时状态",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusInfoItem("步数", "${session.stepCount} 步")
                StatusInfoItem("卡路里", "${session.calories} kcal")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusInfoItem(
                    "速度", 
                    String.format("%.1f km/h", DistanceUtils.paceToSpeed(session.currentPace))
                )
                StatusInfoItem("状态", session.status.name)
            }
        }
    }
}

@Composable
fun MainDataItem(
    label: String,
    value: String,
    unit: String,
    isLarge: Boolean
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
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = if (isLarge) {
                    MaterialTheme.typography.displayLarge
                } else {
                    MaterialTheme.typography.headlineLarge
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SecondaryDataItem(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun StatusInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}