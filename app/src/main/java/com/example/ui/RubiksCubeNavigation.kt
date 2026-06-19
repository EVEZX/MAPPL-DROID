package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CubeFace(val title: String, val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector, val id: String) {
    FRONT("OpenClaw", Color(0xFFE53935), Icons.Filled.Dashboard, "OpenClaw"),
    BACK("Telemetry", Color(0xFF43A047), Icons.Filled.Timeline, "Telemetry"),
    LEFT("Kernel Log", Color(0xFFFFB300), Icons.Filled.List, "Kernel"),
    RIGHT("EVEZ-OS", Color(0xFF1E88E5), Icons.Filled.Computer, "EVEZ"),
    TOP("Gemini Agent", Color(0xFF8E24AA), Icons.Filled.Chat, "Gemini"),
    BOTTOM("Settings", Color(0xFFF4511E), Icons.Filled.Settings, "Settings")
}

@Composable
fun RubiksCubeNavigation(modifier: Modifier = Modifier, onFaceClick: (String) -> Unit = {}, onFaceDoubleClick: (String) -> Unit = {}) {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var targetRotationX by remember { mutableFloatStateOf(0f) }
    var targetRotationY by remember { mutableFloatStateOf(0f) }

    var isAutoRotateEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(isAutoRotateEnabled) {
        if (isAutoRotateEnabled) {
            while (true) {
                kotlinx.coroutines.delay(2500)
                if (Math.random() > 0.5) {
                    rotationY += 90f
                    targetRotationY += 90f
                } else {
                    rotationX += 90f
                    targetRotationX += 90f
                }
            }
        }
    }

    val animatedRotationX by animateFloatAsState(
        targetValue = targetRotationX,
        animationSpec = spring(
            dampingRatio = 0.65f, // Smooth, slightly fluid dampening
            stiffness = 60f // Lower stiffness for fluid/orbital transitions
        ),
        label = "rotX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = targetRotationY,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 60f
        ),
        label = "rotY"
    )

    val cubeSize = 160.dp

    // Determine active face conceptually based on snapped rotations
    val snapX = ((animatedRotationX / 90f).roundToInt() % 4 + 4) % 4
    val snapY = ((animatedRotationY / 90f).roundToInt() % 4 + 4) % 4

    // Map standard Rubik's faces. We lock the visual so Y rotation dictates Left/Right/Front/Back, and X rotation dictates Top/Bottom
    val activeFace = when {
        snapX == 1 -> CubeFace.BOTTOM
        snapX == 3 -> CubeFace.TOP
        snapY == 1 -> CubeFace.RIGHT
        snapY == 2 -> CubeFace.BACK
        snapY == 3 -> CubeFace.LEFT
        else -> CubeFace.FRONT
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUBIK'S ALGORITHMIC NAVIGATION",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { isAutoRotateEnabled = !isAutoRotateEnabled }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Autorenew,
                    contentDescription = "Auto Rotate",
                    tint = if (isAutoRotateEnabled) Color(0xFFA3E635) else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAutoRotateEnabled) "AUTO" else "MANUAL",
                    color = if (isAutoRotateEnabled) Color(0xFFA3E635) else Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isAutoRotateEnabled = false },
                        onDragEnd = {
                            // Snap to nearest 90 degrees
                            targetRotationX = (Math.round(rotationX / 90f) * 90f)
                            targetRotationY = (Math.round(rotationY / 90f) * 90f)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        rotationX -= dragAmount.y * 0.4f
                        rotationY += dragAmount.x * 0.4f
                        targetRotationX = rotationX
                        targetRotationY = rotationY
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Pseudo-3D effect: we just scale and rotate a single card visually mimicking the face
            // This is clean, artifact-free, and mathematically stable in Jetpack Compose
            val activeRotX = (animatedRotationX % 90f).let { if (it > 45f) it - 90f else if (it < -45f) it + 90f else it }
            val activeRotY = (animatedRotationY % 90f).let { if (it > 45f) it - 90f else if (it < -45f) it + 90f else it }
            
            // Cross-fading alpha based on distance from center
            val cardAlpha = 1f - (abs(activeRotX) + abs(activeRotY)) / 90f
            val cardScale = 1f - (abs(activeRotX) + abs(activeRotY)) / 180f

            Box(
                modifier = Modifier
                    .size(cubeSize)
                    .graphicsLayer {
                        this.rotationX = activeRotX * 1.2f
                        this.rotationY = activeRotY * 1.2f
                        this.scaleX = cardScale.coerceAtLeast(0.1f)
                        this.scaleY = cardScale.coerceAtLeast(0.1f)
                        this.alpha = cardAlpha.coerceIn(0f, 1f)
                        this.cameraDistance = 12f * density
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(2.dp, activeFace.color.copy(alpha = cardAlpha.coerceIn(0.2f, 1f)), RoundedCornerShape(12.dp))
                    .pointerInput(activeFace.id) {
                        detectTapGestures(
                            onTap = { onFaceClick(activeFace.id) },
                            onDoubleTap = { onFaceDoubleClick(activeFace.id) }
                        )
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = activeFace.icon, contentDescription = activeFace.title, tint = activeFace.color, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = activeFace.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Active Face Metrics Overlay
        ActiveFaceMetrics(activeFace = activeFace)
    }
}

@Composable
fun ActiveFaceMetrics(activeFace: CubeFace) {
    val systemState by com.example.Kernel.eventLog.collectAsStateWithLifecycle()
    
    // Derived simple metrics for demo purposes based on active face
    val metricData = when (activeFace) {
        CubeFace.FRONT -> mapOf("Status" to "Online", "Resources" to "824 Nodes", "QPS" to "14,021")
        CubeFace.BACK -> mapOf("Event Log Size" to "${systemState.size}", "Latency" to "12ms", "Packets" to "Drops (0.01%)")
        CubeFace.LEFT -> mapOf("Recent Event" to (systemState.lastOrNull()?.type ?: "None"), "Subscribers" to "2", "Uptime" to "99.999%")
        CubeFace.RIGHT -> mapOf("Build" to "v4.5.1", "Kernel" to "Orion", "Processes" to "142 Active")
        CubeFace.TOP -> mapOf("Agent State" to "Idle", "Memory" to "3.4 GB", "Tokens/s" to "0")
        CubeFace.BOTTOM -> mapOf("Config Ver" to "2026-06", "Security" to "Locked", "Audit" to "Passed")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, activeFace.color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${activeFace.title.uppercase()} METRICS",
                color = activeFace.color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                metricData.forEach { (key, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = key, color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
