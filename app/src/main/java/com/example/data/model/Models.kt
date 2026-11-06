package com.example.data.model

enum class SleepMonitoringState {
    STOPPED,
    MONITORING_SCREEN_ON,
    SCREEN_OFF_COUNTDOWN,
    ALARM_RINGING,
    SNOOZED,
    PERMISSION_BLOCKED
}

enum class PomodoroState {
    READY,
    FOCUS_RUNNING,
    FOCUS_PAUSED,
    BREAK_RUNNING,
    BREAK_PAUSED,
    READY_FOR_NEXT_FOCUS
}

enum class AlarmType {
    SLEEP_ALARM,
    POMODORO_FOCUS_COMPLETE,
    POMODORO_BREAK_COMPLETE
}

enum class AlarmSoundType {
    BUILT_IN_RADAR,
    BUILT_IN_CHIME,
    BUILT_IN_SIREN,
    SYSTEM_ALARM,
    SYSTEM_NOTIFICATION,
    CUSTOM
}

enum class AlarmRepeatBehavior {
    PLAY_ONCE,
    REPEAT_3_TIMES,
    CONTINUOUS,
    AUTO_STOP_1_MIN,
    AUTO_STOP_2_MIN
}

data class AlarmProfile(
    val soundType: AlarmSoundType = AlarmSoundType.BUILT_IN_RADAR,
    val soundTitle: String = "Radar Alarm",
    val customUriString: String? = null,
    val customFilePath: String? = null,
    val volume: Float = 0.9f, // 0.0f to 1.0f
    val repeatBehavior: AlarmRepeatBehavior = AlarmRepeatBehavior.CONTINUOUS,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 3
)

data class PomodoroConfig(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val selectedPresetName: String = "Classic"
)
