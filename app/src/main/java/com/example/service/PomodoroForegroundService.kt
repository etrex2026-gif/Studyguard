package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.StudyGuardApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class PomodoroForegroundService : Service() {
    private val TAG = "PomodoroForegroundService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var notificationUpdaterJob: Job? = null
    private var currentPhase: String = "Focus Session"
    private var isRunning: Boolean = true

    companion object {
        const val EXTRA_PHASE = "extra_phase"
        const val EXTRA_REMAINING_MILLIS = "extra_remaining_millis"
        const val EXTRA_IS_RUNNING = "extra_is_running"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PomodoroForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            currentPhase = it.getStringExtra(EXTRA_PHASE) ?: currentPhase
            isRunning = it.getBooleanExtra(EXTRA_IS_RUNNING, isRunning)
        }

        val remainingMillis = StudyGuardApp.instance.manager.pomodoroRemainingMillis.value
        val formattedTime = formatTime(remainingMillis)

        val notif = NotificationHelper.buildPomodoroTimerNotification(
            context = this,
            phase = currentPhase,
            timeFormatted = formattedTime,
            isRunning = isRunning
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.NOTIF_ID_POMO_TIMER,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NotificationHelper.NOTIF_ID_POMO_TIMER,
                    notif
                )
            }
        } else {
            startForeground(NotificationHelper.NOTIF_ID_POMO_TIMER, notif)
        }

        startNotificationTicker()
        return START_STICKY
    }

    private fun startNotificationTicker() {
        notificationUpdaterJob?.cancel()
        notificationUpdaterJob = serviceScope.launch {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            while (isActive) {
                delay(1000L)
                val remainingMillis = StudyGuardApp.instance.manager.pomodoroRemainingMillis.value
                val formattedTime = formatTime(remainingMillis)

                val notif = NotificationHelper.buildPomodoroTimerNotification(
                    context = this@PomodoroForegroundService,
                    phase = currentPhase,
                    timeFormatted = formattedTime,
                    isRunning = isRunning
                )
                nm.notify(NotificationHelper.NOTIF_ID_POMO_TIMER, notif)
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PomodoroForegroundService destroyed")
        notificationUpdaterJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
