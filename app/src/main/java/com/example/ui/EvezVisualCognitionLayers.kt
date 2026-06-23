package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DynamicForm
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DomainEvent
import com.example.Kernel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class VclLayerInfo(
    val name: String,
    val pathway: String,
    val initialSync: Float,
    val frequencyOffset: Float,
    val role: String
)

@Composable
fun EvezVisualCognitionPanel(isActive: Boolean) {
    if (!isActive) return

    val scope = rememberCoroutineScope()
    var synthesisRate by remember { mutableFloatStateOf(0.72f) }
    var resolutionIndex by remember { mutableFloatStateOf(4.0f) }
    var depthLevel by remember { mutableFloatStateOf(3.2f) }
    var syncTriggerActive by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(1f) }
    var anomalyDetected by remember { mutableStateOf(false) }
    
    // Console logs within the subsystem
    val subLogs = remember { mutableStateListOf("VCL System state: Idle (Monitoring active pathways)") }

    // Dynamic wave animation for Canvas
    val transition = rememberInfiniteTransition(label = "VCLAnimation")
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    // Animated scanline Y position
    val scanLineY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanLine"
    )

    // Dynamic rotation of nodes
    val nodeRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "NodeRot"
    )

    val layers = remember {
        listOf(
            VclLayerInfo("VCL-Alpha: Pathway Ocular", "Ophthalm-Tracing", 0.98f, 15.4f, "Oculo-Motor map feedback control"),
            VclLayerInfo("VCL-Beta: Retinal Projection", "Refraction-Grid", 0.93f, 22.1f, "Focal ray alignment matrix"),
            VclLayerInfo("VCL-Theta: Semantic Dreamtime", "Intuition-Router", 0.12f, 4.8f, "Neural network sleep cycle mapping"),
            VclLayerInfo("VCL-Delta: Holographic Mesh", "Phase-Synthesis", 0.81f, 31.6f, "3D vector point depth solver")
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("evez_vcl_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070F1E)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF162A45))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FilterCenterFocus,
                        contentDescription = "Visual Cognition Layer",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EVEZ-VCL COGNITION PATHWAYS",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = if (anomalyDetected) Color(0x33FF1744) else Color(0x1A00FFCC),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, if (anomalyDetected) Color(0xFFFF1744) else Color(0x8000FFCC), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (anomalyDetected) "ANOMALY SEEN" else "STABLE MESH",
                        color = if (anomalyDetected) Color(0xFFFF8A80) else Color(0xFF00FFCC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Visualizer Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF030811))
                    .border(1.dp, Color(0xFF0F1E36), RoundedCornerShape(16.dp))
            ) {
                // Interactive Canvas drawing real-time floating visual nodes, connections, and scanning lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw subtle grid
                    val gridSpacing = 24.dp.toPx()
                    for (x in 0..(w / gridSpacing).toInt()) {
                        drawLine(
                            color = Color(0xFF0E1A2B),
                            start = Offset(x * gridSpacing, 0f),
                            end = Offset(x * gridSpacing, h),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                    for (y in 0..(h / gridSpacing).toInt()) {
                        drawLine(
                            color = Color(0xFF0E1A2B),
                            start = Offset(0f, y * gridSpacing),
                            end = Offset(w, y * gridSpacing),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }

                    // 2. Wave curve based on path calculations
                    val path = Path()
                    path.moveTo(0f, h / 2f)
                    val pointsCount = 100
                    for (i in 0..pointsCount) {
                        val fraction = i.toFloat() / pointsCount
                        val px = fraction * w
                        // Combination of sinus waves influenced by our sliders
                        val angle = (fraction * 4f * java.lang.Math.PI.toFloat()) + waveOffset
                        val extraMod = sin(angle * 1.5f + waveOffset) * 15f * depthLevel
                        val py = (h / 2f) + sin(angle) * 30f * synthesisRate + extraMod
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF00FFCC).copy(alpha = 0.65f),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 3. Floating Ocular Nodes (with rotation logic)
                    val centerX = w / 2f
                    val centerY = h / 2f
                    val nodeCount = 5
                    val radius = 50.dp.toPx() * (resolutionIndex / 4.0f)
                    
                    for (i in 0 until nodeCount) {
                        val angleDeg = (i * (360f / nodeCount)) + nodeRotation
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val nx = centerX + cos(angleRad).toFloat() * radius
                        val ny = centerY + sin(angleRad).toFloat() * radius

                        // Draw node connection line to center
                        drawLine(
                            color = Color(0x3300E5FF),
                            start = Offset(centerX, centerY),
                            end = Offset(nx, ny),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw Node circles
                        drawCircle(
                            color = Color(0xFFAA00FF).copy(alpha = 0.8f),
                            radius = 6.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                        drawCircle(
                            color = Color(0xFF00FFCC),
                            radius = 2.5.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }

                    // Center key node
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 8.dp.toPx() * calibrationProgress,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )

                    // 4. Horizontal scanline representing retinal trace bar
                    val scanY = scanLineY * h
                    drawLine(
                        color = Color(0xFFFF0055).copy(alpha = 0.5f),
                        start = Offset(0f, scanY),
                        end = Offset(w, scanY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawRect(
                        color = Color(0xFFFF0055).copy(alpha = 0.05f),
                        topLeft = Offset(0f, Math.max(0f, scanY - 8.dp.toPx())),
                        size = Size(w, 16.dp.toPx())
                    )
                }

                // Small HUD corner text indicators
                Text(
                    text = "VCL-GRID COORDS: SYNCD",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Text(
                    text = "RESOLUTION INDEX: ${String.format("%.2fx", resolutionIndex)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
                Text(
                    text = "SYNAPSE DEPTH: ${String.format("%.1f", depthLevel)}M",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem Control Sliders
            Text(
                text = "VCL TRANSMISSION TUNERS:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A99AD),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Slider 1: Synthesis Rate
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pathway Synthesis",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.width(110.dp)
                )
                Slider(
                    value = synthesisRate,
                    onValueChange = {
                        synthesisRate = it
                        if (it > 0.9f && !anomalyDetected) {
                            anomalyDetected = true
                            subLogs.add(0, "[WARN] Refractive saturation excess detected in Ocular Array!")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00FFCC),
                        activeTrackColor = Color(0xFF0E2F40)
                    )
                )
                Text(
                    text = "${(synthesisRate * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.width(36.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Slider 2: Spectral Resolution
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Spectral Resolution",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.width(110.dp)
                )
                Slider(
                    value = resolutionIndex,
                    onValueChange = { resolutionIndex = it },
                    valueRange = 1.0f..8.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF0E2F40)
                    )
                )
                Text(
                    text = String.format("%.1fx", resolutionIndex),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.width(36.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Slider 3: Layer Depth
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Synaptic Depth Factor",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.width(110.dp)
                )
                Slider(
                    value = depthLevel,
                    onValueChange = { depthLevel = it },
                    valueRange = 1.0f..6.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFAA00FF),
                        activeTrackColor = Color(0xFF0E2F40)
                    )
                )
                Text(
                    text = String.format("%.1fM", depthLevel),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.width(36.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Suite Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Calibration Trigger
                Button(
                    onClick = {
                        scope.launch {
                            syncTriggerActive = true
                            calibrationProgress = 1.8f
                            subLogs.add(0, "[VCL] Hard resetting holographic focus points...")
                            delay(600)
                            calibrationProgress = 1.0f
                            anomalyDetected = false
                            subLogs.add(0, "[VCL SUCCESS] Calibration trace unified. Node lock: 100% stable.")
                            syncTriggerActive = false
                            Kernel.appendEvent(
                                DomainEvent(
                                    type = "EVEZ_VCL_CALIBRATE",
                                    domainId = "EvezOsManager",
                                    payload = mapOf("ocular_sync" to "1.00", "status" to "CALIBRATED")
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("vcl_calibrate_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3300FFCC),
                        contentColor = Color(0xFF00FFCC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Anomaly Simulator
                Button(
                    onClick = {
                        scope.launch {
                            anomalyDetected = !anomalyDetected
                            if (anomalyDetected) {
                                subLogs.add(0, "[ALERT] High frequency jitter in Retinal Trace Layer detected!")
                                Kernel.appendEvent(
                                    DomainEvent(
                                        type = "EVEZ_VCL_ANOMALY_WARN",
                                        domainId = "EvezOsManager",
                                        payload = mapOf("anomaly" to "High frequency Retinal Jitter", "pathway" to "VCL-Beta")
                                    )
                                )
                            } else {
                                subLogs.add(0, "[INFO] Manually cleared anomalies on all visual cognition pathways.")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("vcl_simulate_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33FF0055),
                        contentColor = Color(0xFFFF3366)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Toggle Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Clear logs
                Button(
                    onClick = {
                        subLogs.clear()
                        subLogs.add("[VCL Console Logs Flushed]")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("vcl_clear_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1A8A99AD),
                        contentColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Pathway Readout / Layer Matrix
            Text(
                text = "VCL CORE COGNITION LAYER MATRIX STATUS:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A99AD),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF03070E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF0F1B2C), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                layers.forEach { layer ->
                    val dynamicSync = if (layer.name.contains("Alpha") && anomalyDetected) "61.2% Jitter" else if (layer.name.contains("Beta") && anomalyDetected) "44.9% Trace Lost" else "${(layer.initialSync * 100).toInt()}% Sync"
                    val isLocked = !anomalyDetected || (!layer.name.contains("Alpha") && !layer.name.contains("Beta"))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = layer.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Path: ${layer.pathway} • ${layer.role}",
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isLocked) Color(0x2200FFCC) else Color(0x33FF1744),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = dynamicSync,
                                color = if (isLocked) Color(0xFF00FFCC) else Color(0xFFFF8A80),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subsystem Console Logs Board
            Text(
                text = "VCL CONSOLE METRIC RUNTIME:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A99AD),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF010408), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF09111D), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    subLogs.take(3).forEach { log ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "»",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = log,
                                color = if (log.contains("ALERT") || log.contains("WARN")) Color(0xFFFF8A80) else Color.LightGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
