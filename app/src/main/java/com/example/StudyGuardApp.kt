package com.example

import android.app.Application
import com.example.data.audio.SoundGenerator
import com.example.service.NotificationHelper
import com.example.service.StudyGuardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyGuardApp : Application() {
    lateinit var manager: StudyGuardManager
        private set

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        NotificationHelper.createNotificationChannels(this)
        manager = StudyGuardManager(this)

        // Ensure built-in audio files are generated offline in the background
        applicationScope.launch {
            SoundGenerator.ensureBuiltInSounds(this@StudyGuardApp)
        }
    }

    companion object {
        lateinit var instance: StudyGuardApp
            private set
    }
}
