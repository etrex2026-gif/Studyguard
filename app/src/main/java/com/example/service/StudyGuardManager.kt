package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.example.data.audio.AlarmAudioPlayer
import com.example.data.model.AlarmProfile
import com.example.data.model.AlarmType
import com.example.data.model.PomodoroConfig
import com.example.data.model.PomodoroState
import com.example.data.model.SleepMonitoringState
import com.example.data.preferences.StudyGuardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudyGuardManager(val context: Context) {
    private val TAG = "StudyGuardManager"
    val prefs = StudyGuardPreferences(context)
    val audioPlayer = AlarmAudioPlayer(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // --- SLEEP MONITORING STATE ---
    private val _sleepState = MutableStateFlow(SleepMonitoringState.STOPPED)
    val sleepState: StateFlow<SleepMonitoringState> = _sleepState.asStateFlow()

    private val _screenOffDeadline = MutableStateFlow(0L)
    val screenOffDeadline: StateFlow<Long> = _screenOffDeadline.asStateFlow()

    private val _screenOffRemainingSeconds = MutableStateFlow(0)
    val screenOffRemainingSeconds: StateFlow<Int> = _screenOffRemainingSeconds.asStateFlow()

    private val _isScreenCurrentlyOn = MutableStateFlow(true)
    val isScreenCurrentlyOn: StateFlow<Boolean> = _isScreenCurrentlyOn.asStateFlow()

    // --- POMODORO STATE ---
    private val _pomodoroState = MutableStateFlow(PomodoroState.READY)
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private val _pomodoroDeadline = MutableStateFlow(0L)
    val pomodoroDeadline: StateFlow<Long> = _pomodoroDeadline.asStateFlow()

    private val _pomodoroRemainingMillis = MutableStateFlow(25 * 60 * 1000L)
    val pomodoroRemainingMillis: StateFlow<Long> = _pomodoroRemainingMillis.asStateFlow()

    private val _pomodoroTotalMillis = MutableStateFlow(25 * 60 * 1000L)
    val pomodoroTotalMillis: StateFlow<Long> = _pomodoroTotalMillis.asStateFlow()

    // --- ACTIVE ALARMS ---
    private val _activeAlarms = MutableStateFlow<Set<AlarmType>>(emptySet())
    val activeAlarms: StateFlow<Set<AlarmType>> = _activeAlarms.asStateFlow()

    private var countdownJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        // Check power manager initial display state
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isScreenCurrentlyOn.value = pm.isInteractive

        // Initialize Pomodoro total from saved preferences
        scope.launch {
            val config = prefs.pomodoroConfig.first()
            _pomodoroRemainingMillis.value = config.focusDurationMinutes * 60 * 1000L
            _pomodoroTotalMillis.value = config.focusDurationMinutes * 60 * 1000L
        }

        startUiTickLoop()
    }

    private fun startUiTickLoop() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()

                // Update Screen-off countdown if applicable
                if (_sleepState.value == SleepMonitoringState.SCREEN_OFF_COUNTDOWN ||
                    _sleepState.value == SleepMonitoringState.SNOOZED
                ) {
                    val deadline = _screenOffDeadline.value
                    if (deadline > now) {
                        _screenOffRemainingSeconds.value = ((deadline - now) / 1000L).toInt().coerceAtLeast(0)
                    } else {
                        _screenOffRemainingSeconds.value = 0
                    }
                }

                // Update Pomodoro countdown if running
                if (_pomodoroState.value == PomodoroState.FOCUS_RUNNING ||
                    _pomodoroState.value == PomodoroState.BREAK_RUNNING
                ) {
                    val deadline = _pomodoroDeadline.value
                    val remaining = (deadline - now).coerceAtLeast(0L)
                    _pomodoroRemainingMillis.value = remaining
                }

                delay(500L)
            }
        }
    }

    // ==========================================
    // SYSTEM 1: SLEEP INACTIVITY DETECTION
    // ==========================================

    fun startSleepMonitoring() {
        Log.d(TAG, "startSleepMonitoring called")
        scope.launch {
            prefs.setSleepMonitoringActive(true)
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isInteractive = pm.isInteractive
        _isScreenCurrentlyOn.value = isInteractive

        if (isInteractive) {
            _sleepState.value = SleepMonitoringState.MONITORING_SCREEN_ON
        } else {
            // Already off when started
            handleScreenOff()
        }

        // Start Foreground Service
        val serviceIntent = Intent(context, SleepMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopSleepMonitoring() {
        Log.d(TAG, "stopSleepMonitoring called")
        scope.launch {
            prefs.setSleepMonitoringActive(false)
        }
        cancelSleepAlarmSchedule()
        dismissSleepAlarm()
        _sleepState.value = SleepMonitoringState.STOPPED
        _screenOffRemainingSeconds.value = 0
        _screenOffDeadline.value = 0L

        val serviceIntent = Intent(context, SleepMonitoringService::class.java)
        context.stopService(serviceIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationHelper.NOTIF_ID_SLEEP_MONITORING)
    }

    fun handleScreenOff() {
        Log.d(TAG, "handleScreenOff: screen turned OFF")
        _isScreenCurrentlyOn.value = false
        if (_sleepState.value == SleepMonitoringState.STOPPED ||
            _sleepState.value == SleepMonitoringState.ALARM_RINGING
        ) {
            return
        }

        scope.launch {
            val thresholdMinutes = prefs.sleepThresholdMinutes.first()
            val thresholdMillis = thresholdMinutes * 60 * 1000L
            val now = SystemClock.elapsedRealtime()
            val deadline = now + thresholdMillis

            _screenOffDeadline.value = deadline
            _screenOffRemainingSeconds.value = (thresholdMillis / 1000L).toInt()
            _sleepState.value = SleepMonitoringState.SCREEN_OFF_COUNTDOWN

            scheduleExactWakeup(
                triggerAtMillis = deadline,
                action = SleepAlarmReceiver.ACTION_TRIGGER_SLEEP_ALARM,
                requestCode = 1000
            )

            // Update foreground service notification
            val serviceIntent = Intent(context, SleepMonitoringService::class.java).apply {
                action = SleepMonitoringService.ACTION_UPDATE_NOTIF
                putExtra("isScreenOff", true)
            }
            context.startService(serviceIntent)
        }
    }

    fun handleScreenOn() {
        Log.d(TAG, "handleScreenOn: screen turned ON")
        _isScreenCurrentlyOn.value = true
        if (_sleepState.value == SleepMonitoringState.STOPPED) {
            return
        }

        // If a pending screen-off countdown was running, cancel it
        if (_sleepState.value == SleepMonitoringState.SCREEN_OFF_COUNTDOWN ||
            _sleepState.value == SleepMonitoringState.SNOOZED
        ) {
            Log.d(TAG, "Screen turned on before threshold; cancelling pending sleep alarm")
            cancelSleepAlarmSchedule()
            _screenOffDeadline.value = 0L
            _screenOffRemainingSeconds.value = 0
            _sleepState.value = SleepMonitoringState.MONITORING_SCREEN_ON

            // Update notification
            val serviceIntent = Intent(context, SleepMonitoringService::class.java).apply {
                action = SleepMonitoringService.ACTION_UPDATE_NOTIF
                putExtra("isScreenOff", false)
            }
            context.startService(serviceIntent)
        }
    }

    fun triggerSleepAlarm() {
        Log.d(TAG, "triggerSleepAlarm: threshold exceeded!")
        _sleepState.value = SleepMonitoringState.ALARM_RINGING
        acquireWakeLock(30000L)

        scope.launch {
            val profile = prefs.sleepAlarmProfile.first()
            val thresholdMinutes = prefs.sleepThresholdMinutes.first()

            audioPlayer.playAlarm(AlarmType.SLEEP_ALARM, profile)
            _activeAlarms.value = audioPlayer.getActiveAlarmTypes()

            // Post High Priority notification
            val notif = NotificationHelper.buildSleepAlarmNotification(context, thresholdMinutes)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NotificationHelper.NOTIF_ID_SLEEP_ALARM, notif)
        }
    }

    fun dismissSleepAlarm() {
        Log.d(TAG, "dismissSleepAlarm")
        val stoppedAll = audioPlayer.stopAlarm(AlarmType.SLEEP_ALARM)
        _activeAlarms.value = audioPlayer.getActiveAlarmTypes()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIF_ID_SLEEP_ALARM)

        // Return to monitoring if service is running
        if (_sleepState.value == SleepMonitoringState.ALARM_RINGING ||
            _sleepState.value == SleepMonitoringState.SNOOZED
        ) {
            _sleepState.value = if (_isScreenCurrentlyOn.value) {
                SleepMonitoringState.MONITORING_SCREEN_ON
            } else {
                // If screen is still off, re-start countdown from now
                handleScreenOff()
                SleepMonitoringState.SCREEN_OFF_COUNTDOWN
            }
        }
    }

    fun snoozeSleepAlarm() {
        Log.d(TAG, "snoozeSleepAlarm")
        audioPlayer.stopAlarm(AlarmType.SLEEP_ALARM)
        _activeAlarms.value = audioPlayer.getActiveAlarmTypes()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIF_ID_SLEEP_ALARM)

        scope.launch {
            val profile = prefs.sleepAlarmProfile.first()
            val snoozeMinutes = if (profile.snoozeMinutes > 0) profile.snoozeMinutes else 3
            val snoozeMillis = snoozeMinutes * 60 * 1000L
            val deadline = SystemClock.elapsedRealtime() + snoozeMillis

            _screenOffDeadline.value = deadline
            _screenOffRemainingSeconds.value = (snoozeMillis / 1000L).toInt()
            _sleepState.value = SleepMonitoringState.SNOOZED

            scheduleExactWakeup(
                triggerAtMillis = deadline,
                action = SleepAlarmReceiver.ACTION_TRIGGER_SLEEP_ALARM,
                requestCode = 1000
            )
        }
    }

    private fun cancelSleepAlarmSchedule() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_TRIGGER_SLEEP_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ==========================================
    // SYSTEM 2: POMODORO TIMER
    // ==========================================

    fun startPomodoroFocus() {
        Log.d(TAG, "startPomodoroFocus")
        scope.launch {
            val config = prefs.pomodoroConfig.first()
            val durationMillis = config.focusDurationMinutes * 60 * 1000L
            val now = SystemClock.elapsedRealtime()
            val deadline = now + durationMillis

            _pomodoroTotalMillis.value = durationMillis
            _pomodoroRemainingMillis.value = durationMillis
            _pomodoroDeadline.value = deadline
            _pomodoroState.value = PomodoroState.FOCUS_RUNNING

            scheduleExactWakeup(
                triggerAtMillis = deadline,
                action = PomodoroAlarmReceiver.ACTION_FOCUS_FINISHED,
                requestCode = 2000
            )

            startPomodoroForegroundService("Focus Session", durationMillis, true)
            prefs.savePomodoroRecoveryState("FOCUS_RUNNING", deadline, durationMillis)
        }
    }

    fun pausePomodoro() {
        Log.d(TAG, "pausePomodoro")
        val state = _pomodoroState.value
        if (state != PomodoroState.FOCUS_RUNNING && state != PomodoroState.BREAK_RUNNING) return

        val now = SystemClock.elapsedRealtime()
        val remaining = (_pomodoroDeadline.value - now).coerceAtLeast(0L)
        _pomodoroRemainingMillis.value = remaining

        cancelPomodoroAlarmSchedule()

        _pomodoroState.value = if (state == PomodoroState.FOCUS_RUNNING) {
            PomodoroState.FOCUS_PAUSED
        } else {
            PomodoroState.BREAK_PAUSED
        }

        val phase = if (state == PomodoroState.FOCUS_RUNNING) "Focus (Paused)" else "Break (Paused)"
        startPomodoroForegroundService(phase, remaining, false)

        scope.launch {
            prefs.savePomodoroRecoveryState(_pomodoroState.value.name, 0L, remaining)
        }
    }

    fun resumePomodoro() {
        Log.d(TAG, "resumePomodoro")
        val state = _pomodoroState.value
        if (state != PomodoroState.FOCUS_PAUSED && state != PomodoroState.BREAK_PAUSED) return

        val remaining = _pomodoroRemainingMillis.value
        val now = SystemClock.elapsedRealtime()
        val newDeadline = now + remaining

        _pomodoroDeadline.value = newDeadline

        val isFocus = (state == PomodoroState.FOCUS_PAUSED)
        _pomodoroState.value = if (isFocus) PomodoroState.FOCUS_RUNNING else PomodoroState.BREAK_RUNNING

        val action = if (isFocus) {
            PomodoroAlarmReceiver.ACTION_FOCUS_FINISHED
        } else {
            PomodoroAlarmReceiver.ACTION_BREAK_FINISHED
        }

        scheduleExactWakeup(
            triggerAtMillis = newDeadline,
            action = action,
            requestCode = 2000
        )

        val phase = if (isFocus) "Focus Session" else "Break Rest"
        startPomodoroForegroundService(phase, remaining, true)

        scope.launch {
            prefs.savePomodoroRecoveryState(_pomodoroState.value.name, newDeadline, remaining)
        }
    }

    fun stopPomodoro() {
        Log.d(TAG, "stopPomodoro")
        cancelPomodoroAlarmSchedule()
        dismissPomodoroAlarm()

        scope.launch {
            val config = prefs.pomodoroConfig.first()
            val focusMillis = config.focusDurationMinutes * 60 * 1000L
            _pomodoroState.value = PomodoroState.READY
            _pomodoroTotalMillis.value = focusMillis
            _pomodoroRemainingMillis.value = focusMillis
            _pomodoroDeadline.value = 0L

            prefs.savePomodoroRecoveryState("READY", 0L, focusMillis)
        }

        val serviceIntent = Intent(context, PomodoroForegroundService::class.java)
        context.stopService(serviceIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIF_ID_POMO_TIMER)
    }

    fun skipBreak() {
        Log.d(TAG, "skipBreak")
        cancelPomodoroAlarmSchedule()
        dismissPomodoroAlarm()

        _pomodoroState.value = PomodoroState.READY_FOR_NEXT_FOCUS
        _pomodoroDeadline.value = 0L

        scope.launch {
            val config = prefs.pomodoroConfig.first()
            _pomodoroRemainingMillis.value = config.focusDurationMinutes * 60 * 1000L
            _pomodoroTotalMillis.value = config.focusDurationMinutes * 60 * 1000L
            prefs.savePomodoroRecoveryState("READY_FOR_NEXT_FOCUS", 0L, _pomodoroRemainingMillis.value)
        }

        val serviceIntent = Intent(context, PomodoroForegroundService::class.java)
        context.stopService(serviceIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIF_ID_POMO_TIMER)
    }

    fun handleFocusFinished() {
        Log.d(TAG, "handleFocusFinished: focus complete, playing alarm and transitioning to BREAK_RUNNING")
        _pomodoroState.value = PomodoroState.BREAK_RUNNING
        acquireWakeLock(15000L)

        // 1. Play Focus alarm independently
        scope.launch {
            val profile = prefs.focusAlarmProfile.first()
            audioPlayer.playAlarm(AlarmType.POMODORO_FOCUS_COMPLETE, profile)
            _activeAlarms.value = audioPlayer.getActiveAlarmTypes()
        }

        // 2. Post completion notification
        val notif = NotificationHelper.buildPomodoroCompletionNotification(
            context = context,
            isFocusFinished = true,
            nextPhaseDescription = "Break has started"
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NotificationHelper.NOTIF_ID_POMO_ALARM, notif)

        // 3. Immediately start break countdown without waiting for alarm dismissal!
        scope.launch {
            val config = prefs.pomodoroConfig.first()
            val breakMillis = config.breakDurationMinutes * 60 * 1000L
            val now = SystemClock.elapsedRealtime()
            val breakDeadline = now + breakMillis

            _pomodoroTotalMillis.value = breakMillis
            _pomodoroRemainingMillis.value = breakMillis
            _pomodoroDeadline.value = breakDeadline

            scheduleExactWakeup(
                triggerAtMillis = breakDeadline,
                action = PomodoroAlarmReceiver.ACTION_BREAK_FINISHED,
                requestCode = 2000
            )

            startPomodoroForegroundService("Break Rest", breakMillis, true)
            prefs.savePomodoroRecoveryState("BREAK_RUNNING", breakDeadline, breakMillis)
        }
    }

    fun handleBreakFinished() {
        Log.d(TAG, "handleBreakFinished: break complete, playing alarm and waiting for manual start")
        _pomodoroState.value = PomodoroState.READY_FOR_NEXT_FOCUS
        _pomodoroDeadline.value = 0L
        acquireWakeLock(15000L)

        // 1. Play Break completion alarm independently
        scope.launch {
            val profile = prefs.breakAlarmProfile.first()
            audioPlayer.playAlarm(AlarmType.POMODORO_BREAK_COMPLETE, profile)
            _activeAlarms.value = audioPlayer.getActiveAlarmTypes()
        }

        // 2. Post completion notification
        val notif = NotificationHelper.buildPomodoroCompletionNotification(
            context = context,
            isFocusFinished = false,
            nextPhaseDescription = "Ready for next session"
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NotificationHelper.NOTIF_ID_POMO_ALARM, notif)

        // 3. Transition to READY_FOR_NEXT_FOCUS and wait for manual user action!
        _pomodoroState.value = PomodoroState.READY_FOR_NEXT_FOCUS
        _pomodoroDeadline.value = 0L

        scope.launch {
            val config = prefs.pomodoroConfig.first()
            _pomodoroRemainingMillis.value = config.focusDurationMinutes * 60 * 1000L
            _pomodoroTotalMillis.value = config.focusDurationMinutes * 60 * 1000L
            prefs.savePomodoroRecoveryState("READY_FOR_NEXT_FOCUS", 0L, _pomodoroRemainingMillis.value)
        }

        val serviceIntent = Intent(context, PomodoroForegroundService::class.java)
        context.stopService(serviceIntent)
    }

    fun dismissPomodoroAlarm() {
        Log.d(TAG, "dismissPomodoroAlarm")
        audioPlayer.stopAlarm(AlarmType.POMODORO_FOCUS_COMPLETE)
        audioPlayer.stopAlarm(AlarmType.POMODORO_BREAK_COMPLETE)
        _activeAlarms.value = audioPlayer.getActiveAlarmTypes()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIF_ID_POMO_ALARM)
    }

    private fun cancelPomodoroAlarmSchedule() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intentFocus = Intent(context, PomodoroAlarmReceiver::class.java).apply {
            action = PomodoroAlarmReceiver.ACTION_FOCUS_FINISHED
        }
        val piFocus = PendingIntent.getBroadcast(
            context,
            2000,
            intentFocus,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(piFocus)

        val intentBreak = Intent(context, PomodoroAlarmReceiver::class.java).apply {
            action = PomodoroAlarmReceiver.ACTION_BREAK_FINISHED
        }
        val piBreak = PendingIntent.getBroadcast(
            context,
            2000,
            intentBreak,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(piBreak)
    }

    private fun startPomodoroForegroundService(phase: String, remainingMillis: Long, isRunning: Boolean) {
        val serviceIntent = Intent(context, PomodoroForegroundService::class.java).apply {
            putExtra(PomodoroForegroundService.EXTRA_PHASE, phase)
            putExtra(PomodoroForegroundService.EXTRA_REMAINING_MILLIS, remainingMillis)
            putExtra(PomodoroForegroundService.EXTRA_IS_RUNNING, isRunning)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    // ==========================================
    // UTILITIES & WAKE LOCKS
    // ==========================================

    private fun scheduleExactWakeup(triggerAtMillis: Long, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val receiverClass = if (action.startsWith("POMO_")) {
            PomodoroAlarmReceiver::class.java
        } else {
            SleepAlarmReceiver::class.java
        }
        val intent = Intent(context, receiverClass).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled exact wakeup for $action at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to window/inexact", e)
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    private fun acquireWakeLock(timeoutMillis: Long) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock?.release()
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "StudyGuard:AlarmWakeLock"
            )
            wakeLock?.acquire(timeoutMillis)
        } catch (e: Exception) {
            Log.e(TAG, "Could not acquire bright wake lock, trying partial", e)
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StudyGuard:PartialWakeLock")
                wakeLock?.acquire(timeoutMillis)
            } catch (ex: Exception) {
                Log.e(TAG, "Could not acquire partial wake lock", ex)
            }
        }
    }
}
