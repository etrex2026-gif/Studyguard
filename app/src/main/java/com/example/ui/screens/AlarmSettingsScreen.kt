package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.StudyGuardApp
import com.example.data.model.AlarmProfile
import com.example.data.model.AlarmRepeatBehavior
import com.example.data.model.AlarmSoundType
import com.example.data.model.AlarmType
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val manager = StudyGuardApp.instance.manager
    val prefs = manager.prefs
    val audioPlayer = manager.audioPlayer
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val sleepProfile by prefs.sleepAlarmProfile.collectAsState(initial = AlarmProfile())
    val focusProfile by prefs.focusAlarmProfile.collectAsState(initial = AlarmProfile())
    val breakProfile by prefs.breakAlarmProfile.collectAsState(initial = AlarmProfile())

    val currentProfile = when (selectedTabIndex) {
        0 -> sleepProfile
        1 -> focusProfile
        else -> breakProfile
    }

    val currentAlarmType = when (selectedTabIndex) {
        0 -> AlarmType.SLEEP_ALARM
        1 -> AlarmType.POMODORO_FOCUS_COMPLETE
        else -> AlarmType.POMODORO_BREAK_COMPLETE
    }

    // Stop preview on exit
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stopPreview()
        }
    }

    // SAF Audio Picker Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = prefs.copyCustomAudioToInternal(uri)
                if (result != null) {
                    val (fileName, filePath) = result
                    val updated = currentProfile.copy(
                        soundType = AlarmSoundType.CUSTOM,
                        soundTitle = fileName,
                        customUriString = uri.toString(),
                        customFilePath = filePath
                    )
                    when (selectedTabIndex) {
                        0 -> prefs.updateSleepAlarmProfile(updated)
                        1 -> prefs.updateFocusAlarmProfile(updated)
                        2 -> prefs.updateBreakAlarmProfile(updated)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm & Sound Profiles", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("alarm_settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        },
        containerColor = DeepNavy
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row for 3 Profiles
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DeepNavy,
                contentColor = EmeraldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = if (selectedTabIndex == 0) EmeraldPrimary else TechBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        audioPlayer.stopPreview()
                        selectedTabIndex = 0
                    },
                    text = {
                        Text(
                            "Sleep Alarm",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) TextPrimary else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        audioPlayer.stopPreview()
                        selectedTabIndex = 1
                    },
                    text = {
                        Text(
                            "Focus Finish",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 1) TextPrimary else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        audioPlayer.stopPreview()
                        selectedTabIndex = 2
                    },
                    text = {
                        Text(
                            "Break Finish",
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 2) TextPrimary else TextMuted
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Sound Selection Card
                item {
                    Text("Select Sound", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val builtInSounds = listOf(
                                Pair(AlarmSoundType.BUILT_IN_RADAR, "Radar Pulse (Sharp Digital)"),
                                Pair(AlarmSoundType.BUILT_IN_CHIME, "Gentle Chime (Melodic Bell)"),
                                Pair(AlarmSoundType.BUILT_IN_SIREN, "Wake Siren (Dynamic Alert)"),
                                Pair(AlarmSoundType.SYSTEM_ALARM, "Android System Alarm"),
                                Pair(AlarmSoundType.SYSTEM_NOTIFICATION, "Android System Alert")
                            )

                            builtInSounds.forEach { (type, label) ->
                                val isSelected = currentProfile.soundType == type
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            audioPlayer.stopPreview()
                                            val updated = currentProfile.copy(soundType = type, soundTitle = label)
                                            scope.launch {
                                                when (selectedTabIndex) {
                                                    0 -> prefs.updateSleepAlarmProfile(updated)
                                                    1 -> prefs.updateFocusAlarmProfile(updated)
                                                    2 -> prefs.updateBreakAlarmProfile(updated)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Custom Audio Button
                            val isCustom = currentProfile.soundType == AlarmSoundType.CUSTOM
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isCustom) TechBlue.copy(alpha = 0.2f) else DeepNavy,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isCustom) 1.5.dp else 1.dp,
                                        color = if (isCustom) TechBlue else NavyCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        audioPickerLauncher.launch("audio/*")
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TechBlue, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isCustom) "Custom: ${currentProfile.soundTitle}" else "SELECT CUSTOM AUDIO",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text("Import from device offline storage", color = TextMuted, fontSize = 11.sp)
                                    }
                                    if (isCustom) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = TechBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Preview Button
                            var isPreviewing by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (isPreviewing) {
                                        audioPlayer.stopPreview()
                                        isPreviewing = false
                                    } else {
                                        audioPlayer.playPreview(currentProfile)
                                        isPreviewing = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPreviewing) CoralRed else TechBlue
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preview_sound_button")
                            ) {
                                Icon(
                                    imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isPreviewing) "Stop Preview" else "Preview Sound", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Independent Volume Slider
                item {
                    Text("Independent Alarm Volume", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    var volumeSlider by remember(currentProfile.volume) {
                        mutableFloatStateOf(currentProfile.volume)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Playback Gain", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Text("${(volumeSlider * 100).roundToInt()}%", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = volumeSlider,
                                onValueChange = { volumeSlider = it },
                                onValueChangeFinished = {
                                    val updated = currentProfile.copy(volume = volumeSlider)
                                    scope.launch {
                                        when (selectedTabIndex) {
                                            0 -> prefs.updateSleepAlarmProfile(updated)
                                            1 -> prefs.updateFocusAlarmProfile(updated)
                                            2 -> prefs.updateBreakAlarmProfile(updated)
                                        }
                                    }
                                },
                                valueRange = 0.05f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = EmeraldPrimary,
                                    activeTrackColor = EmeraldPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("alarm_volume_slider")
                            )
                            Text(
                                "Modifying this profile's volume will not affect the other alarm profiles.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Playback Behavior
                item {
                    Text("Alarm Repetition Behavior", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val behaviors = listOf(
                                Pair(AlarmRepeatBehavior.CONTINUOUS, "Repeat continuously until stopped"),
                                Pair(AlarmRepeatBehavior.REPEAT_3_TIMES, "Repeat 3 times"),
                                Pair(AlarmRepeatBehavior.PLAY_ONCE, "Play once"),
                                Pair(AlarmRepeatBehavior.AUTO_STOP_1_MIN, "Auto-stop after 1 minute"),
                                Pair(AlarmRepeatBehavior.AUTO_STOP_2_MIN, "Auto-stop after 2 minutes")
                            )

                            behaviors.forEach { (b, label) ->
                                val isSelected = currentProfile.repeatBehavior == b
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isSelected) TechBlue.copy(alpha = 0.2f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            val updated = currentProfile.copy(repeatBehavior = b)
                                            scope.launch {
                                                when (selectedTabIndex) {
                                                    0 -> prefs.updateSleepAlarmProfile(updated)
                                                    1 -> prefs.updateFocusAlarmProfile(updated)
                                                    2 -> prefs.updateBreakAlarmProfile(updated)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = TechBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Vibration Toggle
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Device Vibration", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Vibrate tablet during alarm", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = currentProfile.vibrate,
                                onCheckedChange = { checked ->
                                    val updated = currentProfile.copy(vibrate = checked)
                                    scope.launch {
                                        when (selectedTabIndex) {
                                            0 -> prefs.updateSleepAlarmProfile(updated)
                                            1 -> prefs.updateFocusAlarmProfile(updated)
                                            2 -> prefs.updateBreakAlarmProfile(updated)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DeepNavy,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DeepNavy
                                ),
                                modifier = Modifier.testTag("vibration_switch")
                            )
                        }
                    }
                }

                // Test Alarm Button
                item {
                    Button(
                        onClick = {
                            audioPlayer.playAlarm(currentAlarmType, currentProfile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("test_full_alarm_button")
                    ) {
                        Text("TEST FULL ALARM & VIBRATION", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
