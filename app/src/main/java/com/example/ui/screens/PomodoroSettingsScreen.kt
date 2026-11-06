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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.PomodoroConfig
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TechBlueLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val manager = StudyGuardApp.instance.manager
    val prefs = manager.prefs
    val scope = rememberCoroutineScope()

    val pomoConfig by prefs.pomodoroConfig.collectAsState(initial = PomodoroConfig())

    var focusSliderValue by remember(pomoConfig.focusDurationMinutes) {
        mutableFloatStateOf(pomoConfig.focusDurationMinutes.toFloat())
    }
    var breakSliderValue by remember(pomoConfig.breakDurationMinutes) {
        mutableFloatStateOf(pomoConfig.breakDurationMinutes.toFloat())
    }

    val presets = listOf(
        Triple("Classic", 25, 5),
        Triple("Deep Focus", 50, 10),
        Triple("Extended", 90, 20)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("pomodoro_settings_back")) {
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
            // Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
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
                                .background(TechBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = TechBlue)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Pomodoro Cycle Plan", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "${pomoConfig.focusDurationMinutes} min Focus • ${pomoConfig.breakDurationMinutes} min Break",
                                color = TechBlueLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Presets
            item {
                Text("Preset Profiles", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.forEach { (name, fM, bM) ->
                        val isSelected = pomoConfig.selectedPresetName == name &&
                                pomoConfig.focusDurationMinutes == fM &&
                                pomoConfig.breakDurationMinutes == bM

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) TechBlue.copy(alpha = 0.2f) else NavyCard,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) TechBlue else NavyCardBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    focusSliderValue = fM.toFloat()
                                    breakSliderValue = bM.toFloat()
                                    scope.launch {
                                        prefs.updatePomodoroConfig(
                                            PomodoroConfig(
                                                focusDurationMinutes = fM,
                                                breakDurationMinutes = bM,
                                                selectedPresetName = name
                                            )
                                        )
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("$fM min focus + $bM min break", color = TextMuted, fontSize = 13.sp)
                                }
                                if (isSelected) {
                                    Text("Active", color = TechBlueLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Custom Sliders
            item {
                Text("Customize Durations", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Focus Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Focus Duration", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${focusSliderValue.roundToInt()} min", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Slider(
                            value = focusSliderValue,
                            onValueChange = { focusSliderValue = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    prefs.updatePomodoroConfig(
                                        pomoConfig.copy(
                                            focusDurationMinutes = focusSliderValue.roundToInt(),
                                            selectedPresetName = "Custom"
                                        )
                                    )
                                }
                            },
                            valueRange = 5f..120f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldPrimary,
                                activeTrackColor = EmeraldPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("focus_duration_slider")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Break Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Break Duration", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${breakSliderValue.roundToInt()} min", color = TechBlueLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Slider(
                            value = breakSliderValue,
                            onValueChange = { breakSliderValue = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    prefs.updatePomodoroConfig(
                                        pomoConfig.copy(
                                            breakDurationMinutes = breakSliderValue.roundToInt(),
                                            selectedPresetName = "Custom"
                                        )
                                    )
                                }
                            },
                            valueRange = 1f..60f,
                            steps = 58,
                            colors = SliderDefaults.colors(
                                thumbColor = TechBlue,
                                activeTrackColor = TechBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("break_duration_slider")
                        )
                    }
                }
            }

            // Behavioral explanation
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Study Guard Session Transition Rules", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Focus Finish: Triggers focus completion alarm, and automatically starts the break rest countdown.\n" +
                                        "2. Break Finish: Triggers break completion alarm, and waits in 'Ready' state.\n" +
                                        "3. The next focus period will NEVER automatically start until you press 'START NEXT SESSION'.",
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
