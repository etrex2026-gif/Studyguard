package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PomodoroState
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TechBlueLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CircularProgressCountdown(
    state: PomodoroState,
    remainingMillis: Long,
    totalMillis: Long,
    modifier: Modifier = Modifier,
    diameter: Dp = 230.dp,
    strokeWidth: Dp = 10.dp
) {
    val progress = if (totalMillis > 0) {
        (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "pomodoro_progress"
    )

    val isFocus = (state == PomodoroState.FOCUS_RUNNING || state == PomodoroState.FOCUS_PAUSED)
    val isBreak = (state == PomodoroState.BREAK_RUNNING || state == PomodoroState.BREAK_PAUSED)

    val activeGradient = if (isFocus) {
        listOf(EmeraldPrimary, EmeraldLight)
    } else if (isBreak) {
        listOf(TechBlue, TechBlueLight)
    } else {
        listOf(EmeraldPrimary, TechBlue)
    }

    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .size(diameter)
            .testTag("circular_progress_countdown"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2
            val strokePx = strokeWidth.toPx()

            // Background circle track
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                style = Stroke(width = strokePx)
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(activeGradient),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(0f, 0f),
                    size = Size(canvasSize, canvasSize),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Central countdown and labels
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val phaseLabel = when (state) {
                PomodoroState.FOCUS_RUNNING -> "FOCUS"
                PomodoroState.FOCUS_PAUSED -> "FOCUS (PAUSED)"
                PomodoroState.BREAK_RUNNING -> "BREAK"
                PomodoroState.BREAK_PAUSED -> "BREAK (PAUSED)"
                PomodoroState.READY_FOR_NEXT_FOCUS -> "BREAK COMPLETED"
                PomodoroState.READY -> "READY"
            }

            val phaseColor = when {
                isFocus -> EmeraldLight
                isBreak -> TechBlueLight
                state == PomodoroState.READY_FOR_NEXT_FOCUS -> EmeraldPrimary
                else -> TextSecondary
            }

            Text(
                text = phaseLabel,
                color = phaseColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeFormatted,
                color = TextPrimary,
                fontSize = if (timeFormatted.length > 5) 36.sp else 46.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            val subtitleText = when (state) {
                PomodoroState.FOCUS_RUNNING, PomodoroState.FOCUS_PAUSED -> {
                    "${(totalMillis / 60000)} min study session"
                }
                PomodoroState.BREAK_RUNNING, PomodoroState.BREAK_PAUSED -> {
                    "${(totalMillis / 60000)} min rest period"
                }
                PomodoroState.READY_FOR_NEXT_FOCUS -> {
                    "Ready for next focus"
                }
                PomodoroState.READY -> {
                    "Tap Start to begin"
                }
            }

            Text(
                text = subtitleText,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
