package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.StudyGuardApp

class SleepMonitoringService : Service() {
    private val TAG = "SleepMonitoringService"
    private var screenReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_UPDATE_NOTIF = "com.example.service.UPDATE_SLEEP_NOTIF"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SleepMonitoringService created")

        val initialNotif = NotificationHelper.buildSleepMonitoringNotification(
            context = this,
            statusText = "Monitoring Active",
            isScreenOff = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.NOTIF_ID_SLEEP_MONITORING,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NotificationHelper.NOTIF_ID_SLEEP_MONITORING,
                    initialNotif
                )
            }
        } else {
            startForeground(NotificationHelper.NOTIF_ID_SLEEP_MONITORING, initialNotif)
        }

        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_NOTIF) {
            val isScreenOff = intent.getBooleanExtra("isScreenOff", false)
            val notif = NotificationHelper.buildSleepMonitoringNotification(
                context = this,
                statusText = if (isScreenOff) "Screen Off — Inactivity Countdown" else "Screen On — Waiting",
                isScreenOff = isScreenOff
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NotificationHelper.NOTIF_ID_SLEEP_MONITORING, notif)
        }
        return START_STICKY
    }

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Received ACTION_SCREEN_OFF")
                        StudyGuardApp.instance.manager.handleScreenOff()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d(TAG, "Received ACTION_SCREEN_ON")
                        StudyGuardApp.instance.manager.handleScreenOn()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
        Log.d(TAG, "Registered screen receiver for ACTION_SCREEN_OFF and ACTION_SCREEN_ON")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SleepMonitoringService destroyed")
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver)
                screenReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering screen receiver", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
