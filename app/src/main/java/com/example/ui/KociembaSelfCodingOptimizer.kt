package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun KociembaSelfCodingOptimizer() {
    var generations by remember { mutableIntStateOf(0) }
    var efficiencyScore by remember { mutableFloatStateOf(0.1f) }
    var running by remember { mutableStateOf(true) }
    val maxGenerations = 1000

    val codeSnippets = remember { mutableStateListOf<String>() }

    LaunchedEffect(running) {
        if (running) {
            while (generations < maxGenerations) {
                delay((50..200).random().toLong())
                generations++
                
                // Simulate heuristic Kociemba non-layer-by-layer optimization improvement
                efficiencyScore += (1f - efficiencyScore) * 0.05f 

                val turn = listOf("U", "U'", "U2", "D", "D'", "D2", "R", "R'", "R2", "L", "L'", "L2", "F", "F'", "F2", "B", "B'", "B2").random()
                val operation = listOf(
                    "Heuristic A* branch pruning...",
                    "G1 state mapping: $turn",
                    "Interexchanging logic core...",
                    "Phase 2 deep search...",
                    "Solving cross-dimension paths: $turn",
                    "Unfalsifiable state validated."
                ).random()
                
                if (codeSnippets.size > 8) {
                    codeSnippets.removeAt(0)
                }
                codeSnippets.add("[GEN $generations] $operation Efficiency: ${(efficiencyScore * 100).toInt()}%")
                
                if (efficiencyScore > 0.999f) {
                    running = false
                    codeSnippets.add("[SUCCESS] Code has met its most usable, efficient, and unfalsifiable outcome of operations.")
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, com.example.BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = "Optimize",
                        tint = Color(0xFF00FFCC)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELF-CODING KOCIEMBA STATE OPTIMIZER",
                        color = com.example.TextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = if (running) "OPTIMIZING" else "UNFALSIFIABLE",
                    color = if (running) Color(0xFFFFB300) else Color(0xFF00FFCC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (running) Color(0xFFFFB300).copy(alpha = 0.2f) else Color(0xFF00FFCC).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Evolution Generations", color = Color.Gray, fontSize = 10.sp)
                    Text(text = "$generations / α", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Heuristic Efficiency", color = Color.Gray, fontSize = 10.sp)
                    Text(text = "${(efficiencyScore * 100).toInt()}%", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Code Output Log
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF05080F))
                    .border(1.dp, Color(0xFF1A263B), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    codeSnippets.forEach { snippet ->
                        Text(
                            text = snippet,
                            color = if (snippet.contains("SUCCESS")) Color(0xFF00FF00) else Color(0xFF8E9EB8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
