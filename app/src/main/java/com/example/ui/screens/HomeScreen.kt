package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MyraaState
import com.example.MyraaStateManager
import com.example.ui.components.AiAvatarContainer
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    onMicClick: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val myraaState by MyraaStateManager.myraaState.collectAsState()
    val emotion by MyraaStateManager.emotion.collectAsState()
    val spokenText by MyraaStateManager.spokenText.collectAsState()
    val isServiceRunning by MyraaStateManager.isServiceRunning.collectAsState()
    val audioLevel by MyraaStateManager.audioLevel.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MYRAA",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) SuccessGreen else ErrorRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isServiceRunning) "READY" else "OFFLINE",
                    color = if (isServiceRunning) SuccessGreen else ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Avatar Core
        AiAvatarContainer(state = myraaState, modifier = Modifier.size(280.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // Greeting
        Text(
            text = "Good evening, User",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${emotion.name} • ${myraaState.name}",
            color = ElectricCyan,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        if (spokenText.isNotEmpty()) {
            Text(
                text = "\"$spokenText\"",
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        VoiceWaveform(
            state = myraaState,
            audioLevel = audioLevel,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Mic Button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Talk to MYRAA",
                tint = if (myraaState == MyraaState.LISTENING) ElectricCyan else TextPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton("MEMORY") { onNavigate("memory") }
            QuickActionButton("DEVICE") { onNavigate("device") }
            QuickActionButton("DIAGNOSTICS") { onNavigate("diagnostics") }
        }
    }
}

@Composable
fun QuickActionButton(title: String, onClick: () -> Unit) {
    Surface(
        color = GlassWhite,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .clickable { onClick() }
            .height(48.dp)
            .padding(horizontal = 4.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
