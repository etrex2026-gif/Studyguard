package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.StudyGuardApp
import com.example.data.model.AlarmType
import com.example.data.model.PomodoroConfig
import com.example.data.model.PomodoroState
import com.example.data.model.SleepMonitoringState
import com.example.ui.components.ActiveAlarmBanner
import com.example.ui.components.CircularProgressCountdown
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TechBlueLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToSleepSettings: () -> Unit,
    onNavigateToPomodoroSettings: () -> Unit,
    onNavigateToAlarmSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manager = StudyGuardApp.instance.manager
    val prefs = manager.prefs

    val sleepState by manager.sleepState.collectAsState()
    val isScreenOn by manager.isScreenCurrentlyOn.collectAsState()
    val screenOffRemainingSeconds by manager.screenOffRemainingSeconds.collectAsState()
    val sleepThresholdMinutes by prefs.sleepThresholdMinutes.collectAsState(initial = 3)
    val sleepProfile by prefs.sleepAlarmProfile.collectAsState(initial = com.example.data.model.AlarmProfile())

    val pomoState by manager.pomodoroState.collectAsState()
    val pomoRemainingMillis by manager.pomodoroRemainingMillis.collectAsState()
    val pomoTotalMillis by manager.pomodoroTotalMillis.collectAsState()
    val pomoConfig by prefs.pomodoroConfig.collectAsState(initial = PomodoroConfig())

    val activeAlarms by manager.activeAlarms.collectAsState()

    var showSettingsMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        val isTabletLandscape = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTabletLandscape) 28.dp else 16.dp, vertical = 12.dp)
        ) {
            // TOP BAR
            TopBarSection(
                onOpenSettingsMenu = { showSettingsMenu = true },
                onNavigateToPermissions = onNavigateToPermissions
            )

            // Settings dropdown menu
            DropdownMenu(
                expanded = showSettingsMenu,
                onDismissRequest = { showSettingsMenu = false },
                modifier = Modifier.background(NavyCard)
            ) {
                DropdownMenuItem(
                    text = { Text("Sleep Detection Settings", color = TextPrimary) },
                    onClick = {
                        showSettingsMenu = false
                        onNavigateToSleepSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Pomodoro Timer Settings", color = TextPrimary) },
                    onClick = {
                        showSettingsMenu = false
                        onNavigateToPomodoroSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alarm Sounds & Volume", color = TextPrimary) },
                    onClick = {
                        showSettingsMenu = false
                        onNavigateToAlarmSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Permissions & Background Reliability", color = TextPrimary) },
                    onClick = {
                        showSettingsMenu = false
                        onNavigateToPermissions()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sticky Active Alarm Banner if any alarm is sounding
            ActiveAlarmBanner(
                activeAlarms = activeAlarms,
                onDismissSleepAlarm = { manager.dismissSleepAlarm() },
                onSnoozeSleepAlarm = { manager.snoozeSleepAlarm() },
                onDismissPomodoroAlarm = { manager.dismissPomodoroAlarm() },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // MAIN CONTENT (Adaptive Two-Column on Tablets, Single Column on Phones)
            if (isTabletLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Sleep Detection
                    Box(modifier = Modifier.weight(1f)) {
                        SleepDetectionCard(
                            sleepState = sleepState,
                            isScreenOn = isScreenOn,
                            thresholdMinutes = sleepThresholdMinutes,
                            remainingSeconds = screenOffRemainingSeconds,
                            onStartMonitoring = { manager.startSleepMonitoring() },
                            onStopMonitoring = { manager.stopSleepMonitoring() },
                            onTestAlarm = { manager.audioPlayer.playAlarm(AlarmType.SLEEP_ALARM, sleepProfile) },
                            onOpenSettings = onNavigateToSleepSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Right Column: Pomodoro Timer
                    Box(modifier = Modifier.weight(1.2f)) {
                        PomodoroCard(
                            pomoState = pomoState,
                            remainingMillis = pomoRemainingMillis,
                            totalMillis = pomoTotalMillis,
                            pomoConfig = pomoConfig,
                            onStartFocus = { manager.startPomodoroFocus() },
                            onPause = { manager.pausePomodoro() },
                            onResume = { manager.resumePomodoro() },
                            onStop = { manager.stopPomodoro() },
                            onSkipBreak = { manager.skipBreak() },
                            onStartNextSession = { manager.startPomodoroFocus() },
                            onOpenSettings = onNavigateToPomodoroSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Phone vertical scroll layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SleepDetectionCard(
                        sleepState = sleepState,
                        isScreenOn = isScreenOn,
                        thresholdMinutes = sleepThresholdMinutes,
                        remainingSeconds = screenOffRemainingSeconds,
                        onStartMonitoring = { manager.startSleepMonitoring() },
                        onStopMonitoring = { manager.stopSleepMonitoring() },
                        onTestAlarm = { manager.audioPlayer.playAlarm(AlarmType.SLEEP_ALARM, sleepProfile) },
                        onOpenSettings = onNavigateToSleepSettings,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PomodoroCard(
                        pomoState = pomoState,
                        remainingMillis = pomoRemainingMillis,
                        totalMillis = pomoTotalMillis,
                        pomoConfig = pomoConfig,
                        onStartFocus = { manager.startPomodoroFocus() },
                        onPause = { manager.pausePomodoro() },
                        onResume = { manager.resumePomodoro() },
                        onStop = { manager.stopPomodoro() },
                        onSkipBreak = { manager.skipBreak() },
                        onStartNextSession = { manager.startPomodoroFocus() },
                        onOpenSettings = onNavigateToPomodoroSettings,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TopBarSection(
    onOpenSettingsMenu: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = NavyCard, shape = CircleShape)
                    .border(width = 1.5.dp, color = EmeraldPrimary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = "Logo",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "STUDY GUARD",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "STAY AWAKE. STAY FOCUSED.",
                    color = EmeraldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Offline Badge
            Box(
                modifier = Modifier
                    .background(color = NavyCard, shape = RoundedCornerShape(20.dp))
                    .border(width = 1.dp, color = NavyCardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline Mode",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "100% Offline",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Permissions / Reliability Shortcut
            IconButton(
                onClick = onNavigateToPermissions,
                modifier = Modifier.testTag("permissions_quick_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Background Reliability",
                    tint = TextPrimary
                )
            }

            // Settings Menu Icon
            IconButton(
                onClick = onOpenSettingsMenu,
                modifier = Modifier.testTag("settings_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary
                )
            }
        }
    }
}

// ==========================================
// PRIMARY CARD 1: SLEEP DETECTION
// ==========================================

@Composable
fun SleepDetectionCard(
    sleepState: SleepMonitoringState,
    isScreenOn: Boolean,
    thresholdMinutes: Int,
    remainingSeconds: Int,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onTestAlarm: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMonitoring = (sleepState != SleepMonitoringState.STOPPED)

    Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isMonitoring) EmeraldPrimary.copy(alpha = 0.5f) else NavyCardBorder)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.testTag("sleep_detection_card")
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header with Title & Settings shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SLEEP DETECTION", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Screen-Off Inactivity Watcher", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("sleep_settings_shortcut")) {
                        Icon(Icons.Default.Tune, contentDescription = "Sleep Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Badge
                StatusBadge(
                    sleepState = sleepState,
                    isScreenOn = isScreenOn,
                    remainingSeconds = remainingSeconds
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Threshold & Display State Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavy.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Off Threshold", color = TextMuted, fontSize = 11.sp)
                        Text("$thresholdMinutes min", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Current Display", color = TextMuted, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isScreenOn) EmeraldPrimary else AmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isScreenOn) "Display On" else "Display Dark",
                                color = if (isScreenOn) EmeraldPrimary else AmberWarning,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Loud alarm triggers automatically if tablet screen remains off beyond $thresholdMinutes minutes while studying.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isMonitoring) {
                    Button(
                        onClick = onStartMonitoring,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_monitoring_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START MONITORING", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                } else {
                    Button(
                        onClick = onStopMonitoring,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = TextPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("stop_monitoring_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP MONITORING", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                }

                OutlinedButton(
                    onClick = onTestAlarm,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_alarm_quick_button")
                ) {
                    Text("Test Wake-Up Alarm Sound", fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================================
// PRIMARY CARD 2: POMODORO TIMER
// ==========================================

@Composable
fun PomodoroCard(
    pomoState: PomodoroState,
    remainingMillis: Long,
    totalMillis: Long,
    pomoConfig: PomodoroConfig,
    onStartFocus: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkipBreak: () -> Unit,
    onStartNextSession: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.testTag("pomodoro_card")
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("POMODORO TIMER", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "${pomoConfig.focusDurationMinutes}m Focus / ${pomoConfig.breakDurationMinutes}m Break",
                            color = TechBlueLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("pomodoro_settings_shortcut")) {
                        Icon(Icons.Default.Tune, contentDescription = "Pomodoro Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Circular Progress Countdown
                CircularProgressCountdown(
                    state = pomoState,
                    remainingMillis = remainingMillis,
                    totalMillis = totalMillis,
                    diameter = 220.dp,
                    strokeWidth = 10.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contextual Controls depending on Pomodoro State
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (pomoState) {
                    PomodoroState.READY -> {
                        Button(
                            onClick = onStartFocus,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_pomodoro_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START FOCUS SESSION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    PomodoroState.FOCUS_RUNNING, PomodoroState.BREAK_RUNNING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onPause,
                                colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_pomodoro_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PAUSE", fontWeight = FontWeight.Bold)
                            }

                            if (pomoState == PomodoroState.BREAK_RUNNING) {
                                OutlinedButton(
                                    onClick = onSkipBreak,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(48.dp)
                                        .testTag("skip_break_button")
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Skip Break", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = onStop,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(48.dp)
                                    .testTag("stop_pomodoro_button")
                            ) {
                                Text("STOP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    PomodoroState.FOCUS_PAUSED, PomodoroState.BREAK_PAUSED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onResume,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("resume_pomodoro_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RESUME", fontWeight = FontWeight.Bold)
                            }

                            if (pomoState == PomodoroState.BREAK_PAUSED) {
                                OutlinedButton(
                                    onClick = onSkipBreak,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(48.dp)
                                        .testTag("skip_break_paused_button")
                                ) {
                                    Text("Skip Break", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = onStop,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(48.dp)
                                    .testTag("reset_pomodoro_button")
                            ) {
                                Text("RESET", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    PomodoroState.READY_FOR_NEXT_FOCUS -> {
                        Button(
                            onClick = onStartNextSession,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_next_session_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START NEXT SESSION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Timer Durations", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
