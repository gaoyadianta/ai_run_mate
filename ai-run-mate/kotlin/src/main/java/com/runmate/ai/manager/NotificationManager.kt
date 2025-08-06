package com.runmate.ai.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.runmate.ai.R
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.ui.MainActivity
import com.runmate.ai.ui.running.RunningActivity
import com.runmate.ai.utils.DistanceUtils
import com.runmate.ai.utils.TimeUtils

/**
 * 通知管理器
 * 负责管理跑步相关的所有通知
 */
class NotificationManager(private val context: Context) {
    
    companion object {
        // 通知渠道
        const val CHANNEL_RUNNING = "running_channel"
        const val CHANNEL_SUMMARY = "summary_channel"
        const val CHANNEL_REMINDER = "reminder_channel"
        
        // 通知ID
        const val NOTIFICATION_RUNNING = 1001
        const val NOTIFICATION_SUMMARY = 1002
        const val NOTIFICATION_REMINDER = 1003
        const val NOTIFICATION_ACHIEVEMENT = 1004
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 跑步进行中通知渠道
            val runningChannel = NotificationChannel(
                CHANNEL_RUNNING,
                "跑步进行中",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示跑步进度和实时数据"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            // 跑步总结通知渠道
            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "跑步总结",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "跑步结束后的总结通知"
                setShowBadge(true)
            }
            
            // 提醒通知渠道
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "运动提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "运动提醒和激励通知"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(runningChannel, summaryChannel, reminderChannel)
            )
        }
    }
    
    /**
     * 创建跑步进行中的通知
     */
    fun createRunningNotification(session: RunningSession): android.app.Notification {
        val distance = DistanceUtils.formatDistance(session.totalDistance)
        val duration = TimeUtils.formatDuration(session.totalDuration)
        val pace = TimeUtils.formatPace(session.currentPace)
        
        val intent = Intent(context, RunningActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setContentTitle("AI跑伴 - 跑步中")
            .setContentText("距离: $distance | 时间: $duration | 配速: $pace")
            .setSmallIcon(R.drawable.ic_running)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(createPauseAction())
            .addAction(createStopAction())
            .build()
    }
    
    /**
     * 创建跑步总结通知
     */
    fun showSummaryNotification(session: RunningSession) {
        val distance = DistanceUtils.formatDistance(session.totalDistance)
        val duration = TimeUtils.formatDuration(session.totalDuration)
        val pace = TimeUtils.formatPace(session.averagePace)
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setContentTitle("跑步完成！")
            .setContentText("距离: $distance | 时间: $duration | 平均配速: $pace")
            .setSmallIcon(R.drawable.ic_running)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🎉 恭喜完成跑步！\n📏 距离: $distance\n⏱️ 时间: $duration\n🏃 平均配速: $pace\n🔥 卡路里: ${session.calories} kcal")
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_SUMMARY, notification)
    }
    
    /**
     * 创建成就通知
     */
    fun showAchievementNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setContentTitle("🏆 $title")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_running)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_ACHIEVEMENT, notification)
    }
    
    /**
     * 创建运动提醒通知
     */
    fun showReminderNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_running)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notificationManager.notify(NOTIFICATION_REMINDER, notification)
    }
    
    /**
     * 创建暂停操作
     */
    private fun createPauseAction(): NotificationCompat.Action {
        val intent = Intent(context, RunningActivity::class.java).apply {
            action = "ACTION_PAUSE"
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_running,
            "暂停",
            pendingIntent
        ).build()
    }
    
    /**
     * 创建停止操作
     */
    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(context, RunningActivity::class.java).apply {
            action = "ACTION_STOP"
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_running,
            "停止",
            pendingIntent
        ).build()
    }
    
    /**
     * 更新跑步通知
     */
    fun updateRunningNotification(session: RunningSession) {
        val notification = createRunningNotification(session)
        notificationManager.notify(NOTIFICATION_RUNNING, notification)
    }
    
    /**
     * 取消跑步通知
     */
    fun cancelRunningNotification() {
        notificationManager.cancel(NOTIFICATION_RUNNING)
    }
    
    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * 检查通知权限
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}