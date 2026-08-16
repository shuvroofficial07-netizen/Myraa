package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.TextPrimary

@Composable
fun MemoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        Text("MY MEMORY", color = ElectricCyan, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        val dummyMemories = listOf(
            "USER_NAME" to "Shuvro",
            "FAVORITE_COLOR" to "Electric Cyan",
            "ALIAS_YOUTUBE" to "ভিডিও"
        )
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dummyMemories) { memory ->
                Surface(color = GlassWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(memory.first, color = ElectricCyan, fontSize = 12.sp)
                        Text(memory.second, color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
