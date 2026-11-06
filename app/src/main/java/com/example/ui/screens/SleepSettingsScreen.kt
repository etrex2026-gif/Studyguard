package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.StudyGuardApp
import com.example.data.model.AlarmType
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
fun SleepSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlarmSettings: () -> Unit
) {
    val manager = StudyGuardApp.instance.manager
    val prefs = manager.prefs
    val scope = rememberCoroutineScope()

    val thresholdMinutes by prefs.sleepThresholdMinutes.collectAsState(initial = 3)
    val sleepProfile by prefs.sleepAlarmProfile.collectAsState(initial = com.example.data.model.AlarmProfile())

    val presetOptions = listOf(1, 2, 3, 5, 10)
    var isCustomSelected by remember(thresholdMinutes) {
        mutableStateOf(!presetOptions.contains(thresholdMinutes))
    }
    var customSliderValue by remember(thresholdMinutes) {
        mutableFloatStateOf(thresholdMinutes.toFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep Detection Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("sleep_settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        },
        containerColor = DeepNavy
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = EmeraldPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Screen-Off Inactivity Threshold", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Currently: $thresholdMinutes minutes", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Presets grid
            item {
                Text("Select Threshold Preset", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.forEach { minutes ->
                        val isSelected = !isCustomSelected && thresholdMinutes == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) EmeraldPrimary else NavyCard,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) EmeraldPrimary else NavyCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isCustomSelected = false
                                    scope.launch { prefs.setSleepThresholdMinutes(minutes) }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${minutes}m",
                                color = if (isSelected) DeepNavy else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Custom preset button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .background(
                                color = if (isCustomSelected) EmeraldPrimary else NavyCard,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCustomSelected) EmeraldPrimary else NavyCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isCustomSelected = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom",
                            color = if (isCustomSelected) DeepNavy else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Custom slider if selected
            if (isCustomSelected) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Custom Duration (1 – 60 min)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("${customSliderValue.roundToInt()} min", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = customSliderValue,
                                onValueChange = {
                                    customSliderValue = it
                                },
                                onValueChangeFinished = {
                                    scope.launch { prefs.setSleepThresholdMinutes(customSliderValue.roundToInt()) }
                                },
                                valueRange = 1f..60f,
                                steps = 58,
                                colors = SliderDefaults.colors(
                                    thumbColor = EmeraldPrimary,
                                    activeTrackColor = EmeraldPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("sleep_custom_slider")
                            )
                        }
                    }
                }
            }

            // Snooze Duration Setting
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Snooze Duration", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("When snooze is tapped on an active alarm", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        val snoozeOptions = listOf(1, 2, 3, 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            snoozeOptions.forEach { snz ->
                                val isSelected = sleepProfile.snoozeMinutes == snz
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) TechBlue else DeepNavy,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) TechBlue else NavyCardBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            scope.launch {
                                                prefs.updateSleepAlarmProfile(sleepProfile.copy(snoozeMinutes = snz))
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${snz}m",
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Shortcut to sound settings & test alarm
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAlarmSettings,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alarm Sound")
                    }

                    Button(
                        onClick = {
                            manager.audioPlayer.playAlarm(AlarmType.SLEEP_ALARM, sleepProfile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_sleep_alarm_button")
                    ) {
                        Text("Test Alarm", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // How native screen-off monitoring works explanation
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TechBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Native Android Screen-State Detection", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Study Guard monitors Intent.ACTION_SCREEN_OFF and Intent.ACTION_SCREEN_ON via a native foreground service.\n\n" +
                                        "• When display turns OFF: Exact alarm countdown starts.\n" +
                                        "• If display turns ON before threshold: Pending alarm resets immediately without firing.\n" +
                                        "• If display stays OFF past threshold: High-priority wake-up alarm rings.\n" +
                                        "No cameras, microphones, or screen pixel inspection are ever used.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
