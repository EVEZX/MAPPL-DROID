package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CognitosegrephonicsDna(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dna")
    
    // Perma-scrolling phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(12000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier.fillMaxWidth().height(300.dp)) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amplitude = height * 0.4f
        
        val segments = 60
        val segmentWidth = width / segments
        
        val color1 = Color(0xFF00FFCC)
        val color2 = Color(0xFFFF00FF)
        val color3 = Color(0xFFE5E5E5)
        
        withTransform({
            rotate(rotation * 0.15f, Offset(width / 2, height / 2))
        }) {
            // Horizontal DNA
            for (i in 0 until segments) {
                val x1 = i * segmentWidth
                val x2 = (i + 1) * segmentWidth
                val f1 = x1 * 0.02f - phase
                val f2 = x2 * 0.02f - phase
                val y1a = centerY + sin(f1) * amplitude
                val y2a = centerY + sin(f2) * amplitude
                val y1b = centerY + sin(f1 + PI.toFloat()) * amplitude
                val y2b = centerY + sin(f2 + PI.toFloat()) * amplitude
                
                if (i % 2 == 0) {
                    val path = Path().apply {
                        moveTo(x1, y1a)
                        lineTo(x1, y1b)
                        lineTo(x1 + segmentWidth * 3f, centerY + sin(f1 + PI.toFloat() / 2) * amplitude * 0.2f)
                        close()
                    }
                    val alpha = (sin(f1 * 2f) + 1f) / 2f * 0.3f + 0.1f
                    drawPath(
                        path = path,
                        color = color3.copy(alpha = alpha),
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                drawLine(
                    color = color1.copy(alpha = (cos(f1) + 1f) / 2f * 0.7f + 0.3f),
                    start = Offset(x1, y1a),
                    end = Offset(x2, y2a),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color2.copy(alpha = (cos(f1 + PI.toFloat()) + 1f) / 2f * 0.7f + 0.3f),
                    start = Offset(x1, y1b),
                    end = Offset(x2, y2b),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // Vertical DNA
            val segmentHeight = height / segments
            val centerX = width / 2f
            for (i in 0 until segments) {
                val y1 = i * segmentHeight
                val y2 = (i + 1) * segmentHeight
                val f1 = y1 * 0.02f - phase
                val f2 = y2 * 0.02f - phase
                val x1a = centerX + sin(f1) * amplitude
                val x2a = centerX + sin(f2) * amplitude
                val x1b = centerX + sin(f1 + PI.toFloat()) * amplitude
                val x2b = centerX + sin(f2 + PI.toFloat()) * amplitude
                
                if (i % 2 == 0) {
                    val path = Path().apply {
                        moveTo(x1a, y1)
                        lineTo(x1b, y1)
                        lineTo(centerX + sin(f1 + PI.toFloat() / 2) * amplitude * 0.2f, y1 + segmentHeight * 3f)
                        close()
                    }
                    val alpha = (sin(f1 * 2f) + 1f) / 2f * 0.3f + 0.1f
                    drawPath(
                        path = path,
                        color = color3.copy(alpha = alpha),
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                drawLine(
                    color = color1.copy(alpha = (cos(f1) + 1f) / 2f * 0.7f + 0.3f),
                    start = Offset(x1a, y1),
                    end = Offset(x2a, y2),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color2.copy(alpha = (cos(f1 + PI.toFloat()) + 1f) / 2f * 0.7f + 0.3f),
                    start = Offset(x1b, y1),
                    end = Offset(x2b, y2),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        } // End of Canvas
        
        // Phenomenological phone entrapped infinity living words
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "NON-LINGUISTICS COGNITOSEGREPHONICS",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text = "segraphonemics ≈ Δx² telemetrics ouroboros",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Color(0xFF00FFCC).copy(alpha = 0.7f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
