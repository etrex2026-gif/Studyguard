package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.StudyGuardApp
import com.example.ui.AlarmActivity

class SleepAlarmReceiver : BroadcastReceiver() {
    private val TAG = "SleepAlarmReceiver"

    companion object {
        const val ACTION_TRIGGER_SLEEP_ALARM = "com.example.service.TRIGGER_SLEEP_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        if (intent.action == ACTION_TRIGGER_SLEEP_ALARM) {
            val manager = StudyGuardApp.instance.manager
            manager.triggerSleepAlarm()

            // Attempt to launch full-screen AlarmActivity
            try {
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmActivity.EXTRA_ALARM_SOURCE, "SLEEP_ALARM")
                }
                context.startActivity(alarmIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not start AlarmActivity directly, fallback notification active", e)
            }
        }
    }
}
