package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class ReasoningNode(val id: Int, val concept: String, val weight: Float)
data class AutoSuggestion(val id: Int, val text: String, val confidence: Float)

@Composable
fun ContextReasoningOverlay() {
    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(100f) }
    
    val nodes = remember {
        mutableStateListOf(
            ReasoningNode(1, "Entropy Void", 0.95f),
            ReasoningNode(2, "Spectral Synapsis", 0.88f),
            ReasoningNode(3, "Task Execution Deluxe", 0.76f)
        )
    }
    
    val suggestions = remember {
        mutableStateListOf(
            AutoSuggestion(1, "Investigate heuristic pruning in layer 2...", 0.92f),
            AutoSuggestion(2, "Synchronize ambient audio with visual telemetry.", 0.85f),
            AutoSuggestion(3, "Review unhandled exceptions in browser cache.", 0.67f)
        )
    }

    LaunchedEffect(Unit) {
        while(true) {
            delay(5000)
            if (nodes.isNotEmpty()) {
                val idx = nodes.indices.random()
                val newWeight = 0.5f + kotlin.random.Random.Default.nextFloat() * 0.5f
                nodes[idx] = nodes[idx].copy(weight = newWeight)
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .padding(16.dp)
            .width(if (isExpanded) 320.dp else 64.dp)
    ) {
        if (!isExpanded) {
            FloatingActionButton(
                onClick = { isExpanded = true },
                containerColor = Color(0xFFAA00FF),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.DeviceHub, contentDescription = "Open Reasoning Maps")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFAA00FF).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF00FFCC))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AutoSuggest & Reasoning", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reasoning Map Nodes", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    nodes.forEach { node ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (node.weight > 0.8f) Color(0xFF00FFCC) else Color(0xFFFFB300))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(node.concept, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("${(node.weight * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Agent Advice", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    suggestions.forEach { suggestion ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFAA00FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(suggestion.text, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
