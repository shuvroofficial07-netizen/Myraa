package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DiagnosticsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        Text("SYSTEM HEALTH", color = ElectricCyan, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        DiagnosticItem("Foreground Service", "READY", SuccessGreen)
        DiagnosticItem("Wake Word Engine", "READY", SuccessGreen)
        DiagnosticItem("Speech Recognition", "READY", SuccessGreen)
        DiagnosticItem("Text To Speech", "READY", SuccessGreen)
        DiagnosticItem("Accessibility", "LIMITED", ErrorRed)
        DiagnosticItem("Overlay Permission", "LIMITED", ErrorRed)
        
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { /* Run checks */ },
            colors = ButtonDefaults.buttonColors(containerColor = DeepViolet),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RUN FULL DIAGNOSTICS", color = TextPrimary)
        }
    }
}

@Composable
fun DiagnosticItem(name: String, status: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = TextPrimary)
        Text(status, color = color)
    }
}
