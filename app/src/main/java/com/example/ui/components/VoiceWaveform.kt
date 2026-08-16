package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.MyraaState
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    state: MyraaState,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val targetColor = when(state) {
        MyraaState.LISTENING -> Color.White
        MyraaState.SPEAKING -> ElectricCyan
        MyraaState.PROCESSING -> DeepViolet
        MyraaState.WAKE_DETECTED -> ElectricCyan
        else -> TextSecondary.copy(alpha = 0.3f)
    }

    val animatedLevel by animateFloatAsState(
        targetValue = audioLevel,
        animationSpec = tween(50),
        label = "audioLevel"
    )

    Canvas(modifier = modifier.width(120.dp).height(60.dp)) {
        val barCount = 7
        val barWidth = 8.dp.toPx()
        val gap = 6.dp.toPx()
        val totalWidth = (barCount * barWidth) + ((barCount - 1) * gap)
        val startX = (size.width - totalWidth) / 2f
        val centerY = size.height / 2f

        val isAnimating = state == MyraaState.LISTENING || state == MyraaState.SPEAKING
        val isProcessing = state == MyraaState.PROCESSING

        for (i in 0 until barCount) {
            val normalizedLevel = (animatedLevel.coerceIn(0f, 10f) / 10f)
            
            val barHeight = if (isAnimating) {
                val mathOffset = sin(phase + (i * 0.8f))
                val heightFactor = (normalizedLevel * 0.7f) + (mathOffset * 0.3f)
                10f + (heightFactor.coerceAtLeast(0.1f) * (size.height - 10f))
            } else if (isProcessing) {
                val mathOffset = sin((phase * 3f) + (i * 1.5f))
                20f + (mathOffset * 10f)
            } else {
                6f
            }

            val x = startX + (i * (barWidth + gap))
            val y = centerY - (barHeight / 2f)

            drawRoundRect(
                color = targetColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
