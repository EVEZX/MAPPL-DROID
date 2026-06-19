package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.Kernel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitalTelemetryChart(modifier: Modifier = Modifier) {
    val events by Kernel.eventLog.collectAsStateWithLifecycle()
    
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "r1"
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing), RepeatMode.Restart),
        label = "r2"
    )
    val rotation3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing), RepeatMode.Restart),
        label = "r3"
    )

    // Derived stats from events to drive organic motion
    val eventCount = events.size
    val recentActivity = events.takeLast(30)
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2f * 0.95f
        
        // Non-euclidean projection simulation (warped rings)
        val ringCount = 7
        for (i in 0 until ringCount) {
            val radius = maxRadius * (i + 1) / ringCount
            
            // Outer rings react to event count to simulate breathing/pulsing
            val pulse = if (recentActivity.isNotEmpty()) sin(eventCount * 0.1f) * 10f else 0f
            val dynamicRadius = radius + pulse * (i * 0.2f)
            
            val rot = when(i % 3) {
                0 -> rotation1
                1 -> rotation2
                else -> rotation3
            } * (1f + i * 0.3f)

            withTransform({
                translate(left = center.x, top = center.y)
                rotate(degrees = rot)
                // Add non-euclidean tilt via scale and polarize rotation swaps
                scale(scaleX = 1f, scaleY = 0.4f + (i * 0.08f))
                rotate(degrees = i * 45f + rotation1 * 0.1f)
                translate(left = -center.x, top = -center.y)
            }) {
                // Base orbital ring
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.1f + (i * 0.05f)),
                    radius = dynamicRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx() + (eventCount % 3), cap = StrokeCap.Round)
                )
                
                // Draw telemetry data nodes on the rings
                val nodesOnRing = 4 + i * 3
                for (j in 0 until nodesOnRing) {
                    val angle = (j * (360f / nodesOnRing)) * (Math.PI / 180f)
                    val nx = center.x + dynamicRadius * cos(angle).toFloat()
                    val ny = center.y + dynamicRadius * sin(angle).toFloat()
                    
                    // Node color based on recent event types to map semantic domains
                    val activityIndex = (i * nodesOnRing + j) % maxOf(1, recentActivity.size)
                    val nodeColor = if (recentActivity.isNotEmpty() && activityIndex < recentActivity.size) {
                        when (recentActivity[activityIndex].domainId) {
                            "SystemScan" -> Color(0xFFE53935)
                            "GeminiAgent" -> Color(0xFF8E24AA)
                            "UserInterface" -> Color(0xFF1E88E5)
                            "QuantumBrowser" -> Color(0xFFF472B6)
                            else -> Color(0xFFA3E635)
                        }
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    }

                    // Data payload size influences node radius
                    val payloadFactor = recentActivity.getOrNull(activityIndex)?.payload?.size ?: 0
                    
                    drawCircle(
                        color = nodeColor,
                        radius = 4.dp.toPx() + payloadFactor * 1.5f,
                        center = Offset(nx, ny)
                    )
                }
            }
        }
        
        // Draw connecting structural ley lines (geometric puzzle aspect)
        for (i in 0 until 8) {
            val aAngle = (rotation1 + i * 45) * (Math.PI / 180f)
            val ax = center.x + maxRadius * cos(aAngle).toFloat()
            val ay = center.y + (maxRadius * 0.4f) * sin(aAngle).toFloat()
            
            val bAngle = (rotation2 + i * 45 + 22.5) * (Math.PI / 180f)
            val bx = center.x + (maxRadius * 0.7f) * cos(bAngle).toFloat()
            val by = center.y + (maxRadius * 0.7f * 0.5f) * sin(bAngle).toFloat()

            drawLine(
                color = Color(0xFFF472B6).copy(alpha = 0.2f),
                start = Offset(ax, ay),
                end = Offset(bx, by),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
