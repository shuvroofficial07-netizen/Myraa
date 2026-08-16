package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DeviceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        Text("DEVICE CONTROL", color = ElectricCyan, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DeviceCard("Wi-Fi", Icons.Default.Wifi, true)
            DeviceCard("Bluetooth", Icons.Default.Bluetooth, false)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DeviceCard("Battery", Icons.Default.BatteryFull, true)
            DeviceCard("Flashlight", Icons.Default.FlashlightOn, false)
        }
    }
}

@Composable
fun RowScope.DeviceCard(name: String, icon: ImageVector, active: Boolean) {
    Surface(
        color = GlassWhite,
        modifier = Modifier.weight(1f).padding(4.dp).height(100.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = name, tint = if (active) ElectricCyan else TextSecondary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, color = TextPrimary)
        }
    }
}
