package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.StudyGuardApp

class PomodoroAlarmReceiver : BroadcastReceiver() {
    private val TAG = "PomodoroAlarmReceiver"

    companion object {
        const val ACTION_FOCUS_FINISHED = "POMO_FOCUS_FINISHED"
        const val ACTION_BREAK_FINISHED = "POMO_BREAK_FINISHED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        val manager = StudyGuardApp.instance.manager
        when (intent.action) {
            ACTION_FOCUS_FINISHED -> {
                manager.handleFocusFinished()
            }
            ACTION_BREAK_FINISHED -> {
                manager.handleBreakFinished()
            }
        }
    }
}
