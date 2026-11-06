package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.StudyGuardApp

class AlarmActionReceiver : BroadcastReceiver() {
    private val TAG = "AlarmActionReceiver"

    companion object {
        const val ACTION_STOP_SLEEP_MONITORING = "com.example.service.STOP_SLEEP_MONITORING"
        const val ACTION_DISMISS_SLEEP_ALARM = "com.example.service.DISMISS_SLEEP_ALARM"
        const val ACTION_SNOOZE_SLEEP_ALARM = "com.example.service.SNOOZE_SLEEP_ALARM"
        const val ACTION_PAUSE_POMODORO = "com.example.service.PAUSE_POMODORO"
        const val ACTION_RESUME_POMODORO = "com.example.service.RESUME_POMODORO"
        const val ACTION_STOP_POMODORO = "com.example.service.STOP_POMODORO"
        const val ACTION_DISMISS_POMODORO_ALARM = "com.example.service.DISMISS_POMODORO_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val manager = StudyGuardApp.instance.manager
        Log.d(TAG, "onReceive action: ${intent.action}")
        when (intent.action) {
            ACTION_STOP_SLEEP_MONITORING -> {
                manager.stopSleepMonitoring()
            }
            ACTION_DISMISS_SLEEP_ALARM -> {
                manager.dismissSleepAlarm()
            }
            ACTION_SNOOZE_SLEEP_ALARM -> {
                manager.snoozeSleepAlarm()
            }
            ACTION_PAUSE_POMODORO -> {
                manager.pausePomodoro()
            }
            ACTION_RESUME_POMODORO -> {
                manager.resumePomodoro()
            }
            ACTION_STOP_POMODORO -> {
                manager.stopPomodoro()
            }
            ACTION_DISMISS_POMODORO_ALARM -> {
                manager.dismissPomodoroAlarm()
            }
        }
    }
}
