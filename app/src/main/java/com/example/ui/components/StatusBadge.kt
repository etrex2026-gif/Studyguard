package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SleepMonitoringState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusBadge(
    sleepState: SleepMonitoringState,
    isScreenOn: Boolean,
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val (label, dotColor, isPulsing) = when (sleepState) {
        SleepMonitoringState.STOPPED -> {
            Triple("Monitoring Stopped", TextMuted, false)
        }
        SleepMonitoringState.MONITORING_SCREEN_ON -> {
            Triple("Screen On — Waiting", EmeraldPrimary, false)
        }
        SleepMonitoringState.SCREEN_OFF_COUNTDOWN -> {
            val min = remainingSeconds / 60
            val sec = remainingSeconds % 60
            val time = String.format("%02d:%02d", min, sec)
            Triple("Screen Off — Countdown: $time", AmberWarning, true)
        }
        SleepMonitoringState.ALARM_RINGING -> {
            Triple("Wake-up Alarm Active!", CoralRed, true)
        }
        SleepMonitoringState.SNOOZED -> {
            val min = remainingSeconds / 60
            val sec = remainingSeconds % 60
            Triple("Snoozed (${min}m ${sec}s)", TechBlue, true)
        }
        SleepMonitoringState.PERMISSION_BLOCKED -> {
            Triple("Permission Required", AmberWarning, false)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = NavyCard, shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = dotColor.copy(alpha = 0.35f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(if (isPulsing) pulseScale else 1f)
                .background(color = dotColor, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
