package com.example.data.preferences

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AlarmProfile
import com.example.data.model.AlarmRepeatBehavior
import com.example.data.model.AlarmSoundType
import com.example.data.model.PomodoroConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "study_guard_preferences")

class StudyGuardPreferences(private val context: Context) {
    private val TAG = "StudyGuardPreferences"

    companion object {
        // Sleep settings
        val KEY_SLEEP_THRESHOLD_MINUTES = intPreferencesKey("sleep_threshold_minutes")
        val KEY_SLEEP_SOUND_TYPE = stringPreferencesKey("sleep_sound_type")
        val KEY_SLEEP_SOUND_TITLE = stringPreferencesKey("sleep_sound_title")
        val KEY_SLEEP_CUSTOM_URI = stringPreferencesKey("sleep_custom_uri")
        val KEY_SLEEP_CUSTOM_FILE = stringPreferencesKey("sleep_custom_file")
        val KEY_SLEEP_VOLUME = floatPreferencesKey("sleep_volume")
        val KEY_SLEEP_REPEAT_BEHAVIOR = stringPreferencesKey("sleep_repeat_behavior")
        val KEY_SLEEP_VIBRATE = booleanPreferencesKey("sleep_vibrate")
        val KEY_SLEEP_SNOOZE_MINUTES = intPreferencesKey("sleep_snooze_minutes")

        // Pomodoro settings
        val KEY_POMO_FOCUS_MINUTES = intPreferencesKey("pomo_focus_minutes")
        val KEY_POMO_BREAK_MINUTES = intPreferencesKey("pomo_break_minutes")
        val KEY_POMO_PRESET_NAME = stringPreferencesKey("pomo_preset_name")

        // Focus Alarm
        val KEY_FOCUS_SOUND_TYPE = stringPreferencesKey("focus_sound_type")
        val KEY_FOCUS_SOUND_TITLE = stringPreferencesKey("focus_sound_title")
        val KEY_FOCUS_CUSTOM_URI = stringPreferencesKey("focus_custom_uri")
        val KEY_FOCUS_CUSTOM_FILE = stringPreferencesKey("focus_custom_file")
        val KEY_FOCUS_VOLUME = floatPreferencesKey("focus_volume")
        val KEY_FOCUS_REPEAT_BEHAVIOR = stringPreferencesKey("focus_repeat_behavior")
        val KEY_FOCUS_VIBRATE = booleanPreferencesKey("focus_vibrate")

        // Break Alarm
        val KEY_BREAK_SOUND_TYPE = stringPreferencesKey("break_sound_type")
        val KEY_BREAK_SOUND_TITLE = stringPreferencesKey("break_sound_title")
        val KEY_BREAK_CUSTOM_URI = stringPreferencesKey("break_custom_uri")
        val KEY_BREAK_CUSTOM_FILE = stringPreferencesKey("break_custom_file")
        val KEY_BREAK_VOLUME = floatPreferencesKey("break_volume")
        val KEY_BREAK_REPEAT_BEHAVIOR = stringPreferencesKey("break_repeat_behavior")
        val KEY_BREAK_VIBRATE = booleanPreferencesKey("break_vibrate")

        // Timer Recovery
        val KEY_POMO_SAVED_STATE = stringPreferencesKey("pomo_saved_state")
        val KEY_POMO_SAVED_DEADLINE = longPreferencesKey("pomo_saved_deadline")
        val KEY_POMO_SAVED_REMAINING = longPreferencesKey("pomo_saved_remaining")
        val KEY_SLEEP_MONITORING_ACTIVE = booleanPreferencesKey("sleep_monitoring_active")
    }

    // Sleep Threshold
    val sleepThresholdMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SLEEP_THRESHOLD_MINUTES] ?: 3
    }

    suspend fun setSleepThresholdMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_SLEEP_THRESHOLD_MINUTES] = minutes.coerceIn(1, 60) }
    }

    // Sleep Alarm Profile
    val sleepAlarmProfile: Flow<AlarmProfile> = context.dataStore.data.map { prefs ->
        val soundTypeStr = prefs[KEY_SLEEP_SOUND_TYPE] ?: AlarmSoundType.BUILT_IN_RADAR.name
        val soundType = try {
            AlarmSoundType.valueOf(soundTypeStr)
        } catch (e: Exception) {
            AlarmSoundType.BUILT_IN_RADAR
        }
        val repeatStr = prefs[KEY_SLEEP_REPEAT_BEHAVIOR] ?: AlarmRepeatBehavior.CONTINUOUS.name
        val repeat = try {
            AlarmRepeatBehavior.valueOf(repeatStr)
        } catch (e: Exception) {
            AlarmRepeatBehavior.CONTINUOUS
        }

        AlarmProfile(
            soundType = soundType,
            soundTitle = prefs[KEY_SLEEP_SOUND_TITLE] ?: "Radar Alarm",
            customUriString = prefs[KEY_SLEEP_CUSTOM_URI],
            customFilePath = prefs[KEY_SLEEP_CUSTOM_FILE],
            volume = prefs[KEY_SLEEP_VOLUME] ?: 0.95f,
            repeatBehavior = repeat,
            vibrate = prefs[KEY_SLEEP_VIBRATE] ?: true,
            snoozeMinutes = prefs[KEY_SLEEP_SNOOZE_MINUTES] ?: 3
        )
    }

    suspend fun updateSleepAlarmProfile(profile: AlarmProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SLEEP_SOUND_TYPE] = profile.soundType.name
            prefs[KEY_SLEEP_SOUND_TITLE] = profile.soundTitle
            if (profile.customUriString != null) prefs[KEY_SLEEP_CUSTOM_URI] = profile.customUriString
            if (profile.customFilePath != null) prefs[KEY_SLEEP_CUSTOM_FILE] = profile.customFilePath
            prefs[KEY_SLEEP_VOLUME] = profile.volume
            prefs[KEY_SLEEP_REPEAT_BEHAVIOR] = profile.repeatBehavior.name
            prefs[KEY_SLEEP_VIBRATE] = profile.vibrate
            prefs[KEY_SLEEP_SNOOZE_MINUTES] = profile.snoozeMinutes
        }
    }

    // Pomodoro Config
    val pomodoroConfig: Flow<PomodoroConfig> = context.dataStore.data.map { prefs ->
        PomodoroConfig(
            focusDurationMinutes = prefs[KEY_POMO_FOCUS_MINUTES] ?: 25,
            breakDurationMinutes = prefs[KEY_POMO_BREAK_MINUTES] ?: 5,
            selectedPresetName = prefs[KEY_POMO_PRESET_NAME] ?: "Classic"
        )
    }

    suspend fun updatePomodoroConfig(config: PomodoroConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POMO_FOCUS_MINUTES] = config.focusDurationMinutes.coerceIn(1, 180)
            prefs[KEY_POMO_BREAK_MINUTES] = config.breakDurationMinutes.coerceIn(1, 60)
            prefs[KEY_POMO_PRESET_NAME] = config.selectedPresetName
        }
    }

    // Focus Alarm Profile
    val focusAlarmProfile: Flow<AlarmProfile> = context.dataStore.data.map { prefs ->
        val soundTypeStr = prefs[KEY_FOCUS_SOUND_TYPE] ?: AlarmSoundType.BUILT_IN_CHIME.name
        val soundType = try {
            AlarmSoundType.valueOf(soundTypeStr)
        } catch (e: Exception) {
            AlarmSoundType.BUILT_IN_CHIME
        }
        val repeatStr = prefs[KEY_FOCUS_REPEAT_BEHAVIOR] ?: AlarmRepeatBehavior.REPEAT_3_TIMES.name
        val repeat = try {
            AlarmRepeatBehavior.valueOf(repeatStr)
        } catch (e: Exception) {
            AlarmRepeatBehavior.REPEAT_3_TIMES
        }

        AlarmProfile(
            soundType = soundType,
            soundTitle = prefs[KEY_FOCUS_SOUND_TITLE] ?: "Gentle Chime",
            customUriString = prefs[KEY_FOCUS_CUSTOM_URI],
            customFilePath = prefs[KEY_FOCUS_CUSTOM_FILE],
            volume = prefs[KEY_FOCUS_VOLUME] ?: 0.85f,
            repeatBehavior = repeat,
            vibrate = prefs[KEY_FOCUS_VIBRATE] ?: true,
            snoozeMinutes = 0
        )
    }

    suspend fun updateFocusAlarmProfile(profile: AlarmProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FOCUS_SOUND_TYPE] = profile.soundType.name
            prefs[KEY_FOCUS_SOUND_TITLE] = profile.soundTitle
            if (profile.customUriString != null) prefs[KEY_FOCUS_CUSTOM_URI] = profile.customUriString
            if (profile.customFilePath != null) prefs[KEY_FOCUS_CUSTOM_FILE] = profile.customFilePath
            prefs[KEY_FOCUS_VOLUME] = profile.volume
            prefs[KEY_FOCUS_REPEAT_BEHAVIOR] = profile.repeatBehavior.name
            prefs[KEY_FOCUS_VIBRATE] = profile.vibrate
        }
    }

    // Break Alarm Profile
    val breakAlarmProfile: Flow<AlarmProfile> = context.dataStore.data.map { prefs ->
        val soundTypeStr = prefs[KEY_BREAK_SOUND_TYPE] ?: AlarmSoundType.BUILT_IN_SIREN.name
        val soundType = try {
            AlarmSoundType.valueOf(soundTypeStr)
        } catch (e: Exception) {
            AlarmSoundType.BUILT_IN_SIREN
        }
        val repeatStr = prefs[KEY_BREAK_REPEAT_BEHAVIOR] ?: AlarmRepeatBehavior.REPEAT_3_TIMES.name
        val repeat = try {
            AlarmRepeatBehavior.valueOf(repeatStr)
        } catch (e: Exception) {
            AlarmRepeatBehavior.REPEAT_3_TIMES
        }

        AlarmProfile(
            soundType = soundType,
            soundTitle = prefs[KEY_BREAK_SOUND_TITLE] ?: "Wake Siren",
            customUriString = prefs[KEY_BREAK_CUSTOM_URI],
            customFilePath = prefs[KEY_BREAK_CUSTOM_FILE],
            volume = prefs[KEY_BREAK_VOLUME] ?: 0.85f,
            repeatBehavior = repeat,
            vibrate = prefs[KEY_BREAK_VIBRATE] ?: true,
            snoozeMinutes = 0
        )
    }

    suspend fun updateBreakAlarmProfile(profile: AlarmProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BREAK_SOUND_TYPE] = profile.soundType.name
            prefs[KEY_BREAK_SOUND_TITLE] = profile.soundTitle
            if (profile.customUriString != null) prefs[KEY_BREAK_CUSTOM_URI] = profile.customUriString
            if (profile.customFilePath != null) prefs[KEY_BREAK_CUSTOM_FILE] = profile.customFilePath
            prefs[KEY_BREAK_VOLUME] = profile.volume
            prefs[KEY_BREAK_REPEAT_BEHAVIOR] = profile.repeatBehavior.name
            prefs[KEY_BREAK_VIBRATE] = profile.vibrate
        }
    }

    // Save timer state for recovery
    suspend fun savePomodoroRecoveryState(stateName: String, deadlineMillis: Long, remainingMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POMO_SAVED_STATE] = stateName
            prefs[KEY_POMO_SAVED_DEADLINE] = deadlineMillis
            prefs[KEY_POMO_SAVED_REMAINING] = remainingMillis
        }
    }

    suspend fun getPomodoroRecoveryState(): Triple<String, Long, Long> {
        val prefs = context.dataStore.data.first()
        val state = prefs[KEY_POMO_SAVED_STATE] ?: "READY"
        val deadline = prefs[KEY_POMO_SAVED_DEADLINE] ?: 0L
        val remaining = prefs[KEY_POMO_SAVED_REMAINING] ?: 0L
        return Triple(state, deadline, remaining)
    }

    suspend fun setSleepMonitoringActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SLEEP_MONITORING_ACTIVE] = active
        }
    }

    suspend fun isSleepMonitoringActive(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_SLEEP_MONITORING_ACTIVE] ?: false
    }

    /**
     * Copies audio from an external content Uri (selected via SAF) to internal private storage
     * so it never expires and remains playable offline indefinitely.
     */
    fun copyCustomAudioToInternal(uri: Uri): Pair<String, String>? {
        return try {
            var fileName = "custom_alarm_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        fileName = displayName
                    }
                }
            }

            val customDir = File(context.filesDir, "custom_sounds")
            if (!customDir.exists()) customDir.mkdirs()

            // Keep extension or default to .mp3/.wav
            val safeFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(customDir, safeFileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d(TAG, "Saved custom audio: ${targetFile.absolutePath}, name: $fileName")
            Pair(fileName, targetFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy custom audio to internal storage", e)
            null
        }
    }
}
