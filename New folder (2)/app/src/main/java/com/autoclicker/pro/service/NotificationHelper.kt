package com.autoclicker.pro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autoclicker.pro.R
import com.autoclicker.pro.presentation.main.MainActivity

object NotificationHelper {
    const val OVERLAY_CHANNEL_ID = "overlay_status_channel"
    const val EXECUTION_CHANNEL_ID = "execution_status_channel"

    const val OVERLAY_NOTIF_ID = 1001
    const val EXECUTION_NOTIF_ID = 1002

    const val ACTION_PAUSE = "com.autoclicker.pro.action.PAUSE"
    const val ACTION_STOP = "com.autoclicker.pro.action.STOP"
    const val ACTION_RESUME = "com.autoclicker.pro.action.RESUME"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "Overlay control panel",
                NotificationManager.IMPORTANCE_MIN
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                EXECUTION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    fun buildOverlayNotification(context: Context): android.app.Notification {
        ensureChannels(context)
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, OVERLAY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle("Floating panel active")
            .setContentText("Tap to open Auto Clicker Pro")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    fun buildExecutionNotification(context: Context, isPaused: Boolean, clickCount: Long): android.app.Notification {
        ensureChannels(context)
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeAction = if (isPaused) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Resume",
                actionPendingIntent(context, ACTION_RESUME)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                actionPendingIntent(context, ACTION_PAUSE)
            )
        }

        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel, "Stop",
            actionPendingIntent(context, ACTION_STOP)
        )

        val title = context.getString(
            if (isPaused) R.string.notification_paused_title else R.string.notification_active_title
        )

        return NotificationCompat.Builder(context, EXECUTION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle(title)
            .setContentText("Clicks performed: $clickCount")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun actionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
