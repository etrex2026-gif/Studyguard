package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.AlarmActivity

object NotificationHelper {
    const val CHANNEL_SLEEP_MONITORING = "study_guard_sleep_monitoring"
    const val CHANNEL_SLEEP_ALARM = "study_guard_sleep_alarm"
    const val CHANNEL_POMO_TIMER = "study_guard_pomodoro_timer"
    const val CHANNEL_POMO_ALARM = "study_guard_pomodoro_alarm"

    const val NOTIF_ID_SLEEP_MONITORING = 1001
    const val NOTIF_ID_SLEEP_ALARM = 1002
    const val NOTIF_ID_POMO_TIMER = 2001
    const val NOTIF_ID_POMO_ALARM = 2002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val sleepMonChannel = NotificationChannel(
                CHANNEL_SLEEP_MONITORING,
                "Sleep Inactivity Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing status when Study Guard is monitoring tablet screen state"
                setShowBadge(false)
            }

            val sleepAlarmChannel = NotificationChannel(
                CHANNEL_SLEEP_ALARM,
                "Wake-Up Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency wake-up alarms when the screen has been off past threshold"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val pomoTimerChannel = NotificationChannel(
                CHANNEL_POMO_TIMER,
                "Pomodoro Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining time during study focus and break sessions"
                setShowBadge(false)
            }

            val pomoAlarmChannel = NotificationChannel(
                CHANNEL_POMO_ALARM,
                "Pomodoro Phase Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a focus or break session finishes"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannels(
                listOf(sleepMonChannel, sleepAlarmChannel, pomoTimerChannel, pomoAlarmChannel)
            )
        }
    }

    fun buildSleepMonitoringNotification(
        context: Context,
        statusText: String,
        isScreenOff: Boolean = false
    ): Notification {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_STOP_SLEEP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = if (isScreenOff) {
            "Screen Off: Countdown running..."
        } else {
            "Screen On: Waiting for inactivity"
        }

        return NotificationCompat.Builder(context, CHANNEL_SLEEP_MONITORING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Study Guard: Sleep Monitoring Active")
            .setContentText("$statusText • $subtitle")
            .setSubText("Study Guard")
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "STOP MONITORING",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun buildSleepAlarmNotification(context: Context, thresholdMinutes: Int): Notification {
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_SOURCE, "SLEEP_ALARM")
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            101,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS_SLEEP_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE_SLEEP_ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            103,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SLEEP_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WAKE UP! INACTIVITY DETECTED")
            .setContentText("Screen was off for more than $thresholdMinutes min. Tap to dismiss.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_delete, "STOP ALARM", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "SNOOZE", snoozePendingIntent)
            .build()
    }

    fun buildPomodoroTimerNotification(
        context: Context,
        phase: String,
        timeFormatted: String,
        isRunning: Boolean
    ): Notification {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            200,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_POMO_TIMER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Pomodoro: $phase")
            .setContentText("Remaining time: $timeFormatted")
            .setSubText("Study Guard")
            .setOngoing(isRunning)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (isRunning) {
            val pauseIntent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_PAUSE_POMODORO
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "PAUSE", pausePendingIntent)
        } else {
            val resumeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_RESUME_POMODORO
            }
            val resumePendingIntent = PendingIntent.getBroadcast(
                context,
                202,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "RESUME", resumePendingIntent)
        }

        val stopIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_STOP_POMODORO
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            203,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPendingIntent)

        return builder.build()
    }

    fun buildPomodoroCompletionNotification(
        context: Context,
        isFocusFinished: Boolean,
        nextPhaseDescription: String
    ): Notification {
        val title = if (isFocusFinished) "Focus Session Finished!" else "Break Finished!"
        val text = if (isFocusFinished) {
            "Great work! Break session has started automatically."
        } else {
            "Break over! Ready when you are for the next focus session."
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            210,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS_POMODORO_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            211,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_POMO_ALARM)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(nextPhaseDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISMISS ALARM", dismissPendingIntent)
            .build()
    }
}
