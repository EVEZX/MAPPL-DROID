package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.Content
import com.example.GenerateContentRequest
import com.example.GeminiApi
import com.example.Part
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VoiceAgentState(val label: String, val color: Color) {
    INITIALIZING("Initializing...", Color(0xFF9E9E9E)),
    IDLE("Ready (Tap Mic to Speak)", Color(0xFF00FFCC)),
    LISTENING("Listening...", Color(0xFFFF0077)),
    THINKING("Thinking...", Color(0xFFAA00FF)),
    SPEAKING("Vocalizing...", Color(0xFF1E88E5)),
    ERROR("Error Occurred", Color(0xFFD32F2F))
}

@Composable
fun AppVoiceAgentDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var agentState by remember { mutableStateOf(VoiceAgentState.INITIALIZING) }
    var transcription by remember { mutableStateOf("") }
    var geminiReply by remember { mutableStateOf("") }
    var partialSpeech by remember { mutableStateOf("") }
    var debugLog by remember { mutableStateOf("Ready to initiate voice link...") }
    var continuousMode by remember { mutableStateOf(true) }
    var simulatedPower by remember { mutableFloatStateOf(0f) }

    // TTS holder
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    val hasAudioPermission = remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasAudioPermission.value = granted
            if (!granted) {
                debugLog = "[ERROR] Record Audio Permission denied. Running in visual fallback/simulation mode."
                agentState = VoiceAgentState.IDLE
            }
        }
    )

    // Speech Recognizer reference
    var speechRecognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Create TTS callback and initialization
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                ttsInstance?.language = Locale.getDefault()
                debugLog = "TextToSpeech Engine Ready."
                // Only start state sequence once TTS is ready
                if (agentState == VoiceAgentState.INITIALIZING) {
                    agentState = VoiceAgentState.IDLE
                }
            } else {
                debugLog = "[ERROR] TTS initialization failed. Continuing with visual feedback only."
                if (agentState == VoiceAgentState.INITIALIZING) {
                    agentState = VoiceAgentState.IDLE
                }
            }
        }
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Function to speak response out loud using TTS
    fun speakText(text: String) {
        val tts = ttsInstance
        if (tts != null && isTtsReady) {
            agentState = VoiceAgentState.SPEAKING
            debugLog = "Speaking assistant reply..."
            
            // Set listener to detect when speech is completed
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                
                override fun onDone(utteranceId: String?) {
                    scope.launch {
                        if (continuousMode) {
                            debugLog = "Speech done. Continuous Mode: listening again."
                            // Give a short 500ms breather before starting next recognition
                            delay(500)
                            triggerListening(context, speechRecognizerRef, hasAudioPermission.value, permissionLauncher, onStartListening = {
                                agentState = VoiceAgentState.LISTENING
                                transcription = ""
                                partialSpeech = ""
                            }, onError = { err ->
                                agentState = VoiceAgentState.IDLE
                                debugLog = err
                            })
                        } else {
                            agentState = VoiceAgentState.IDLE
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    scope.launch {
                        agentState = VoiceAgentState.IDLE
                        debugLog = "[ERROR] TTS speaking error: $errorCode"
                    }
                }
            })

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "GeminiReplyUtterance")
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "GeminiReplyUtterance")
        } else {
            debugLog = "TTS uninitialized, response displayed visually."
            agentState = VoiceAgentState.IDLE
        }
    }

    // Function to handle sending text queries to Gemini Agent
    fun queryGemini(text: String) {
        if (text.isBlank()) return
        agentState = VoiceAgentState.THINKING
        debugLog = "Sending telemetry query to Gemini-3.5-Flash..."
        
        scope.launch {
            try {
                // Ensure natural synthesized response style
                val systemInstruction = "You are the vocal electronic assistant of OpenClaw. Your name is EVEZ Core. The user is speaking to you via real-time microphone input. Respond conversationally, keeping your responses strictly brief, concise, friendly, and natural (1 or 2 short sentences maximum). Speak in a helpful system assistant persona."
                val request = GenerateContentRequest(
                    systemInstruction = Content(parts = listOf(Part(systemInstruction)), role = "system"),
                    contents = listOf(Content(parts = listOf(Part(text)), role = "user"))
                )
                
                val response = GeminiApi.service.generateContent(
                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = request
                )
                
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "Synthesizer failed to generate response."
                
                geminiReply = reply
                com.example.Kernel.appendEvent(com.example.DomainEvent("VOICE_AGENT_REPLY", "VoiceAgent", mapOf("query" to text, "reply" to reply)))
                speakText(reply)
            } catch (e: Exception) {
                agentState = VoiceAgentState.ERROR
                val err = e.message ?: "Unreachable API"
                debugLog = "[ERROR] Gemini Dispatch failed: $err"
                geminiReply = "Apologies, the mental synapsis API is currently unreachable."
                speakText(geminiReply)
            }
        }
    }

    // Initialize and maintain SpeechRecognizer safely
    LaunchedEffect(hasAudioPermission.value) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    agentState = VoiceAgentState.LISTENING
                    debugLog = "Live Audio Channel Active. Speak now..."
                }

                override fun onBeginningOfSpeech() {
                    debugLog = "Decompressed speech energy detected..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize RMSdB typical values (-2 to 10) to 0.0 - 1.0
                    val rmsNormalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    simulatedPower = rmsNormalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    debugLog = "Speech energy cycle concluded. Restructuring..."
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No voice matched (silence)"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Android speech servers error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (silence)"
                        else -> "Unknown error"
                    }
                    debugLog = "[RECOGNIZER] Code $error: $message"
                    
                    // If no results, gracefully fall back to IDLE
                    if (agentState == VoiceAgentState.LISTENING) {
                        agentState = VoiceAgentState.IDLE
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val resultText = matches[0]
                        transcription = resultText
                        partialSpeech = ""
                        debugLog = "Recognized: '$resultText'"
                        com.example.Kernel.appendEvent(com.example.DomainEvent("VOICE_INPUT_RECOGNIZED", "VoiceAgent", mapOf("text" to resultText)))
                        queryGemini(resultText)
                    } else {
                        agentState = VoiceAgentState.IDLE
                        debugLog = "No speech results captured."
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        partialSpeech = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizerRef = recognizer
        } else {
            debugLog = "[ERROR] SpeechRecognizer not natively available on this system/device."
        }
    }

    // Clean up SpeechRecognizer
    DisposableEffect(speechRecognizerRef) {
        onDispose {
            speechRecognizerRef?.destroy()
        }
    }

    // Interactive Pulsing Phase & Rotation Transitions for visual representation
    val infiniteTransition = rememberInfiniteTransition(label = "voice_agent_sphere")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "idle_phase"
    )
    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "fast_rotation"
    )

    // Modal Frame View
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712).copy(alpha = 0.95f))
            .clickable { /* Block clicks from dismiss */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "A16 QUANTUM VOICE AGENT",
                        color = Color(0xFF00FFCC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "CONNECTED VIA INTEL CORE SPECS",
                        color = Color(0xFFAA00FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                IconButton(
                    onClick = {
                        ttsInstance?.stop()
                        speechRecognizerRef?.stopListening()
                        onClose()
                    }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Agent Screen", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Master Visualizer / Spherical Waveforms
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFAA00FF).copy(alpha = 0.3f), CircleShape)
                    .background(Color(0xFF0C101F))
                    .clickable {
                        // User triggers listening manually when tapping center sphere
                        if (agentState == VoiceAgentState.IDLE || agentState == VoiceAgentState.ERROR) {
                            ttsInstance?.stop() // stop speaking if spoken
                            triggerListening(
                                context = context,
                                recognizer = speechRecognizerRef,
                                hasPermission = hasAudioPermission.value,
                                launcher = permissionLauncher,
                                onStartListening = {
                                    agentState = VoiceAgentState.LISTENING
                                    transcription = ""
                                    partialSpeech = ""
                                },
                                onError = { err ->
                                    agentState = VoiceAgentState.IDLE
                                    debugLog = err
                                }
                            )
                        } else if (agentState == VoiceAgentState.LISTENING) {
                            speechRecognizerRef?.stopListening()
                            agentState = VoiceAgentState.IDLE
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val baseRadius = size.width / 4f
                    
                    // State-based coloration and dynamics
                    val colorOuter = when (agentState) {
                        VoiceAgentState.INITIALIZING -> Color(0xFF555555)
                        VoiceAgentState.IDLE -> Color(0xFF00FFCC)
                        VoiceAgentState.LISTENING -> Color(0xFFFF0066)
                        VoiceAgentState.THINKING -> Color(0xFFAA00FF)
                        VoiceAgentState.SPEAKING -> Color(0xFF00E5FF)
                        VoiceAgentState.ERROR -> Color(0xFFD50000)
                    }
                    
                    val activeVolumeScale = if (agentState == VoiceAgentState.LISTENING) {
                        simulatedPower.coerceAtLeast(0.1f)
                    } else if (agentState == VoiceAgentState.SPEAKING) {
                        // Simulate speech ripple using a sine pattern
                        (0.4f + 0.3f * sin(idlePhase * 4f))
                    } else if (agentState == VoiceAgentState.THINKING) {
                        0.2f
                    } else {
                        0.05f
                    }

                    // Render dynamic surrounding energy levels
                    val rippleRadius = baseRadius + (baseRadius * activeVolumeScale * 1.5f)
                    drawCircle(
                        color = colorOuter.copy(alpha = 0.15f),
                        radius = rippleRadius,
                        center = center
                    )
                    drawCircle(
                        color = colorOuter.copy(alpha = 0.3f),
                        radius = baseRadius + (baseRadius * activeVolumeScale * 0.8f),
                        style = Stroke(width = 2.dp.toPx()),
                        center = center
                    )

                    // Draw intricate center rotating fractal rings
                    withTransform({
                        rotate(if (agentState == VoiceAgentState.THINKING) fastRotation else idlePhase * (180f / PI.toFloat()), center)
                    }) {
                        val sideCount = 12
                        for (i in 0 until sideCount) {
                            val angle = i * (2f * PI.toFloat() / sideCount)
                            val ringWidth = 4.dp.toPx()
                            val extension = if (agentState == VoiceAgentState.THINKING) {
                                baseRadius * 0.4f * sin(angle * 3f + (fastRotation * PI.toFloat() / 180f))
                            } else {
                                baseRadius * 0.2f * cos(angle * 4f + idlePhase)
                            }
                            
                            val startOffset = baseRadius - 10.dp.toPx()
                            val endOffset = baseRadius + extension + (activeVolumeScale * 30.dp.toPx())
                            
                            drawLine(
                                color = colorOuter.copy(alpha = 0.8f),
                                start = Offset(center.x + cos(angle) * startOffset, center.y + sin(angle) * startOffset),
                                end = Offset(center.x + cos(angle) * endOffset, center.y + sin(angle) * endOffset),
                                strokeWidth = ringWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    // Central core orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colorOuter, colorOuter.copy(alpha = 0.4f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 0.8f
                        ),
                        radius = baseRadius * 0.8f,
                        center = center
                    )
                }

                // Centered dynamic instruction icon
                Icon(
                    imageVector = when (agentState) {
                        VoiceAgentState.LISTENING -> Icons.Filled.Mic
                        VoiceAgentState.THINKING -> Icons.Filled.AutoAwesome
                        VoiceAgentState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                        VoiceAgentState.ERROR -> Icons.Filled.Error
                        else -> Icons.Filled.PowerSettingsNew
                    },
                    contentDescription = "Vocal State Flag",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Badge
            Surface(
                color = agentState.color.copy(alpha = 0.15f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, agentState.color)
            ) {
                Text(
                    text = agentState.label.uppercase(),
                    color = agentState.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continuous Conversation Option Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F2937))
                    .clickable { continuousMode = !continuousMode }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (continuousMode) Icons.Filled.Sync else Icons.Filled.SyncDisabled,
                    contentDescription = null,
                    tint = if (continuousMode) Color(0xFF00FFCC) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (continuousMode) "AUTO-CONVERSE ACTIVE (Hands-Free Loop)" else "AUTO-CONVERSE DISABLED",
                    color = if (continuousMode) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text transcription and speech bubble panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "SPEECH TELEMETRY OUTPUT:",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val currentUtterance = if (partialSpeech.isNotEmpty()) partialSpeech else if (transcription.isNotEmpty()) transcription else "No voice captured yet."
                Text(
                    text = if (agentState == VoiceAgentState.LISTENING) "\"$currentUtterance\"" else "\"$transcription\"",
                    color = if (transcription.isEmpty() && partialSpeech.isEmpty()) Color.DarkGray else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ASSISTANT SYNAPSE RESPONSE:",
                    color = Color(0xFF00FFCC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (geminiReply.isEmpty()) "Awaiting response pipeline query..." else geminiReply,
                    color = if (geminiReply.isEmpty()) Color.DarkGray else Color(0xFFE5E7EB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulated fallback input area for non-audiographic hosts
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                var manualText by remember { mutableStateOf("") }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MANUAL TELEMETRY PORT (FALLBACK)",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            placeholder = { Text("Simulator context command...", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualText.isNotBlank()) {
                                    val queryText = manualText
                                    manualText = ""
                                    transcription = queryText
                                    ttsInstance?.stop()
                                    queryGemini(queryText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color(0xFF030712)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("SEND", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic core telemetry logs
            Text(
                text = "LOG: $debugLog",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun triggerListening(
    context: Context,
    recognizer: SpeechRecognizer?,
    hasPermission: Boolean,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    onStartListening: () -> Unit,
    onError: (String) -> Unit
) {
    if (!hasPermission) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
        return
    }

    if (recognizer == null) {
        onError("Speech Recognition Service Unavailable on host.")
        return
    }

    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
        onStartListening()
    } catch (e: Exception) {
        onError("Could not bind speech intent: ${e.message}")
    }
}
