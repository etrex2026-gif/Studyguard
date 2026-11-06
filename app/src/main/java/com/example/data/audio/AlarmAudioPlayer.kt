package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.model.AlarmProfile
import com.example.data.model.AlarmRepeatBehavior
import com.example.data.model.AlarmSoundType
import com.example.data.model.AlarmType
import java.io.File

class AlarmAudioPlayer(private val context: Context) {
    private val TAG = "AlarmAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isPlayingAlarm = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null
    private var repeatCount = 0
    private var maxRepeats = -1 // -1 = continuous
    private val activeAlarmTypes = mutableSetOf<AlarmType>()

    init {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize vibrator", e)
        }
    }

    @Synchronized
    fun playAlarm(alarmType: AlarmType, profile: AlarmProfile) {
        Log.d(TAG, "playAlarm triggered for type: $alarmType")
        activeAlarmTypes.add(alarmType)

        // Stop any preview sound
        stopPreview()

        // If an alarm is already playing, add to active types and ensure volume/loop is highest
        if (isPlayingAlarm && mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "Alarm already active, added $alarmType to active list")
            return
        }

        stopAlarmPlaybackOnly()
        isPlayingAlarm = true

        try {
            val player = MediaPlayer()
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            player.setAudioAttributes(audioAttributes)

            val uri = resolveUri(profile)
            if (uri != null) {
                player.setDataSource(context, uri)
            } else {
                // Fallback to default system alarm
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                player.setDataSource(context, fallbackUri)
            }

            val volume = profile.volume.coerceIn(0.01f, 1.0f)
            player.setVolume(volume, volume)

            when (profile.repeatBehavior) {
                AlarmRepeatBehavior.CONTINUOUS -> {
                    player.isLooping = true
                    maxRepeats = -1
                }
                AlarmRepeatBehavior.PLAY_ONCE -> {
                    player.isLooping = false
                    maxRepeats = 1
                }
                AlarmRepeatBehavior.REPEAT_3_TIMES -> {
                    player.isLooping = false
                    maxRepeats = 3
                }
                AlarmRepeatBehavior.AUTO_STOP_1_MIN -> {
                    player.isLooping = true
                    maxRepeats = -1
                    scheduleAutoStop(60 * 1000L)
                }
                AlarmRepeatBehavior.AUTO_STOP_2_MIN -> {
                    player.isLooping = true
                    maxRepeats = -1
                    scheduleAutoStop(120 * 1000L)
                }
            }

            repeatCount = 1

            player.setOnCompletionListener {
                if (maxRepeats > 0 && repeatCount < maxRepeats) {
                    repeatCount++
                    try {
                        player.seekTo(0)
                        player.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error repeating audio", e)
                    }
                } else if (maxRepeats > 0) {
                    stopAlarm(alarmType)
                }
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                stopAlarmPlaybackOnly()
                false
            }

            player.prepare()
            player.start()
            mediaPlayer = player

            if (profile.vibrate) {
                startVibration()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm audio", e)
            // Try fallback beep
            fallbackSystemTone()
        }
    }

    private fun scheduleAutoStop(delayMillis: Long) {
        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = Runnable {
            Log.d(TAG, "Auto-stop triggered after timeout")
            stopAllAlarms()
        }
        mainHandler.postDelayed(autoStopRunnable!!, delayMillis)
    }

    @Synchronized
    fun stopAlarm(alarmType: AlarmType): Boolean {
        Log.d(TAG, "stopAlarm for $alarmType, active before: $activeAlarmTypes")
        activeAlarmTypes.remove(alarmType)
        if (activeAlarmTypes.isEmpty()) {
            stopAlarmPlaybackOnly()
            return true // all alarms stopped
        }
        return false // other alarms still active
    }

    @Synchronized
    fun stopAllAlarms() {
        activeAlarmTypes.clear()
        stopAlarmPlaybackOnly()
    }

    @Synchronized
    fun getActiveAlarmTypes(): Set<AlarmType> {
        return activeAlarmTypes.toSet()
    }

    private fun stopAlarmPlaybackOnly() {
        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            isPlayingAlarm = false
        }
        stopVibration()
    }

    fun playPreview(profile: AlarmProfile) {
        stopPreview()
        try {
            val player = MediaPlayer()
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            player.setAudioAttributes(audioAttributes)

            val uri = resolveUri(profile)
            if (uri != null) {
                player.setDataSource(context, uri)
            } else {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                player.setDataSource(context, fallbackUri)
            }

            val volume = profile.volume.coerceIn(0.01f, 1.0f)
            player.setVolume(volume, volume)
            player.isLooping = false
            player.setOnCompletionListener {
                stopPreview()
            }
            player.setOnErrorListener { _, _, _ ->
                stopPreview()
                false
            }
            player.prepare()
            player.start()
            previewPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing preview", e)
            stopPreview()
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping preview", e)
        } finally {
            previewPlayer = null
        }
    }

    fun isPreviewPlaying(): Boolean {
        return try {
            previewPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    private fun resolveUri(profile: AlarmProfile): Uri? {
        return when (profile.soundType) {
            AlarmSoundType.BUILT_IN_RADAR -> {
                val file = SoundGenerator.getSoundFile(context, "radar_alarm.wav")
                Uri.fromFile(file)
            }
            AlarmSoundType.BUILT_IN_CHIME -> {
                val file = SoundGenerator.getSoundFile(context, "chime_alarm.wav")
                Uri.fromFile(file)
            }
            AlarmSoundType.BUILT_IN_SIREN -> {
                val file = SoundGenerator.getSoundFile(context, "siren_alarm.wav")
                Uri.fromFile(file)
            }
            AlarmSoundType.SYSTEM_ALARM -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            AlarmSoundType.SYSTEM_NOTIFICATION -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            AlarmSoundType.CUSTOM -> {
                if (!profile.customFilePath.isNullOrEmpty()) {
                    val file = File(profile.customFilePath)
                    if (file.exists() && file.length() > 0) {
                        Uri.fromFile(file)
                    } else if (!profile.customUriString.isNullOrEmpty()) {
                        Uri.parse(profile.customUriString)
                    } else {
                        // fallback to built-in
                        Uri.fromFile(SoundGenerator.getSoundFile(context, "radar_alarm.wav"))
                    }
                } else if (!profile.customUriString.isNullOrEmpty()) {
                    Uri.parse(profile.customUriString)
                } else {
                    Uri.fromFile(SoundGenerator.getSoundFile(context, "radar_alarm.wav"))
                }
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                val pattern = longArrayOf(0, 500, 300, 500, 300, 800, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, 0)
                    v.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }

    private fun fallbackSystemTone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed fallback system ringtone", e)
        }
    }
}
