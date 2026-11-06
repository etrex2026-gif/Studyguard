package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PomodoroState
import com.example.data.model.SleepMonitoringState
import com.example.service.StudyGuardManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `verify app name resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Study Guard", appName)
  }

  @Test
  fun `verify initial state of StudyGuardManager`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = StudyGuardManager(context)

    assertEquals(SleepMonitoringState.STOPPED, manager.sleepState.value)
    assertEquals(PomodoroState.READY, manager.pomodoroState.value)
  }

  @Test
  fun `verify pomodoro transition from focus complete to break running`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = StudyGuardManager(context)

    // Complete focus
    manager.handleFocusFinished()
    assertEquals(PomodoroState.BREAK_RUNNING, manager.pomodoroState.value)

    // Complete break
    manager.handleBreakFinished()
    assertEquals(PomodoroState.READY_FOR_NEXT_FOCUS, manager.pomodoroState.value)
  }
}
