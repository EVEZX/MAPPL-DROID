package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SensoryAudioGeometries(modifier: Modifier = Modifier) {
    var rawAudioPeak by remember { mutableFloatStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val hasPermission = remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission.value = granted
            isListening = granted
        }
    )

    LaunchedEffect(hasPermission.value) {
        if (!hasPermission.value) {
            // request permission
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            isListening = true
        }
    }

    // Audio input task
    LaunchedEffect(isListening) {
        if (isListening && hasPermission.value) {
            withContext(Dispatchers.IO) {
                var audioRecord: AudioRecord? = null
                try {
                    val sampleRate = 8000
                    val bufferSize = AudioRecord.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )

                    audioRecord.startRecording()
                    val audioBuffer = ShortArray(bufferSize)

                    while (isActive) {
                        val read = audioRecord.read(audioBuffer, 0, bufferSize)
                        if (read > 0) {
                            var maxAmplitude = 0
                            for (i in 0 until read) {
                                val amplitude = Math.abs(audioBuffer[i].toInt())
                                if (amplitude > maxAmplitude) {
                                    maxAmplitude = amplitude
                                }
                            }
                            // Normalize somewhat (Short.MAX_VALUE is 32767)
                            val normalized = (maxAmplitude.toFloat() / 32767f) * 2f
                            rawAudioPeak = normalized.coerceIn(0f, 1f)
                        }
                        delay(16) // roughly 60fps update
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    audioRecord?.stop()
                    audioRecord?.release()
                }
            }
        } else {
             // Fallback simulation if no permission - sync with global audio pulse!
             while(true) {
                 delay(16)
                 rawAudioPeak = com.example.globalAudioPulse
             }
        }
    }

    val smoothedAudioPeak by animateFloatAsState(
        targetValue = rawAudioPeak,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 100f),
        label = "audio_peak"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "geometries")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(10000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(20000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.CardColor),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Audio input",
                        tint = Color(0xFF8E24AA)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SENSORY AUDIO GEOMETRIES",
                        color = com.example.TextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = "MIC/BROWSER SYNC",
                    color = Color(0xFF8E24AA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF8E24AA).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val center = Offset(width / 2f, height / 2f)
                    val baseRadius = minOf(width, height) / 3f
                    
                    // The reactive audio radius
                    val dynamicRadius = baseRadius + (baseRadius * smoothedAudioPeak * 0.5f)
                    
                    val colorOrb = Color(0xFF00FFCC)
                    val colorHex = Color(0xFF8E24AA)
                    val colorGrid = Color(0xFF2A2E3B)
                    
                    // Draw Xbox-style background grid or circular ripples
                    for (i in 1..4) {
                        drawCircle(
                            color = colorGrid.copy(alpha = 0.5f / i),
                            radius = baseRadius * i * 0.5f + (smoothedAudioPeak * 20f),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Gamecube/Xbox style cube/hex forms in perspective
                    withTransform({
                        rotate(rotation, center)
                        scale(1f, 0.85f, center) // isometric squish
                    }) {
                        val sides = 6
                        
                        // Draw outer hexagon
                        val hexPath = Path().apply {
                            for (i in 0 until sides) {
                                val angle = i * (2f * PI.toFloat() / sides) + phase
                                val x = center.x + cos(angle) * dynamicRadius
                                val y = center.y + sin(angle) * dynamicRadius
                                
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        
                        drawPath(
                            path = hexPath,
                            color = colorHex.copy(alpha = 0.8f),
                            style = Stroke(
                                width = 6.dp.toPx() * smoothedAudioPeak.coerceAtLeast(0.2f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Draw Y-lines forming the 3D cube illusion
                        for (i in listOf(1, 3, 5)) {
                            val a = i * (2f * PI.toFloat() / sides) + phase
                            drawLine(
                                color = colorHex.copy(alpha = 0.6f),
                                start = center,
                                end = Offset(center.x + cos(a) * dynamicRadius, center.y + sin(a) * dynamicRadius),
                                strokeWidth = 6.dp.toPx() * smoothedAudioPeak.coerceAtLeast(0.2f),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Floating particles that react to audio
                    withTransform({
                        rotate(-rotation * 1.5f, center)
                    }) {
                        val particleCount = 20
                        for (i in 0 until particleCount) {
                            val f = i * (2f * PI.toFloat() / particleCount)
                            val r = dynamicRadius * (1f + 0.3f * sin(phase * 2f + i))
                            val px = center.x + cos(f) * r
                            val py = center.y + sin(f) * r
                            
                            drawCircle(
                                color = colorOrb.copy(alpha = 0.6f + (smoothedAudioPeak * 0.4f)),
                                radius = 4.dp.toPx() * smoothedAudioPeak,
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }
        }
    }
}
