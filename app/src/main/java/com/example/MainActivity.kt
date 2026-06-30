package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.example.BuildConfig
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    try {
        FirebaseAuthManager.init(applicationContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    try {
        val jsCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
        val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
        if (!jsCacheDir.exists()) {
            jsCacheDir.mkdirs()
        }
        if (!wasmCacheDir.exists()) {
            wasmCacheDir.mkdirs()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            OpenClawDashboard()
            com.example.ui.ContextReasoningOverlay()
        }
      }
    }
  }
}

var isDarkMode by mutableStateOf(true)
var isAutopilotActive by mutableStateOf(false)
var globalAudioPulse by mutableStateOf(0.1f)
var isRetroMusicEnabled by mutableStateOf(false)

fun startRetroChiptuneEngine(
    scope: kotlinx.coroutines.CoroutineScope,
    onPeak: (Float) -> Unit
): (() -> Unit) {
    var isRunning = true
    val sampleRate = 11025
    val minBufferSize = try {
        AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    } catch (e: Exception) {
        512
    }
    
    val audioTrack = try {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(if (minBufferSize > 0) minBufferSize else 1024)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    } catch (e: Exception) {
        null
    }

    if (audioTrack != null) {
        try {
            audioTrack.play()
        } catch (e: Exception) {
            // Let fallback handle it if play fails
        }
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val notes = listOf(
                261.63f, 293.66f, 329.63f, 349.23f, 392.00f, 440.00f, 493.88f, 523.25f, // Major
                196.00f, 220.00f, 246.94f, 261.63f, 293.66f, 329.63f, 392.00f, 440.00f  // OSINT Arpeggio
            )
            val chordProgressions = listOf(
                listOf(261.63f, 329.63f, 392.00f), // C Major
                listOf(220.00f, 261.63f, 329.63f), // A Minor (Mysterious)
                listOf(349.23f, 440.00f, 523.25f), // F Major
                listOf(196.00f, 246.94f, 293.66f)  // G Major
            )
            var noteIndex = 0
            val noteDurationSamples = sampleRate / 6 // 6 notes per second
            val buffer = ShortArray(noteDurationSamples)

            while (isRunning && isActive) {
                val noteFreq = notes[noteIndex % notes.size]
                val chord = chordProgressions[(noteIndex / 4) % chordProgressions.size]
                
                var maxVal = 0f
                for (i in 0 until noteDurationSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    // Melody oscillator (Square wave for retro feel!)
                    val melodySample = if (kotlin.math.sin(2.0 * Math.PI * noteFreq * t) > 0) 0.15 else -0.15
                    
                    // Chords oscillators (Triangle/Sine wave for backing)
                    var chordSample = 0.0
                    chord.forEach { freq ->
                        chordSample += kotlin.math.sin(2.0 * Math.PI * freq * t) * 0.08
                    }
                    
                    // Add noise or telemetry beep boops to give retro cockpit feel
                    val telemetrySample = if (noteIndex % 3 == 0 && i < noteDurationSamples / 4) {
                        kotlin.math.sin(2.0 * Math.PI * 1500.0 * t) * 0.1
                    } else 0.0
                    
                    val combined = melodySample + chordSample + telemetrySample
                    val pcmValue = (combined * 32767.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    buffer[i] = pcmValue
                    
                    val absVal = Math.abs(pcmValue.toFloat()) / 32767f
                    if (absVal > maxVal) {
                        maxVal = absVal
                    }
                }
                
                try {
                    audioTrack.write(buffer, 0, noteDurationSamples)
                } catch (e: Exception) {
                    // ignore write failures
                }
                
                onPeak(maxVal)
                
                noteIndex++
                kotlinx.coroutines.yield()
            }
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {}
        }
    } else {
        // Fallback procedural animation generator if audio track is not supported
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            var t = 0f
            while (isRunning && isActive) {
                val value = 0.3f + 0.5f * kotlin.math.sin(t).coerceIn(-1f, 1f)
                onPeak(value)
                t += 0.2f
                kotlinx.coroutines.delay(100)
            }
        }
    }

    return {
        isRunning = false
    }
}

val BgColor: Color
  @Composable get() = if (isDarkMode) Color(0xFF0B0D0F) else Color(0xFFF3F4F6)

val CardColor: Color
  @Composable get() = if (isDarkMode) Color(0xFF1A1C1E) else Color(0xFFFFFFFF)

val TextColor: Color
  @Composable get() = if (isDarkMode) Color(0xFFE2E2E6) else Color(0xFF111827)

val BorderColor: Color
  @Composable get() = if (isDarkMode) Color(0xFF2D3135) else Color(0xFFE5E7EB)

val AccentColor: Color
  @Composable get() = if (isDarkMode) Color(0xFFD1E4FF) else Color(0xFF2563EB)

val SubTextColor: Color
  @Composable get() = if (isDarkMode) Color(0xFF8E9196) else Color(0xFF4B5563)

@Composable
fun OpenClawDashboard() {
  var isChatOpen by remember { mutableStateOf(false) }
  var isBrowserOpen by remember { mutableStateOf(false) }
  var showAuthDialog by remember { mutableStateOf(false) }
  val firebaseUser by FirebaseAuthManager.currentUserState.collectAsStateWithLifecycle()
  val simulatedEmail by FirebaseAuthManager.simulatedUserEmail.collectAsStateWithLifecycle()
  val isFallbackMode by FirebaseAuthManager.isFallbackMode.collectAsStateWithLifecycle()
  val authLogs by FirebaseAuthManager.authLogs.collectAsStateWithLifecycle()
  var isFabMenuOpen by remember { mutableStateOf(false) }
  var showVoiceDialog by remember { mutableStateOf(false) }
  var systemAlerts by remember { mutableStateOf(listOf("System initializing... Gemini quota scanners active.")) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val database = remember { AppDatabase.getDatabase(context) }
  val cachedStates by database.resourceStateDao().getAllStates().collectAsStateWithLifecycle(initialValue = emptyList())

  var widenCognitionKeys by remember { mutableStateOf<String?>(null) }
  var sshConnected by remember { mutableStateOf(false) }
  var gatewayToken by remember { mutableStateOf("NOT_GENERATED") }
  var showAddProviderDialog by remember { mutableStateOf(false) }
  var providers by remember { mutableStateOf(listOf(
    ProviderData("G", "Gemini Pro Latest", "AQ.Ab8RN6JH...", Color(0xFF4285F4), true),
    ProviderData("C", "Claude 3.5 Sonnet", "sk-ant-api03-...", Color(0xFF7b61ff), false),
    ProviderData("O", "OpenAI GPT-4o", "sk-proj-4/0Adk...", Color(0xFF10a37f), true)
  )) }
  var newProviderName by remember { mutableStateOf("") }
  var newProviderKey by remember { mutableStateOf("") }
  var newProviderEndpoint by remember { mutableStateOf("") }
  var latencyData by remember { mutableStateOf(listOf(120f, 135f, 110f, 180f, 95f, 150f, 115f)) }
  var tokenData by remember { mutableStateOf(listOf(40f, 60f, 30f, 90f, 50f, 85f, 75f)) }

  // Retro Music Engine Activator
  DisposableEffect(isRetroMusicEnabled) {
      var stopEngine: (() -> Unit)? = null
      if (isRetroMusicEnabled) {
          stopEngine = startRetroChiptuneEngine(scope) { peak ->
              globalAudioPulse = peak
          }
      }
      onDispose {
          stopEngine?.invoke()
      }
  }

  // Auto-activate Music when Autopilot is toggled
  LaunchedEffect(isAutopilotActive) {
      if (isAutopilotActive) {
          isRetroMusicEnabled = true
      }
  }

  // Autopilot Agent Simulation Spine
  var autopilotPhase by remember { mutableStateOf(1) }
  LaunchedEffect(isAutopilotActive) {
      if (isAutopilotActive) {
          var step = 1
          while (true) {
              autopilotPhase = step
              val logMsg = when (step) {
                  1 -> "[PHASE 1: INSTANCE ENUMERATION] Scanned 6 tabs (Quantum | Finance | Intel). Mapped interdependencies."
                  2 -> "[PHASE 2: PARALLEL EXTRACTION] Extracted Bell state parameters. Analyzed Solscan wallet address anomalies."
                  3 -> "[PHASE 3: CROSS-DOMAIN SYNTHESIS] Matrix complete: quantum circuit ID correlated with gov AARO reports."
                  4 -> "[PHASE 4: SIMULTANEOUS ACTION] Generated Bell State entanglement circuit. Tested decoherence time."
                  5 -> "[PHASE 5: SAFETY & VERIFICATION] Checked sandbox constraints. Logged universal trace to spine."
                  6 -> "[PHASE 6: RECURSIVE IMPROVEMENT] Formulated PROMPT-Ω-V2. Saved pattern state to Memory DB."
                  else -> "[AUTOPILOT RUNNING] Continuing circuitronicyclical geometrigami alignment..."
              }
              systemAlerts = systemAlerts + logMsg
              Kernel.appendEvent(
                  DomainEvent(
                      type = "AUTOPILOT_PHASE_UPDATE",
                      domainId = "SystemScan",
                      payload = mapOf("phase" to step, "log" to logMsg)
                  )
              )
              
              step = if (step >= 6) 1 else step + 1
              kotlinx.coroutines.delay(4000)
          }
      }
  }

  LaunchedEffect(Unit) {
      while(true) {
          kotlinx.coroutines.delay(2000)
          latencyData = (latencyData.drop(1) + (80..200).random().toFloat())
          tokenData = (tokenData.drop(1) + (20..100).random().toFloat())
      }
  }

  LaunchedEffect(Unit) {
      Kernel.subscribeProjection(
          domainId = "SystemScan",
          initialState = mapOf("totalScans" to 0, "lastStatus" to "Idle")
      ) { state, event ->
          val currentScans = (state?.get("totalScans") as? Int) ?: 0
          when (event.type) {
              "DIAGNOSTIC_START" -> mapOf("totalScans" to currentScans + 1, "lastStatus" to "Scanning...")
              "DIAGNOSTIC_COMPLETE" -> mapOf("totalScans" to currentScans, "lastStatus" to "Complete: ${event.payload?.get("reply")}")
              "DIAGNOSTIC_ERROR" -> mapOf("totalScans" to currentScans, "lastStatus" to "Error: ${event.payload?.get("error")}")
              else -> state ?: mapOf()
          }
      }
      Kernel.subscribeProjection(
          domainId = "UserInterface",
          initialState = mapOf("activeWindows" to listOf<String>())
      ) { state, event ->
          val currentWindows = (state?.get("activeWindows") as? List<String>) ?: emptyList()
          when (event.type) {
              "LIFELINE_OPEN" -> mapOf("activeWindows" to currentWindows.plus(event.payload?.get("target")?.toString() ?: "Unknown").distinct())
              else -> state ?: mapOf()
          }
      }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(), 
      containerColor = BgColor,
      topBar = {
          Surface(
              modifier = Modifier.fillMaxWidth().statusBarsPadding(),
              color = CardColor,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
              tonalElevation = 8.dp
          ) {
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                          modifier = Modifier
                              .size(12.dp)
                              .background(
                                  if (isAutopilotActive) {
                                      Color(0xFF00FFCC).copy(alpha = 0.5f + globalAudioPulse * 0.5f)
                                  } else {
                                      Color(0xFFFF5252)
                                  },
                                  shape = RoundedCornerShape(6.dp)
                              )
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                          Text(
                              text = "EVEZ // PROMPT-Ω",
                              style = MaterialTheme.typography.titleSmall,
                              fontWeight = FontWeight.Bold,
                              color = TextColor,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                          )
                          Text(
                              text = if (isAutopilotActive) "SPINE COGNITION: AGENT AUTOPILOT ON" else "SPINE COGNITION: MANUAL CONTROLS",
                              style = MaterialTheme.typography.labelSmall,
                              color = if (isAutopilotActive) Color(0xFF00FFCC) else SubTextColor,
                              fontSize = 8.sp,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                          )
                      }
                  }

                  Row(
                      modifier = Modifier.height(24.dp),
                      horizontalArrangement = Arrangement.spacedBy(2.dp),
                      verticalAlignment = Alignment.Bottom
                  ) {
                      for (i in 0 until 5) {
                          val barHeight = if (isAutopilotActive) {
                              (10 + (i * 3) + (globalAudioPulse * 12).toInt()).coerceIn(10, 24).dp
                          } else {
                              (6 + (i * 3)).dp
                          }
                          Box(
                              modifier = Modifier
                                  .width(3.dp)
                                  .height(barHeight)
                                  .background(
                                      if (isAutopilotActive) {
                                          Color(0xFF8E24AA).copy(alpha = 0.7f + globalAudioPulse * 0.3f)
                                      } else {
                                          AccentColor
                                      },
                                      shape = RoundedCornerShape(1.dp)
                                  )
                          )
                      }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "AUTOPILOT",
                          style = MaterialTheme.typography.labelSmall,
                          color = if (isAutopilotActive) Color(0xFF00FFCC) else SubTextColor,
                          fontWeight = FontWeight.Bold,
                          fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                          modifier = Modifier.padding(end = 8.dp)
                      )
                      Switch(
                          checked = isAutopilotActive,
                          onCheckedChange = { active ->
                              isAutopilotActive = active
                              Kernel.appendEvent(
                                  DomainEvent(
                                      type = "AUTOPILOT_TOGGLE",
                                      domainId = "SystemScan",
                                      payload = mapOf("active" to active)
                                      )
                                  )
                              },
                          colors = SwitchDefaults.colors(
                              checkedThumbColor = Color(0xFF00FFCC),
                              checkedTrackColor = Color(0xFF083C3C),
                              uncheckedThumbColor = Color.Gray,
                              uncheckedTrackColor = Color.DarkGray
                          ),
                          modifier = Modifier.testTag("autopilot_switch")
                      )
                  }
              }
          }
      },
      floatingActionButton = {
          Column(horizontalAlignment = Alignment.End) {
              if (isFabMenuOpen) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("A16 Voice Listener", color = TextColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                      SmallFloatingActionButton(
                          onClick = {
                              isFabMenuOpen = false
                              showVoiceDialog = true
                          },
                          containerColor = CardColor,
                          contentColor = AccentColor
                      ) { Icon(Icons.Filled.Mic, contentDescription = "Voice") }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("Diagnostics Scan", color = TextColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                      SmallFloatingActionButton(
                          onClick = {
                              isFabMenuOpen = false
                              scope.launch {
                                  systemAlerts = systemAlerts + "Initiating deep diagnostics..."
                                  com.example.Kernel.appendEvent(com.example.DomainEvent("DIAGNOSTIC_START", "SystemScan", mapOf("target" to "GCP Storage")))
                                  try {
                                      val request = GenerateContentRequest(
                                          contents = listOf(Content(parts = listOf(Part("You are a system diagnostics AI for OpenClaw. Generate a 1 sentence status update for a manual deep scan across GCP resources.")), role = "user"))
                                      )
                                      val response = GeminiApi.service.generateContent(
                                          url = "v1beta/models/gemini-3.5-flash:generateContent",
                                          apiKey = BuildConfig.GEMINI_API_KEY,
                                          request = request
                                      )
                                      val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Scan complete."
                                      systemAlerts = systemAlerts + reply
                                      com.example.Kernel.appendEvent(com.example.DomainEvent("DIAGNOSTIC_COMPLETE", "SystemScan", mapOf("reply" to reply)))
                                  } catch (e: Exception) {
                                      systemAlerts = systemAlerts + "Error: API Unreachable."
                                      com.example.Kernel.appendEvent(com.example.DomainEvent("DIAGNOSTIC_ERROR", "SystemScan", mapOf("error" to "API Unreachable")))
                                  }
                              }
                          },
                          containerColor = CardColor,
                          contentColor = AccentColor
                      ) { Icon(Icons.Filled.Analytics, contentDescription = "Scan") }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("Clear Logs", color = TextColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                      SmallFloatingActionButton(
                          onClick = {
                              isFabMenuOpen = false
                              systemAlerts = listOf("Logs cleared.")
                          },
                          containerColor = CardColor,
                          contentColor = AccentColor
                      ) { Icon(Icons.Filled.Delete, contentDescription = "Clear Logs") }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("Refresh API", color = TextColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                      SmallFloatingActionButton(
                          onClick = {
                              isFabMenuOpen = false
                              systemAlerts = systemAlerts + "Refreshed connections."
                          },
                          containerColor = CardColor,
                          contentColor = AccentColor
                      ) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("AI Chat", color = TextColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                      SmallFloatingActionButton(
                          onClick = {
                              isFabMenuOpen = false
                              isChatOpen = true
                              com.example.Kernel.appendEvent(com.example.DomainEvent("LIFELINE_OPEN", "UserInterface", mapOf("target" to "AIChat")))
                          },
                          containerColor = CardColor,
                          contentColor = AccentColor
                      ) { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("Quantum Browser", color = TextColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                      SmallFloatingActionButton(
                          onClick = {
                              isFabMenuOpen = false
                              isBrowserOpen = true
                              com.example.Kernel.appendEvent(com.example.DomainEvent("LIFELINE_OPEN", "UserInterface", mapOf("target" to "QuantumBrowser")))
                          },
                          containerColor = CardColor,
                          contentColor = AccentColor
                      ) { Icon(Icons.Filled.Public, contentDescription = "Browser") }
                  }
                  Spacer(modifier = Modifier.height(16.dp))
              }
              FloatingActionButton(
                  onClick = { isFabMenuOpen = !isFabMenuOpen },
                  containerColor = AccentColor,
                  contentColor = Color(0xFF003355)
              ) {
                  Icon(if (isFabMenuOpen) Icons.Filled.Close else Icons.Filled.Add, contentDescription = "Menu")
              }
          }
      }
  ) { innerPadding ->
    val loopState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = 1000000)
    androidx.compose.foundation.lazy.LazyColumn(
        state = loopState,
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
    ) {
      items(count = Int.MAX_VALUE) { index ->
          val itemInfo by remember { androidx.compose.runtime.derivedStateOf { loopState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } } }
          val offsetCenter = itemInfo?.let { info ->
              val viewportCenter = loopState.layoutInfo.viewportEndOffset / 2f
              val itemCenter = info.offset + info.size / 2f
              (itemCenter - viewportCenter) / viewportCenter
          } ?: 0f
          
          val absoluteOffset = offsetCenter
          val scale = 1f - kotlin.math.abs(absoluteOffset) * 0.15f
          val alpha = 1f - kotlin.math.abs(absoluteOffset) * 0.4f
          
          val mod = Modifier
                  .fillMaxWidth()
                  .graphicsLayer {
                      rotationX = -absoluteOffset * 65f
                      scaleX = scale
                      scaleY = scale
                      this.alpha = alpha
                      cameraDistance = 12f * density
                  }
                  .padding(vertical = 12.dp)

          when (index % 13) {
              0 -> Column(modifier = mod) {
                  Spacer(modifier = Modifier.height(16.dp))
                  
                  // Title Area with retro theme toggle
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Column(modifier = Modifier.weight(1f)) {
                          Text(
                              text = "EVEZ COGNITIVE COCKPIT",
                              style = MaterialTheme.typography.headlineSmall,
                              color = TextColor,
                              fontWeight = FontWeight.Bold,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                          )
                          Text(
                              text = "N64 • GAMECUBE • PLAYSTATION SCI-FI OSINT INTERFACE",
                              style = MaterialTheme.typography.labelSmall,
                              color = AccentColor,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                          )
                      }
                      IconButton(
                          onClick = { 
                              isDarkMode = !isDarkMode 
                              Kernel.appendEvent(
                                  DomainEvent(
                                      type = "THEME_TOGGLE",
                                      domainId = "UserInterface",
                                      payload = mapOf("darkMode" to isDarkMode)
                                  )
                              )
                          },
                          modifier = Modifier
                              .size(44.dp)
                              .background(CardColor, RoundedCornerShape(12.dp))
                              .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                              .testTag("theme_toggle_btn")
                      ) {
                          Icon(
                              imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                              contentDescription = "Toggle Theme Mode",
                              tint = AccentColor
                          )
                      }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // RETRO MEMORY CARD AND SYSTEM STATUS (PSX/N64 Vibes!)
                  Card(
                      modifier = Modifier.fillMaxWidth(),
                      colors = CardDefaults.cardColors(containerColor = CardColor),
                      shape = RoundedCornerShape(16.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                  ) {
                      Column(modifier = Modifier.padding(16.dp)) {
                          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                              Text("SYSTEM DIAGNOSTICS [AARO-NHI-v1]", color = Color(0xFF00FFCC), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold)
                              Text("PORT: 19001", color = SubTextColor, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          }
                          Spacer(modifier = Modifier.height(8.dp))
                          Text("• MEM-CARD 1: OK [64 BLOCKS FREE - FORENSIC SAVE ON]", color = TextColor, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("• MEM-CARD 2: OK [SPINE CONVERGENCE MANIFOLD CONNECTED]", color = TextColor, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("• ENGINE CLOCK: 433 MHz (R4300i MIPs Analog)", color = TextColor, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                          Text("• GROUNDING SCANNERS: GOOGLE SEARCH & MAPS Grounded", color = TextColor, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                      }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // SPINNING ALIEN UFO/SAUCER & PLANETARY RING RETRO RENDERING
                  Card(
                      modifier = Modifier.fillMaxWidth(),
                      colors = CardDefaults.cardColors(containerColor = Color.Black),
                      shape = RoundedCornerShape(16.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                  ) {
                      val rotationAngle = rememberInfiniteTransition().animateFloat(
                          initialValue = 0f,
                          targetValue = 360f,
                          animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
                          label = "ufo_spin"
                      )
                      val ringBounce = rememberInfiniteTransition().animateFloat(
                          initialValue = -10f,
                          targetValue = 10f,
                          animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                          label = "ring_bounce"
                      )

                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .height(180.dp),
                          contentAlignment = Alignment.Center
                      ) {
                          Canvas(modifier = Modifier.fillMaxSize()) {
                              val cx = size.width / 2f
                              val cy = size.height / 2f
                              val ufoWidth = 100.dp.toPx() + (globalAudioPulse * 40.dp.toPx())
                              val ufoHeight = 35.dp.toPx() + (globalAudioPulse * 15.dp.toPx())

                              // Draw Retro Starfield Background
                              for (i in 0 until 15) {
                                  val sx = (cx * (1f + kotlin.math.sin(i.toDouble()).toFloat() * 0.9f))
                                  val sy = (cy * (1f + kotlin.math.cos((i * 2).toDouble()).toFloat() * 0.9f))
                                  drawCircle(
                                      color = Color.White.copy(alpha = 0.3f + globalAudioPulse * 0.7f),
                                      radius = 2.dp.toPx(),
                                      center = Offset(sx, sy)
                                  )
                              }

                              // Draw Planetary / Orbit Rings (Retro Isometric Wireframe)
                              drawOval(
                                  color = Color(0xFF8E24AA).copy(alpha = 0.6f),
                                  topLeft = Offset(cx - ufoWidth * 1.3f, cy - ufoHeight * 1.5f + ringBounce.value),
                                  size = Size(ufoWidth * 2.6f, ufoHeight * 3f),
                                  style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                              )

                              // Draw Spinning UFO Alien Craft (N64 Low-Poly style)
                              // Dome
                              drawPath(
                                  path = Path().apply {
                                      moveTo(cx - ufoWidth * 0.3f, cy)
                                      cubicTo(
                                          cx - ufoWidth * 0.2f, cy - ufoHeight * 1.3f,
                                          cx + ufoWidth * 0.2f, cy - ufoHeight * 1.3f,
                                          cx + ufoWidth * 0.3f, cy
                                      )
                                      close()
                                  },
                                  color = Color(0xFF00FFCC).copy(alpha = 0.7f),
                                  style = Stroke(width = 3.dp.toPx())
                              )

                              // Saucer Body
                              drawOval(
                                  color = Color(0xFF00C8FF),
                                  topLeft = Offset(cx - ufoWidth / 2f, cy - ufoHeight / 2f),
                                  size = Size(ufoWidth, ufoHeight),
                                  style = Stroke(width = 4.dp.toPx())
                              )

                              // Inner Low-poly details (spinning lines)
                              val rad = rotationAngle.value * Math.PI / 180f
                              val lineXOffset = (ufoWidth / 2f) * kotlin.math.cos(rad).toFloat()
                              drawLine(
                                  color = Color.White.copy(alpha = 0.8f),
                                  start = Offset(cx - lineXOffset, cy),
                                  end = Offset(cx + lineXOffset, cy),
                                  strokeWidth = 3.dp.toPx()
                              )

                              // Propulsion Beam (pulsing to audio rhythm!)
                              val beamHeight = 40.dp.toPx() + (globalAudioPulse * 60.dp.toPx())
                              drawPath(
                                  path = Path().apply {
                                      moveTo(cx - ufoWidth * 0.15f, cy + ufoHeight * 0.4f)
                                      lineTo(cx + ufoWidth * 0.15f, cy + ufoHeight * 0.4f)
                                      lineTo(cx + ufoWidth * 0.3f, cy + ufoHeight * 0.4f + beamHeight)
                                      lineTo(cx - ufoWidth * 0.3f, cy + ufoHeight * 0.4f + beamHeight)
                                      close()
                                  },
                                  color = Color(0xFF00FFCC).copy(alpha = 0.15f + globalAudioPulse * 0.35f)
                              )
                          }

                          // Retro Console Overlay Texts
                          Column(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .padding(12.dp),
                              verticalArrangement = Arrangement.SpaceBetween
                          ) {
                              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                  Text("CRT SCAN_MODE: 240p", color = Color(0xFFFF5252).copy(alpha = 0.8f), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                  Text("LOCK: AARO/A-16", color = Color(0xFF00FFCC), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                              }
                              Text(
                                  text = "ALIEN TELEMETRY VECTOR STREAM",
                                  modifier = Modifier.align(Alignment.CenterHorizontally),
                                  color = Color.White.copy(alpha = 0.5f),
                                  fontSize = 8.sp,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                              )
                          }
                      }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // RETRO AUDIO & MUSIC CONTROLLER PANEL
                  Card(
                      modifier = Modifier.fillMaxWidth(),
                      colors = CardDefaults.cardColors(containerColor = CardColor),
                      shape = RoundedCornerShape(16.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                  ) {
                      Column(modifier = Modifier.padding(16.dp)) {
                          Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically
                          ) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                  Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = Color(0xFF8E24AA), modifier = Modifier.size(20.dp))
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text(
                                      text = "FM CHIPTUNE SYNTH MUSIC ENGINE",
                                      color = TextColor,
                                      fontWeight = FontWeight.Bold,
                                      fontSize = 12.sp,
                                      fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                  )
                              }
                              Switch(
                                  checked = isRetroMusicEnabled,
                                  onCheckedChange = { isRetroMusicEnabled = it },
                                  colors = SwitchDefaults.colors(
                                      checkedThumbColor = Color(0xFF8E24AA),
                                      checkedTrackColor = Color(0xFF3B0F3F)
                                  ),
                                  modifier = Modifier.testTag("retro_music_switch")
                              )
                          }
                          Spacer(modifier = Modifier.height(8.dp))
                          Text(
                              text = if (isRetroMusicEnabled) "PLAYING: COGNITIVE SPACE ARPEGGIATOR [CD-QUALITY 11kHz MONO]" else "STANDBY: COGNITIVE ARPEGGIATOR STOPPED",
                              color = if (isRetroMusicEnabled) Color(0xFF00FFCC) else SubTextColor,
                              fontSize = 10.sp,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                          )
                          Spacer(modifier = Modifier.height(12.dp))
                          
                          // Dynamic Color Transition / Pulse Bar
                          Row(
                              modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black),
                              verticalAlignment = Alignment.CenterVertically
                          ) {
                              Box(
                                  modifier = Modifier
                                      .fillMaxHeight()
                                      .fillMaxWidth(globalAudioPulse)
                                      .background(
                                          androidx.compose.ui.graphics.Brush.horizontalGradient(
                                              listOf(Color(0xFF8E24AA), Color(0xFF00FFCC), Color(0xFF00C8FF))
                                          )
                                      )
                              )
                          }
                      }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // AUTOPILOT SPINE TRUNK MANIFOLD (PHASE TRAIL)
                  if (isAutopilotActive) {
                      Card(
                          modifier = Modifier.fillMaxWidth(),
                          colors = CardDefaults.cardColors(containerColor = CardColor),
                          shape = RoundedCornerShape(16.dp),
                          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFCC))
                      ) {
                          Column(modifier = Modifier.padding(16.dp)) {
                              Text(
                                  text = "SPINE RECURSIVE AUTOPILOT MANIFOLD",
                                  color = Color(0xFF00FFCC),
                                  fontWeight = FontWeight.Bold,
                                  fontSize = 12.sp,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                              )
                              Spacer(modifier = Modifier.height(8.dp))
                              
                              Row(
                                  modifier = Modifier.fillMaxWidth(),
                                  horizontalArrangement = Arrangement.SpaceBetween
                              ) {
                                  for (ph in 1..6) {
                                      val isCurrent = autopilotPhase == ph
                                      val ledColor = if (isCurrent) Color(0xFF00FFCC) else Color(0xFF1E2A2B)
                                      val textColor = if (isCurrent) Color.White else SubTextColor
                                      Column(
                                          horizontalAlignment = Alignment.CenterHorizontally,
                                          modifier = Modifier.weight(1f)
                                      ) {
                                          Box(
                                              modifier = Modifier
                                                  .size(16.dp)
                                                  .background(ledColor, RoundedCornerShape(8.dp))
                                                  .border(1.dp, if (isCurrent) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                          )
                                          Spacer(modifier = Modifier.height(4.dp))
                                          Text(
                                              text = "P$ph",
                                              color = textColor,
                                              fontSize = 8.sp,
                                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                              fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                          )
                                      }
                                  }
                              }
                          }
                      }
                  }
              }
              1 -> Box(modifier = mod) { EvezOsIntegration() }
              2 -> Column(modifier = mod) {
                  CloudResourceOrchestrationToolbar(database = database)
                  Spacer(modifier = Modifier.height(16.dp))
                  AutomatedResourceInventory(database = database)
              }
              3 -> Box(modifier = mod) { ThreatIntelligenceRadar() }
              4 -> Box(modifier = mod) {
                  Box(
          modifier = Modifier
              .fillMaxWidth()
              .height(450.dp)
      ) {
          com.example.ui.OrbitalTelemetryChart(
              modifier = Modifier.fillMaxSize()
          )
          com.example.ui.RubiksCubeNavigation(
              modifier = Modifier.align(Alignment.Center),
              onFaceClick = { faceId ->
                  Kernel.appendEvent(com.example.DomainEvent("RUBIKS_NAV_CLICK", "UserInterface", mapOf("target" to faceId)))
                  when (faceId) {
                      "Gemini" -> {
                          isFabMenuOpen = false
                          isChatOpen = true
                      }
                      "OpenClaw" -> {
                          isFabMenuOpen = true
                      }
                  }
              },
              onFaceDoubleClick = { faceId ->
                  Kernel.appendEvent(com.example.DomainEvent("RUBIKS_NAV_DOUBLECLICK", "UserInterface", mapOf("target" to faceId)))
                  widenCognitionKeys = faceId
              }
          )
              }
              }
              5 -> Box(modifier = mod) { com.example.ui.KernelDashboardUI() }
              6 -> Box(modifier = mod) { com.example.ui.DeveloperParadiseDashboard() }
              7 -> Box(modifier = mod) {
                  Column {
                      Text(text = "Identity & Persistence", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      val isLoggedIn = firebaseUser != null || simulatedEmail != null
                      val connectionLabel = if (isLoggedIn) {
                          if (isFallbackMode) "Simulated User:\n$simulatedEmail" else "Firebase User:\n$simulatedEmail"
                      } else {
                          "Tap to Sign In"
                      }
                      val syncLabel = if (isLoggedIn) {
                          if (isFallbackMode) "Local Sandbox Session" else "Firestore Cloud Synced"
                      } else {
                          "Firebase Auth Disconnected"
                      }
                      Card(
                          modifier = Modifier.fillMaxWidth().clickable { showAuthDialog = true },
                          colors = CardDefaults.cardColors(containerColor = CardColor),
                          shape = RoundedCornerShape(24.dp),
                          border = BorderStroke(1.dp, BorderColor)
                      ) {
                          Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                              Icon(if (isLoggedIn) Icons.Filled.AccountCircle else Icons.Filled.Storage, contentDescription = null, tint = AccentColor, modifier = Modifier.size(40.dp))
                              Spacer(modifier = Modifier.width(16.dp))
                              Column(modifier = Modifier.weight(1f)) {
                                  Text(text = connectionLabel, style = MaterialTheme.typography.bodyMedium, color = AccentColor, fontWeight = FontWeight.Bold)
                                  Text(text = syncLabel, style = MaterialTheme.typography.bodySmall, color = SubTextColor)
                              }
                              if (isLoggedIn) {
                                  Container(color = if (isFallbackMode) Color(0xFF423B0F) else Color(0xFF003355), textColor = if (isFallbackMode) Color(0xFFFFD54F) else AccentColor, text = if (isFallbackMode) "SANDBOX" else "SECURE")
                              } else {
                                  Container(color = BorderColor, textColor = SubTextColor, text = "STANDBY")
                              }
                          }
                      }
                  }
              }
              8 -> Box(modifier = mod) {
                  Column {
                      Text(text = "Model Token Hub", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = CardColor),
          shape = RoundedCornerShape(32.dp),
          border = BorderStroke(1.dp, BorderColor)
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
            providers.forEachIndexed { index, provider ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val updated = provider.copy(isActive = !provider.isActive)
                            providers = providers.toMutableList().apply { set(index, updated) }
                            FirebaseAuthManager.pushProviderToCloud(updated)
                        }
                ) {
                    ProviderRow(icon = provider.icon, name = provider.name, keyPreview = provider.keyPreview, iconColor = provider.iconColor, isActive = provider.isActive)
                }
                if (index < providers.size - 1) Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { showAddProviderDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BorderColor, contentColor = TextColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = "+ AUTOMATED PROVIDER CONFIG", fontWeight = FontWeight.Bold)
             }
          }
        }
      }
    }
    9 -> Box(modifier = mod) { com.example.ui.CognitosegrephonicsDna() }
    10 -> Box(modifier = mod) { com.example.ui.KociembaSelfCodingOptimizer() }
    11 -> Box(modifier = mod) { com.example.ui.SensoryAudioGeometries() }
    12 -> Box(modifier = mod) { OpenClawGameSharkCheatCodeCenter() }
  }
}
}
    
if (showAddProviderDialog) {
          AlertDialog(
              onDismissRequest = { showAddProviderDialog = false },
              containerColor = CardColor,
              titleContentColor = TextColor,
              textContentColor = TextColor,
              title = { Text("Configure New Provider") },
              text = {
                  Column {
                      OutlinedTextField(
                          value = newProviderName,
                          onValueChange = { newProviderName = it },
                          label = { Text("Provider Name") },
                          colors = OutlinedTextFieldDefaults.colors(
                              unfocusedTextColor = TextColor, focusedTextColor = TextColor,
                              focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                              focusedLabelColor = AccentColor, unfocusedLabelColor = SubTextColor
                          )
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      OutlinedTextField(
                          value = newProviderKey,
                          onValueChange = { newProviderKey = it },
                          label = { Text("API Key") },
                          colors = OutlinedTextFieldDefaults.colors(
                              unfocusedTextColor = TextColor, focusedTextColor = TextColor,
                              focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                              focusedLabelColor = AccentColor, unfocusedLabelColor = SubTextColor
                          )
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      OutlinedTextField(
                          value = newProviderEndpoint,
                          onValueChange = { newProviderEndpoint = it },
                          label = { Text("Endpoint URL (Optional)") },
                          colors = OutlinedTextFieldDefaults.colors(
                              unfocusedTextColor = TextColor, focusedTextColor = TextColor,
                              focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                              focusedLabelColor = AccentColor, unfocusedLabelColor = SubTextColor
                          )
                      )
                  }
              },
              confirmButton = {
                  Button(
                      onClick = {
                          if (newProviderName.isNotBlank() && newProviderKey.isNotBlank()) {
                              val preview = if (newProviderKey.length > 10) newProviderKey.substring(0, 10) + "..." else newProviderKey
                              val newProvider = ProviderData(
                                  icon = newProviderName.firstOrNull()?.uppercase() ?: "?",
                                  name = newProviderName,
                                  keyPreview = preview,
                                  iconColor = Color(0xFFE2E2E6),
                                  isActive = true
                              )
                              providers = providers + newProvider
                              FirebaseAuthManager.pushProviderToCloud(newProvider)
                          }
                          showAddProviderDialog = false
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355))
                  ) {
                      Text("Add Provider", fontWeight = FontWeight.Bold)
                  }
              },
              dismissButton = {
                  TextButton(onClick = { showAddProviderDialog = false }) {
                      Text("Cancel", color = SubTextColor)
                  }
              }
          )
      }
  }

  if (isChatOpen) {
      GeminiChatAssistant(onClose = { isChatOpen = false })
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .then(if (!isBrowserOpen) Modifier.offset(x = (-10000).dp) else Modifier) // Hide off-screen if closed to persist webview state
  ) {
      QuantumBrowserDialog(database = database, onClose = { isBrowserOpen = false })
  }

  if (showVoiceDialog) {
      com.example.ui.AppVoiceAgentDialog(onClose = { showVoiceDialog = false })
  }

  if (showAuthDialog) {
      OpenClawAuthDialog(onDismiss = { showAuthDialog = false }, database = database, onUpdateProviders = { providers = it })
  }

  if (widenCognitionKeys != null) {
      com.example.ui.UniversalCognitionKeys(
          faceId = widenCognitionKeys!!,
          onClose = { widenCognitionKeys = null }
      )
  }
}

@Composable
fun GeminiChatAssistant(onClose: () -> Unit) {
    val systemState by Kernel.eventLog.collectAsStateWithLifecycle()
    var messages by remember { mutableStateOf(listOf("System: Online. Gemini Intelligence initialized.", "System: Thinking mode ready. Low-latency relay ready. Search grounding connected.")) }
    var input by remember { mutableStateOf("") }
    var isThinkingRequested by remember { mutableStateOf(false) }
    var isLowLatencyRequested by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Gemini OpenClaw Assistant", style = MaterialTheme.typography.titleMedium, color = AccentColor, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = SubTextColor)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isThinkingRequested,
                        onClick = { isThinkingRequested = !isThinkingRequested; if(isThinkingRequested) isLowLatencyRequested = false },
                        label = { Text("High Thinking") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF003355), selectedLabelColor = AccentColor)
                    )
                    FilterChip(
                        selected = isLowLatencyRequested,
                        onClick = { isLowLatencyRequested = !isLowLatencyRequested; if(isLowLatencyRequested) isThinkingRequested = false },
                        label = { Text("Low Latency") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF003355), selectedLabelColor = AccentColor)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(messages) { msg ->
                        Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = TextColor, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Command Google Cloud...", color = SubTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = TextColor, focusedTextColor = TextColor,
                            focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                messages = messages + ("User: $input")
                                val currentInput = input
                                input = ""
                                Kernel.appendEvent(DomainEvent("AGENT_CHAT_MESSAGE", "GeminiAgent", mapOf("text" to currentInput, "role" to "user")))
                                scope.launch {
                                    messages = messages + ("System: Processing securely via REST...")
                                    try {
                                        val modelName = if (isThinkingRequested) "gemini-3.1-pro-preview" else if (isLowLatencyRequested) "gemini-3.1-flash-lite-preview" else "gemini-3.5-flash"
                                        val url = "v1beta/models/$modelName:generateContent"
                                        
                                        val config = if (isThinkingRequested) GenerationConfig(thinkingConfig = ThinkingConfig("HIGH")) else null
                                        val tools = if (!isThinkingRequested && !isLowLatencyRequested) listOf(Tool(GoogleSearch())) else null
                                        
                                        val request = GenerateContentRequest(
                                            systemInstruction = Content(parts = listOf(Part("You are the Gemini OpenClaw agent. You have access to real-time events. Latest system stats: ${systemState.takeLast(10).map{it.type}}")), role = "system"),
                                            contents = listOf(Content(parts = listOf(Part(currentInput)), role = "user")),
                                            generationConfig = config,
                                            tools = tools
                                        )
                                        
                                        val response = GeminiApi.service.generateContent(
                                            url = url,
                                            apiKey = BuildConfig.GEMINI_API_KEY,
                                            request = request
                                        )
                                        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                                        messages = messages + ("Gemini ($modelName): $reply")
                                        Kernel.appendEvent(DomainEvent("AGENT_CHAT_MESSAGE", "GeminiAgent", mapOf("text" to reply, "role" to "assistant")))
                                    } catch (e: Exception) {
                                        val errorMsg = "Error: ${e.message}"
                                        messages = messages + errorMsg
                                        Kernel.appendEvent(DomainEvent("AGENT_ERROR", "GeminiAgent", mapOf("error" to errorMsg)))
                                    }
                                }
                            }
                        },
                        containerColor = AccentColor,
                        contentColor = Color(0xFF003355)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun Container(color: Color, textColor: Color, text: String) {
    Surface(color = color, shape = RoundedCornerShape(16.dp)) {
        Text(text = text, color = textColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricsBox(modifier: Modifier = Modifier, label: String, value: String, valueColor: Color = TextColor) {
    Surface(color = BorderColor, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = SubTextColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProviderRow(icon: String, name: String, keyPreview: String, iconColor: Color, isActive: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Surface(color = if (isActive) iconColor else BorderColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = icon, color = if (isActive) Color.White else SubTextColor, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = if (isActive) TextColor else SubTextColor, fontWeight = FontWeight.Bold)
            Text(text = keyPreview, style = MaterialTheme.typography.labelSmall, color = SubTextColor)
        }
        if (isActive) {
            Surface(color = Color(0xFF00FF00), shape = RoundedCornerShape(50), modifier = Modifier.size(8.dp)) {}
        } else {
            Surface(color = SubTextColor, shape = RoundedCornerShape(50), modifier = Modifier.size(8.dp)) {}
        }
    }
}

data class Resource(val name: String, val url: String)

data class ProviderData(
    val icon: String,
    val name: String,
    val keyPreview: String,
    val iconColor: Color,
    val isActive: Boolean
)

@Composable
fun MetricChart(title: String, dataPoints: List<Float>, color: Color, isBarChart: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = SubTextColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.isEmpty()) return@Canvas
                val maxPoint = dataPoints.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                val count = if (isBarChart) dataPoints.size else (dataPoints.size - 1).coerceAtLeast(1)
                val stepX = size.width / count
                
                if (isBarChart) {
                    val barWidth = stepX * 0.6f
                    dataPoints.forEachIndexed { index, value ->
                        val barHeight = (value / maxPoint) * size.height
                        val startX = (index * stepX) + (stepX - barWidth) / 2
                        drawRect(
                            color = color,
                            topLeft = Offset(startX, size.height - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                } else {
                    val path = Path()
                    dataPoints.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - ((value / maxPoint) * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun EvezOsIntegration() {
    var evezNexusActive by remember { mutableStateOf(true) }
    var evezartEngineActive by remember { mutableStateOf(false) }
    var evezxSecurityActive by remember { mutableStateOf(false) }
    var evezVclActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Text(text = "Evez-OS Core Integrations", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Storage, contentDescription = null, tint = Color(0xFFFF0055))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "evezart / evez-os", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Container(
                    color = if (evezNexusActive) Color(0xFF550022) else BorderColor,
                    textColor = if (evezNexusActive) Color(0xFFFF0055) else SubTextColor,
                    text = if (evezNexusActive) "KERN ONLINE" else "OFFLINE"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Acquired Systems (GitHub/Evezart & Evezx)", style = MaterialTheme.typography.labelMedium, color = SubTextColor)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subsystem 1
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Evez-666 Logic Gate", color = if (evezNexusActive) TextColor else SubTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Primary OpenClaw Router", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(
                    checked = evezNexusActive,
                    onCheckedChange = { 
                        evezNexusActive = it 
                        com.example.Kernel.appendEvent(
                            com.example.DomainEvent(
                                type = "EVEZ_GATE_TOGGLE",
                                domainId = "EvezOsManager",
                                payload = mapOf("active" to it, "system" to "Evez-666 Logic Gate")
                            )
                        )
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF0055), checkedTrackColor = Color(0xFF550022))
                )
            }
            // Subsystem 2
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Evezart UI / Graphic Engine", color = if (evezartEngineActive) TextColor else SubTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "God Engine Overlays", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(
                    checked = evezartEngineActive,
                    onCheckedChange = { 
                        evezartEngineActive = it
                        if(it) evezNexusActive = true 
                        com.example.Kernel.appendEvent(
                            com.example.DomainEvent(
                                type = "EVEZART_ENGINE_TOGGLE",
                                domainId = "EvezOsManager",
                                payload = mapOf("active" to it, "system" to "Evezart UI / Graphic Engine")
                            )
                        )
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF0055), checkedTrackColor = Color(0xFF550022))
                )
            }
            // Subsystem 3
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Evezx Quantum Security", color = if (evezxSecurityActive) TextColor else SubTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Deep system protections", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(
                    checked = evezxSecurityActive,
                    onCheckedChange = { 
                        evezxSecurityActive = it
                        if(it) evezNexusActive = true 
                        com.example.Kernel.appendEvent(
                            com.example.DomainEvent(
                                type = "EVEZX_SECURITY_TOGGLE",
                                domainId = "EvezOsManager",
                                payload = mapOf("active" to it, "system" to "Evezx Quantum Security")
                            )
                        )
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF0055), checkedTrackColor = Color(0xFF550022))
                )
            }
            // Subsystem 4 (Evez-VCL Visual Cognition Layers)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Evez-VCL Visual Cognition Layers", color = if (evezVclActive) TextColor else SubTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Alpha/Beta optical pathways & neural overlays", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(
                    checked = evezVclActive,
                    onCheckedChange = { 
                        evezVclActive = it
                        if(it) evezNexusActive = true 
                        com.example.Kernel.appendEvent(
                            com.example.DomainEvent(
                                type = "EVEZ_VCL_TOGGLE",
                                domainId = "EvezOsManager",
                                payload = mapOf("active" to it, "system" to "Evez-VCL Visual Cognition Layers")
                            )
                        )
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF0055), checkedTrackColor = Color(0xFF550022))
                )
            }

            AnimatedVisibility(
                visible = evezVclActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    com.example.ui.EvezVisualCognitionPanel(isActive = evezVclActive)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    evezNexusActive = true
                    evezartEngineActive = true
                    evezxSecurityActive = true
                    evezVclActive = true
                    com.example.Kernel.appendEvent(
                        com.example.DomainEvent(
                            type = "EVEZ_FULL_ACQUIRE",
                            domainId = "EvezOsManager",
                            payload = mapOf("status" to "ALL_ONLINE")
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1929), contentColor = Color(0xFFFF0055)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "ACQUIRE & INIT ALL EVEZ SYSTEMS", fontWeight = FontWeight.Bold)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Galaxy A16 Base System Kernel Wrapper
    Text(text = "Base System Kernel Wrapper (Galaxy A16)", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            var freqOverride by remember { mutableStateOf(false) }
            var memCompr by remember { mutableStateOf(true) }
            var ioScheduler by remember { mutableStateOf("CFQ") }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF00FF00))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "/dev/openclaw_kern", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Container(
                    color = Color(0xFF003300),
                    textColor = Color(0xFF00FF00),
                    text = "A16 HOOK ACTIVE"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // CPUFreq
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "CPU Freq Override", color = TextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = if (freqOverride) "performance_governor enforced" else "schedutil governor default", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(
                    checked = freqOverride,
                    onCheckedChange = { freqOverride = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF00), checkedTrackColor = Color(0xFF003300))
                )
            }
            // ZRAM
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "ZRAM Memory Compression", color = TextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = if (memCompr) "LZ4 compression active" else "Compression disabled", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(
                    checked = memCompr,
                    onCheckedChange = { memCompr = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF00), checkedTrackColor = Color(0xFF003300))
                )
            }
            // Scheduler
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "I/O Scheduler", color = TextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Current: $ioScheduler", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { ioScheduler = "NOOP" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (ioScheduler == "NOOP") AccentColor else Color.Transparent, contentColor = if(ioScheduler == "NOOP") Color.Black else TextColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                        border = BorderStroke(1.dp, AccentColor)
                    ) { Text("NOOP", fontSize = 10.sp) }
                     Button(
                        onClick = { ioScheduler = "CFQ" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (ioScheduler == "CFQ") AccentColor else Color.Transparent, contentColor = if(ioScheduler == "CFQ") Color.Black else TextColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                        border = BorderStroke(1.dp, AccentColor)
                    ) { Text("CFQ", fontSize = 10.sp) }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        // simulate kernel flush
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003300), contentColor = Color(0xFF00FF00)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "FLUSH KERNEL BUFFERS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AutomatedResourceInventory(database: AppDatabase) {
    var isScanning by remember { mutableStateOf(false) }
    val resourcesFound by database.resourceStateDao().getAllStates().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    val activeUser by FirebaseAuthManager.simulatedUserEmail.collectAsStateWithLifecycle()
    val isFallbackMode by FirebaseAuthManager.isFallbackMode.collectAsStateWithLifecycle()

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customStatus by remember { mutableStateOf("Provisioned") }
    var customMetrics by remember { mutableStateOf("Optimal") }

    Text(text = "Automated Resource Inventory", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Storage, contentDescription = null, tint = AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Auto-Discovery", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showAddCustomDialog = true }) {
                        Text("+ ADD CUSTOM", color = AccentColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Container(
                        color = if (isScanning) Color(0xFF003355) else BorderColor,
                        textColor = if (isScanning) AccentColor else SubTextColor,
                        text = if (isScanning) "SCANNING" else "READY"
                    )
                }
            }
            
            if (activeUser != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (isFallbackMode) Color(0xFF332B00) else Color(0xFF0D253F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (isFallbackMode) Color(0xFFFFD54F) else AccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFallbackMode) "Secured locally as sandbox user: $activeUser" else "Authenticated! Cloud sync active for $activeUser",
                            color = if (isFallbackMode) Color(0xFFFFD54F) else AccentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (resourcesFound.isEmpty()) {
                Text("No resources scanned yet. Initiate discovery.", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
            } else {
                resourcesFound.forEach { res ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0x22000000), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = res.name, style = MaterialTheme.typography.bodyMedium, color = TextColor, fontWeight = FontWeight.Bold)
                                Text(text = "Status: ${res.status} | Metric: ${res.metrics}", style = MaterialTheme.typography.labelSmall, color = SubTextColor)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Cycle Status
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val cycleStatus = when (res.status) {
                                            "Active" -> "Warning"
                                            "Warning" -> "Disabled"
                                            else -> "Active"
                                        }
                                        val cycleMetrics = when (res.metrics) {
                                            "Normal" -> "High Latency"
                                            "High Latency" -> "Optimal"
                                            else -> "Normal"
                                        }
                                        val updated = res.copy(status = cycleStatus, metrics = cycleMetrics)
                                        database.resourceStateDao().insertState(updated)
                                        FirebaseAuthManager.pushResourceToCloud(updated, scope)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Cycle Status", tint = AccentColor, modifier = Modifier.size(16.dp))
                            }
                            
                            // Delete
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        database.resourceStateDao().deleteStateById(res.id)
                                        FirebaseAuthManager.deleteResourceFromCloud(res.id)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete config", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!isScanning) {
                        isScanning = true
                        scope.launch {
                            try {
                                val request = GenerateContentRequest(
                                    contents = listOf(Content(parts = listOf(Part("You are an automated resource discovery system for Evez-OS and Google Cloud. Generate a realistic comma-separated list of 3 fictional cloud or edge resources you just discovered without introductory text.")), role = "user"))
                                )
                                val response = GeminiApi.service.generateContent(
                                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                                    apiKey = BuildConfig.GEMINI_API_KEY,
                                    request = request
                                )
                                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Compute VCM, Edge Node Alpha, Database Cluster"
                                val newResources = reply.split(",").map { it.trim().trim('*', ' ', '\n', '\r', '"') }
                                newResources.forEachIndexed { index, name ->
                                    if (name.isNotBlank()) {
                                        val newState = ResourceState(
                                            id = "res_${System.currentTimeMillis()}_$index",
                                            name = name,
                                            status = "Active",
                                            metrics = "Normal"
                                        )
                                        database.resourceStateDao().insertState(newState)
                                        FirebaseAuthManager.pushResourceToCloud(newState, scope)
                                    }
                                }
                            } catch (e: Exception) {
                                val fallbackName = "Compute_Cluster_${(10..99).random()}"
                                val newState = ResourceState(
                                    id = "res_${System.currentTimeMillis()}",
                                    name = fallbackName,
                                    status = "Active",
                                    metrics = "Normal"
                                )
                                database.resourceStateDao().insertState(newState)
                                FirebaseAuthManager.pushResourceToCloud(newState, scope)
                            }
                            isScanning = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isScanning) "DISCOVERING..." else "RUN RESOURCE & INVENTORY SCAN", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            containerColor = CardColor,
            titleContentColor = TextColor,
            textContentColor = TextColor,
            title = { Text("Configure Custom OpenClaw Resource", fontWeight = FontWeight.Bold, color = AccentColor) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Resource Configuration Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor, unfocusedTextColor = TextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customStatus,
                        onValueChange = { customStatus = it },
                        label = { Text("Initial System Status") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor, unfocusedTextColor = TextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customMetrics,
                        onValueChange = { customMetrics = it },
                        label = { Text("Telemetry Metric Tag") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor, unfocusedTextColor = TextColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customName.isNotBlank()) {
                            val targetState = ResourceState(
                                id = "res_custom_${System.currentTimeMillis()}",
                                name = customName,
                                status = customStatus,
                                metrics = customMetrics
                            )
                            scope.launch {
                                database.resourceStateDao().insertState(targetState)
                                FirebaseAuthManager.pushResourceToCloud(targetState, scope)
                            }
                            customName = ""
                            showAddCustomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355))
                ) {
                    Text("Provision Node", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel", color = SubTextColor)
                }
            }
        )
    }
}

@Composable
fun CloudResourceOrchestrationToolbar(database: AppDatabase) {
    val resourcesFound by database.resourceStateDao().getAllStates().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTargetId by remember { mutableStateOf("ALL") }
    var consoleMsg by remember { mutableStateOf("Ready for operator instruction...") }
    
    val targetName = if (selectedTargetId == "ALL") "ALL ACTIVE CLUSTERS" else {
        resourcesFound.find { it.id == selectedTargetId }?.name ?: "Unknown Node"
    }

    Text(
        text = "Cloud Coalition Orchestration Toolbar",
        style = MaterialTheme.typography.titleMedium,
        color = TextColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_orchestration_toolbar"),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Cloud Controller Settings",
                        tint = AccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Global Console Controller",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF003355), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SYSTEM ACTIVE",
                        color = AccentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Target Clusters Row
            Text(
                text = "SELECT COMPUTE TARGET ARCHITECTURE:",
                style = MaterialTheme.typography.labelSmall,
                color = SubTextColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ALL NODES target button
                val isAllSelected = selectedTargetId == "ALL"
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isAllSelected) Color(0xFF0D253F) else Color(0x22000000),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) AccentColor else BorderColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTargetId = "ALL" }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("cloud_target_all_chip")
                ) {
                    Text(
                        text = "⚡ ALL CLUSTERS",
                        color = if (isAllSelected) Color.White else SubTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Discovered resources target buttons
                resourcesFound.forEach { res ->
                    val isSelected = selectedTargetId == res.id
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF0D253F) else Color(0x22000000),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentColor else BorderColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTargetId = res.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("cloud_target_${res.id}_chip")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = res.name,
                                color = if (isSelected) Color.White else SubTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = when (res.status) {
                                            "Active" -> Color(0xFF00FF00)
                                            "Warning" -> Color(0xFFFFB74D)
                                            else -> Color(0xFFFF5252)
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Live Orchestrator Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // START Action Button
                Button(
                    onClick = {
                        scope.launch {
                            val targetsToUpdate = if (selectedTargetId == "ALL") resourcesFound else resourcesFound.filter { it.id == selectedTargetId }
                            if (targetsToUpdate.isEmpty()) {
                                consoleMsg = "WARNING: No valid nodes identified to receive command."
                                return@launch
                            }
                            targetsToUpdate.forEach { item ->
                                val updated = item.copy(status = "Active", metrics = "Optimal")
                                database.resourceStateDao().insertState(updated)
                                FirebaseAuthManager.pushResourceToCloud(updated, scope)
                            }
                            consoleMsg = "SUCCESS: START signal activated for $targetName."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cloud_start_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20),
                        contentColor = Color(0xFFC8E6C9)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start targets",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // STOP Action Button
                Button(
                    onClick = {
                        scope.launch {
                            val targetsToUpdate = if (selectedTargetId == "ALL") resourcesFound else resourcesFound.filter { it.id == selectedTargetId }
                            if (targetsToUpdate.isEmpty()) {
                                consoleMsg = "WARNING: No systems to execute structural shutdown instruction."
                                return@launch
                            }
                            targetsToUpdate.forEach { item ->
                                val updated = item.copy(status = "Disabled", metrics = "Suspended")
                                database.resourceStateDao().insertState(updated)
                                FirebaseAuthManager.pushResourceToCloud(updated, scope)
                            }
                            consoleMsg = "HALTED: STOP command deployed to $targetName."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cloud_stop_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color(0xFFFFCDD2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop targets",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("STOP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // RESTART Action Button
                Button(
                    onClick = {
                        scope.launch {
                            val targetsToUpdate = if (selectedTargetId == "ALL") resourcesFound else resourcesFound.filter { it.id == selectedTargetId }
                            if (targetsToUpdate.isEmpty()) {
                                consoleMsg = "WARNING: No active connections established for cyclic load."
                                return@launch
                            }
                            consoleMsg = "CYCLIC REBOOT: Initializing nodes for $targetName..."
                            
                            // 1. Initializing state update
                            targetsToUpdate.forEach { item ->
                                val rebootingState = item.copy(status = "Initializing", metrics = "Rebooting")
                                database.resourceStateDao().insertState(rebootingState)
                                FirebaseAuthManager.pushResourceToCloud(rebootingState, scope)
                            }
                            
                            // Delayed simulation
                            kotlinx.coroutines.delay(1200)
                            
                            // 2. Clear state back to active/optimal
                            targetsToUpdate.forEach { item ->
                                val optimalState = item.copy(status = "Active", metrics = "Optimal")
                                database.resourceStateDao().insertState(optimalState)
                                FirebaseAuthManager.pushResourceToCloud(optimalState, scope)
                            }
                            consoleMsg = "SUCCESS: Cyclic restore complete. Cluster $targetName status: ACTIVE."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cloud_restart_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE65100),
                        contentColor = Color(0xFFFFE0B2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Restart targets",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "COMMON GOOGLE CLOUD TASKS:",
                style = MaterialTheme.typography.labelSmall,
                color = SubTextColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Restart Instance Task Button (Task Suite)
                Button(
                    onClick = {
                        scope.launch {
                            val targetsToUpdate = if (selectedTargetId == "ALL") resourcesFound else resourcesFound.filter { it.id == selectedTargetId }
                            if (targetsToUpdate.isEmpty()) {
                                consoleMsg = "EXEC ERROR: No active instance selected to issue Reset signal."
                                return@launch
                            }
                            consoleMsg = "GCLOUD: reset command deployed for ${targetsToUpdate.joinToString(", ") { it.name }}..."
                            targetsToUpdate.forEach { item ->
                                val resetting = item.copy(status = "Initializing", metrics = "Resetting")
                                database.resourceStateDao().insertState(resetting)
                                FirebaseAuthManager.pushResourceToCloud(resetting, scope)
                            }
                            kotlinx.coroutines.delay(1000)
                            targetsToUpdate.forEach { item ->
                                val active = item.copy(status = "Active", metrics = "Optimal")
                                database.resourceStateDao().insertState(active)
                                FirebaseAuthManager.pushResourceToCloud(active, scope)
                            }
                            consoleMsg = "GCLOUD: Reset sequence completed successfully for $targetName."
                        }
                    },
                    modifier = Modifier.height(36.dp).testTag("gcp_task_restart_instance"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33E65100),
                        contentColor = Color(0xFFFFB74D)
                    ),
                    border = BorderStroke(1.dp, Color(0x55E65100))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Restart Instance", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart Instance", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Clear Cache Task Button
                Button(
                    onClick = {
                        scope.launch {
                            consoleMsg = "EDGE CACHE: Invalidating CDN url-maps and gateway proxies..."
                            kotlinx.coroutines.delay(800)
                            consoleMsg = "SUCCESS: CDN Edge and local memory cache cleared [0.00ms latency]."
                        }
                    },
                    modifier = Modifier.height(36.dp).testTag("gcp_task_clear_cache"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3300E5FF),
                        contentColor = Color(0xFFE0F7FA)
                    ),
                    border = BorderStroke(1.dp, Color(0x5500E5FF))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear Cache", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Cache", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Flush DNS Task Button
                Button(
                    onClick = {
                        scope.launch {
                            consoleMsg = "DNS: Refreshing Cloud DNS response policies..."
                            kotlinx.coroutines.delay(700)
                            consoleMsg = "SUCCESS: DNS lookup caches flushed globally across 14 PoPs."
                        }
                    },
                    modifier = Modifier.height(36.dp).testTag("gcp_task_flush_dns"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x337E57C2),
                        contentColor = Color(0xFFEDE7F6)
                    ),
                    border = BorderStroke(1.dp, Color(0x557E57C2))
                ) {
                    Icon(Icons.Filled.Public, contentDescription = "Flush DNS", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flush DNS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Rotate Keys Task Button
                Button(
                    onClick = {
                        scope.launch {
                            consoleMsg = "IAM security: Initiating automated OAuth client & SA credentials rotation..."
                            kotlinx.coroutines.delay(1000)
                            consoleMsg = "SUCCESS: Generated brand-new JWT verification key definitions."
                        }
                    },
                    modifier = Modifier.height(36.dp).testTag("gcp_task_rotate_keys"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3300C853),
                        contentColor = Color(0xFFE8F5E9)
                    ),
                    border = BorderStroke(1.dp, Color(0x5500C853))
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Rotate Keys", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate Keys", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Micro-terminal feedback board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0F11), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "> ",
                        color = AccentColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = consoleMsg,
                        color = Color.White,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
fun GeoDistributionChart() {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Geographical Resource Distribution", style = MaterialTheme.typography.bodySmall, color = SubTextColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            val localAccent = AccentColor
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height

                // Draw a simple map wireframe
                drawPath(
                    path = Path().apply {
                        moveTo(cw * 0.1f, ch * 0.3f)
                        lineTo(cw * 0.4f, ch * 0.2f)
                        lineTo(cw * 0.5f, ch * 0.4f)
                        lineTo(cw * 0.8f, ch * 0.3f)
                        lineTo(cw * 0.9f, ch * 0.7f)
                        lineTo(cw * 0.6f, ch * 0.8f)
                        lineTo(cw * 0.3f, ch * 0.9f)
                        lineTo(cw * 0.1f, ch * 0.7f)
                        close()
                    },
                    color = Color(0xFF003355),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Render "Device" point
                val devicePoint = Offset(cw * 0.3f, ch * 0.6f)
                drawCircle(color = localAccent, radius = 6.dp.toPx(), center = devicePoint)

                // Render instances
                val instances = listOf(
                    Offset(cw * 0.2f, ch * 0.3f), // us-west
                    Offset(cw * 0.45f, ch * 0.35f), // us-central
                    Offset(cw * 0.8f, ch * 0.4f), // eu-west
                    Offset(cw * 0.75f, ch * 0.65f) // asia-east
                )

                instances.forEach { instance ->
                    drawCircle(color = Color(0xFF00E5FF), radius = 4.dp.toPx(), center = instance)
                    // draw connection line
                    drawPath(
                        path = Path().apply {
                            moveTo(devicePoint.x, devicePoint.y)
                            lineTo(instance.x, instance.y)
                        },
                        color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatIntelligenceRadar() {
    var scanning by remember { mutableStateOf(false) }
    var threats by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Warning, contentDescription = null, tint = AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Evez-OS Threat Radar", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Container(
                    color = if (scanning) Color(0xFF003355) else BorderColor,
                    textColor = if (scanning) AccentColor else SubTextColor,
                    text = if (scanning) "MAPPING..." else "IDLE"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Advanced OSINT & Spectral Memory Catalogue", style = MaterialTheme.typography.bodySmall, color = SubTextColor)
            Spacer(modifier = Modifier.height(16.dp))

            if (threats.isEmpty() && !scanning) {
                Text("No anomalies detected. Awaiting manual metric measurement.", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
            } else if (scanning) {
                LinearProgressIndicator(color = AccentColor, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bypassing noclip locks... Unfalsifiably mapping property assumptions...", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
            } else {
                threats.forEach { threat ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = threat, style = MaterialTheme.typography.bodyMedium, color = TextColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!scanning) {
                        scanning = true
                        threats = emptyList()
                        scope.launch {
                            try {
                                val request = GenerateContentRequest(
                                    contents = listOf(Content(parts = listOf(Part("You are an Evez-OS OSINT threat detection module. Generate a comma-separated list of 3 fictional sci-fi OSINT memory catalog anomalies or spectral tracing threats. Make them sound very technical and obscure.")), role = "user"))
                                )
                                val response = GeminiApi.service.generateContent(
                                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                                    apiKey = BuildConfig.GEMINI_API_KEY,
                                    request = request
                                )
                                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Spectral memory leak, Metric catalog falsification, Noclip memory bypass"
                                threats = reply.split(",").map { it.trim().trim('*', ' ', '\n', '\r', '"') }
                            } catch (e: Exception) {
                                threats = listOf("Database Decentralization Interrupted", "Noclip Replay Signature Found", "Unfalsifiable Property Masking")
                            }
                            scanning = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "EXECUTE DECENTRALIZED OSINT MAP", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OpenClawAuthDialog(onDismiss: () -> Unit, database: AppDatabase, onUpdateProviders: (List<ProviderData>) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var operationMsg by remember { mutableStateOf("") }
    var showLogsTab by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val currentUser by FirebaseAuthManager.currentUserState.collectAsStateWithLifecycle()
    val activeEmail by FirebaseAuthManager.simulatedUserEmail.collectAsStateWithLifecycle()
    val isFallbackMode by FirebaseAuthManager.isFallbackMode.collectAsStateWithLifecycle()
    val authLogs by FirebaseAuthManager.authLogs.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        if (currentUser != null && !isFallbackMode) {
            FirebaseAuthManager.pullProvidersFromCloud { onUpdateProviders(it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        titleContentColor = TextColor,
        textContentColor = TextColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = AccentColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentUser != null || activeEmail != null) "Unified Secure Console" else "OpenClaw OAuth Connect",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (currentUser == null && activeEmail == null) {
                    TabRow(
                        selectedTabIndex = if (isSignUpMode) 1 else 0,
                        containerColor = BgColor,
                        contentColor = AccentColor,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = !isSignUpMode,
                            onClick = { isSignUpMode = false },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = isSignUpMode,
                            onClick = { isSignUpMode = true },
                            text = { Text("Register", fontWeight = FontWeight.Bold) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor, unfocusedTextColor = TextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Security Shield Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor, unfocusedTextColor = TextColor
                        )
                    )
                    
                    if (operationMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(operationMsg, color = Color(0xFFFFA726), style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                operationMsg = "Please enter valid email and password"
                                return@Button
                            }
                            operationMsg = "Connecting database..."
                            if (isSignUpMode) {
                                FirebaseAuthManager.signUpWithEmail(email, password, scope) { success, msg ->
                                    operationMsg = msg
                                    if (success) {
                                        FirebaseAuthManager.pullProvidersFromCloud { onUpdateProviders(it) }
                                    }
                                }
                            } else {
                                FirebaseAuthManager.signInWithEmail(email, password, scope) { success, msg ->
                                    operationMsg = msg
                                    if (success) {
                                        FirebaseAuthManager.pullProvidersFromCloud { onUpdateProviders(it) }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isSignUpMode) "SECURE COMMITS" else "AUTHENTICATE GATEWAY", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            FirebaseAuthManager.signInAnonymously { success, msg ->
                                operationMsg = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue as Sandbox Guest", color = AccentColor, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Logged in mode
                    Surface(
                        color = Color(0xFF0D253F),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ACTIVE AUTHENTICATION:", style = MaterialTheme.typography.labelSmall, color = AccentColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(activeEmail ?: "Guest Session Token", color = TextColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isFallbackMode) "Mode: Secured Local Storage Sandbox" else "Mode: Remote Firebase Cloud Connected",
                                style = MaterialTheme.typography.bodySmall, color = SubTextColor
                            )
                        }
                    }
                    
                    if (!isFallbackMode) {
                        Button(
                            onClick = {
                                FirebaseAuthManager.pullResourcesFromCloud(database, scope)
                                FirebaseAuthManager.pullProvidersFromCloud { onUpdateProviders(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2235), contentColor = AccentColor),
                            border = BorderStroke(1.dp, AccentColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = AccentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FORCE RESYNC PULL FROM CLOUD", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Button(
                        onClick = {
                            FirebaseAuthManager.signOut()
                            operationMsg = "Logged out successfully"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1E1E), contentColor = Color(0xFFFF8585)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("REVOKE SESSION / LOG OUT", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connection Trace Logs:", color = SubTextColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showLogsTab = !showLogsTab }) {
                        Text(if (showLogsTab) "Hide Trace" else "Show Trace", color = AccentColor, fontSize = 11.sp)
                    }
                }
                
                if (showLogsTab) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BgColor),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().height(140.dp).padding(vertical = 4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp).fillMaxSize()) {
                            items(authLogs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = if (log.contains("SUCCESS") || log.contains("Authenticated")) Color(0xFF00FF00) else if (log.contains("WARNING") || log.contains("FAILED")) Color(0xFFFFB74D) else TextColor
                                )
                            }
                        }
                    }
                    TextButton(onClick = { FirebaseAuthManager.clearLogs() }) {
                        Text("Clear system trace log", color = Color(0xFFFF5252), fontSize = 10.sp)
                    }
                }
                
                if (isFallbackMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFF2B2B11),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("INTEGRATION TROUBLESHOOTING GUIDE:", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "To lock connection to live Firestore databases:\n\n" +
                                "1. Download Google Services definition configuration bundle (google-services.json) from Firebase Console.\n" +
                                "2. Store google-services.json directly within the '/app/' directory.\n" +
                                "3. Confirm SHA-1 keystore settings coincide with Firebase config.\n" +
                                "4. Re-compile and trigger deep validation. Our engine will dynamically interface and bridge transactions.",
                                fontSize = 10.sp,
                                color = TextColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close Console", color = AccentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OpenClawGameSharkCheatCodeCenter() {
    var openclawBeta by remember { mutableStateOf(false) }
    var debugPayload by remember { mutableStateOf(false) }
    var cacheTrace by remember { mutableStateOf(false) }
    var disableDeviceAuth by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "🕹️ GAMESHARK CHEAT REGISTER",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time environment & sandbox modifiers",
                        style = MaterialTheme.typography.bodySmall,
                        color = SubTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CheatToggleRow(
                title = "OPENCLAW_BETA",
                description = "Enable advanced unreleased model structures",
                checked = openclawBeta,
                onCheckedChange = {
                    openclawBeta = it
                    Kernel.appendEvent(
                        DomainEvent(
                            type = "CHEATS_STATE_MUTATED",
                            domainId = "SystemScan",
                            payload = mapOf("variable" to "OPENCLAW_BETA", "status" to if (it) "Enabled" else "Disabled")
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CheatToggleRow(
                title = "DEBUG_MODEL_PAYLOAD",
                description = "Dump all raw request JSON to standard logs",
                checked = debugPayload,
                onCheckedChange = {
                    debugPayload = it
                    Kernel.appendEvent(
                        DomainEvent(
                            type = "CHEATS_STATE_MUTATED",
                            domainId = "SystemScan",
                            payload = mapOf("variable" to "OPENCLAW_DEBUG_MODEL_PAYLOAD", "status" to if (it) "Enabled" else "Disabled")
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CheatToggleRow(
                title = "OPENCLAW_CACHE_TRACE",
                description = "Analyze prompt cache hits / misses in real-time",
                checked = cacheTrace,
                onCheckedChange = {
                    cacheTrace = it
                    Kernel.appendEvent(
                        DomainEvent(
                            type = "CHEATS_STATE_MUTATED",
                            domainId = "SystemScan",
                            payload = mapOf("variable" to "OPENCLAW_CACHE_TRACE", "status" to if (it) "Enabled" else "Disabled")
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CheatToggleRow(
                title = "DEVICE_AUTH_DISABLE",
                description = "Bypass hardware certificate gates",
                checked = disableDeviceAuth,
                onCheckedChange = {
                    disableDeviceAuth = it
                    Kernel.appendEvent(
                        DomainEvent(
                            type = "CHEATS_STATE_MUTATED",
                            domainId = "SystemScan",
                            payload = mapOf("variable" to "DEVICE_AUTH_DISABLE", "status" to if (it) "Enabled" else "Disabled")
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CODER CHEAT-SHEET HOTKEYS:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFB74D),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        Kernel.appendEvent(
                            DomainEvent(
                                type = "CHEATS_SYSTEM_EVENT_INJECT",
                                domainId = "SystemScan",
                                payload = mapOf("command" to "openclaw system event", "source" to "AndroidConsole")
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E230D), contentColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("⚙️ Inject Event", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = {
                        Kernel.appendEvent(
                            DomainEvent(
                                type = "RUBIKS_NAV_CLICK",
                                domainId = "UserInterface",
                                payload = mapOf("target" to "CommitmentsListAll")
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E230D), contentColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("📋 Commitments", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        Kernel.appendEvent(
                            DomainEvent(
                                type = "MISSION_STEP",
                                domainId = "MissionControl",
                                payload = mapOf("step" to "CLI: openclaw tasks flow checked")
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E230D), contentColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("🔄 Flow Pipelines", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = {
                        Kernel.appendEvent(
                            DomainEvent(
                                type = "DIAGNOSTIC_START",
                                domainId = "SystemScan",
                                payload = mapOf("target" to "GoogleMetadata")
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F262B), contentColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("🔑 SA Metadata Token", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun CheatToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0F11), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = AccentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                fontSize = 10.sp,
                color = SubTextColor
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF003355),
                checkedTrackColor = Color(0xFFFFB74D),
                uncheckedThumbColor = SubTextColor,
                uncheckedTrackColor = Color(0xFF1E2125)
            )
        )
    }
}

