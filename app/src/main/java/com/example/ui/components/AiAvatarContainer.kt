package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.MyraaState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ErrorRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AiAvatarContainer(
    state: MyraaState,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var pulsePhase by remember { mutableFloatStateOf(0f) }
    
    // Dynamically adjust animation speed based on the current state
    val targetSpeed = when (state) {
        MyraaState.IDLE -> 0.5f
        MyraaState.WAKE_DETECTED -> 1.5f
        MyraaState.LISTENING -> 2.0f
        MyraaState.PROCESSING -> 3.0f
        MyraaState.EXECUTING -> 2.5f
        MyraaState.SPEAKING -> 1.5f
        MyraaState.ERROR -> 0.2f
    }
    
    val currentSpeed by animateFloatAsState(
        targetValue = targetSpeed,
        animationSpec = tween(500),
        label = "speed_transition"
    )

    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (true) {
            val currentTime = withFrameNanos { it }
            val deltaMs = (currentTime - lastTime) / 1_000_000f
            lastTime = currentTime
            
            rotationAngle = (rotationAngle + (deltaMs * 0.09f * currentSpeed)) % 360f
            pulsePhase = (pulsePhase + (deltaMs * 0.003f * currentSpeed)) % (2 * Math.PI.toFloat())
        }
    }

    val pulse = 1f + (sin(pulsePhase.toDouble()).toFloat() * 0.2f)

    // Dynamically adjust glow color based on state
    val targetColor = when (state) {
        MyraaState.IDLE -> ElectricCyan
        MyraaState.LISTENING -> Color.White
        MyraaState.PROCESSING -> DeepViolet
        MyraaState.EXECUTING -> NeonMagenta
        MyraaState.SPEAKING -> ElectricCyan
        MyraaState.ERROR -> ErrorRed
        MyraaState.WAKE_DETECTED -> ElectricCyan
    }

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width / 2) * 0.6f * pulse
            
            // Outer Core Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(targetColor.copy(alpha = 0.5f), Color.Transparent),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )
            
            // Inner Core Body
            drawCircle(
                color = targetColor,
                radius = radius * 0.4f,
                center = center
            )

            // Dynamic Rotating Rings
            drawArc(
                color = targetColor.copy(alpha = 0.8f),
                startAngle = rotationAngle,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
            drawArc(
                color = DeepViolet.copy(alpha = 0.6f),
                startAngle = -rotationAngle * 1.5f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Dynamic Waveform effect spikes when speaking or listening
            if (state == MyraaState.SPEAKING || state == MyraaState.LISTENING) {
                for (i in 0 until 8) {
                    val angle = (i * 45f + rotationAngle) * (Math.PI / 180f)
                    val x1 = center.x + cos(angle).toFloat() * radius * 0.5f
                    val y1 = center.y + sin(angle).toFloat() * radius * 0.5f
                    val waveMultiplier = if (state == MyraaState.SPEAKING) pulse * 1.5f else pulse * 1.2f
                    val x2 = center.x + cos(angle).toFloat() * radius * waveMultiplier
                    val y2 = center.y + sin(angle).toFloat() * radius * waveMultiplier
                    
                    drawLine(
                        color = targetColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }
    }
}
