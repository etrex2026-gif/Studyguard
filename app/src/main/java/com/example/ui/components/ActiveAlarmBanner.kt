package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmType
import com.example.ui.theme.CoralRed
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TextPrimary

@Composable
fun ActiveAlarmBanner(
    activeAlarms: Set<AlarmType>,
    onDismissSleepAlarm: () -> Unit,
    onSnoozeSleepAlarm: () -> Unit,
    onDismissPomodoroAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeAlarms.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFF3B151E), shape = RoundedCornerShape(16.dp))
                .border(width = 1.5.dp, color = CoralRed, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("active_alarm_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Active Alarm",
                    tint = CoralRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ALARM SOUNDING",
                        color = CoralRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    val activeNames = activeAlarms.map {
                        when (it) {
                            AlarmType.SLEEP_ALARM -> "Inactivity Wake-Up"
                            AlarmType.POMODORO_FOCUS_COMPLETE -> "Focus Finished"
                            AlarmType.POMODORO_BREAK_COMPLETE -> "Break Finished"
                        }
                    }.joinToString(" & ")

                    Text(
                        text = activeNames,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons tailored to active alarms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeAlarms.contains(AlarmType.SLEEP_ALARM)) {
                    Button(
                        onClick = onDismissSleepAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_sleep_alarm_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Sleep Alarm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSnoozeSleepAlarm,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("snooze_sleep_alarm_button")
                    ) {
                        Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (activeAlarms.contains(AlarmType.POMODORO_FOCUS_COMPLETE) ||
                    activeAlarms.contains(AlarmType.POMODORO_BREAK_COMPLETE)
                ) {
                    Button(
                        onClick = onDismissPomodoroAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dismiss_pomodoro_alarm_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dismiss Timer Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
