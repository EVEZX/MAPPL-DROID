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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    try {
        val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
        if (webViewCacheDir.exists()) {
            webViewCacheDir.deleteRecursively()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OpenClawDashboard()
            com.example.ui.ContextReasoningOverlay()
        }
      }
    }
  }
}

val BgColor = Color(0xFF0B0D0F)
val CardColor = Color(0xFF1A1C1E)
val TextColor = Color(0xFFE2E2E6)
val BorderColor = Color(0xFF2D3135)
val AccentColor = Color(0xFFD1E4FF)
val SubTextColor = Color(0xFF8E9196)

@Composable
fun OpenClawDashboard() {
  var isChatOpen by remember { mutableStateOf(false) }
  var isBrowserOpen by remember { mutableStateOf(false) }
  var isAuthMocked by remember { mutableStateOf(false) }
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

          when (index % 12) {
              0 -> Column(modifier = mod) {
                  Spacer(modifier = Modifier.height(16.dp))
                  Text(text = "God AI Task Execution Deluxe Ultra Extra", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                  Text(text = "v1.0-KILOCLAW-FULL • EVEZ-OS ACTIVE", style = MaterialTheme.typography.bodySmall, color = AccentColor)
              }
              1 -> Box(modifier = mod) { EvezOsIntegration() }
              2 -> Box(modifier = mod) { AutomatedResourceInventory(database = database) }
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
              6 -> Box(modifier = mod) {
                  // SSH Gateway / Compute Node
                  Card(
          modifier = Modifier.fillMaxWidth(), 
          colors = CardDefaults.cardColors(containerColor = CardColor),
          shape = RoundedCornerShape(32.dp),
          border = BorderStroke(1.dp, BorderColor)
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "COMPUTE VCM INSTANCE", style = MaterialTheme.typography.labelSmall, color = SubTextColor)
                    Text(text = "openclaw-power-node", style = MaterialTheme.typography.titleLarge, color = AccentColor, fontWeight = FontWeight.Bold)
                    Text(text = "us-central1-a • 16GB RAM", style = MaterialTheme.typography.bodySmall, color = SubTextColor)
                }
                Container(
                    color = if (sshConnected) Color(0xFF003355) else BorderColor,
                    textColor = AccentColor,
                    text = if (sshConnected) "CONNECTED" else "STANDBY"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsBox(modifier = Modifier.weight(1f), label = "GATEWAY PORT", value = "18789")
                MetricsBox(modifier = Modifier.weight(1f), label = "LATENCY", value = if (sshConnected) "14ms" else "--", valueColor = Color(0xFF00FF00))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (sshConnected) {
                Surface(color = Color(0xFF0A1929), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF003355)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column {
                            Text(text = "ACTIVE GATEWAY TOKEN", style = MaterialTheme.typography.labelSmall, color = AccentColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = gatewayToken, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Button(
                onClick = { 
                    sshConnected = !sshConnected 
                    if (sshConnected) {
                        gatewayToken = "oct_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                    } else {
                        gatewayToken = "NOT_GENERATED"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(text = if (sshConnected) "DISCONNECT GATEWAY" else "⚡ FORCE SSH GATEWAY", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
7 -> Box(modifier = mod) {
                  Column {
                      Text(text = "Identity & Persistence", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      Card(
          modifier = Modifier.fillMaxWidth().clickable { isAuthMocked = !isAuthMocked },
          colors = CardDefaults.cardColors(containerColor = CardColor),
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, BorderColor)
      ) {
          Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Icon(if (isAuthMocked) Icons.Filled.AccountCircle else Icons.Filled.Storage, contentDescription = null, tint = AccentColor, modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                  Text(text = if (isAuthMocked) "Authenticated User\n(Firebase Active)" else "Tap to Sign In", style = MaterialTheme.typography.bodyMedium, color = AccentColor, fontWeight = FontWeight.Bold)
                  Text(text = if (isAuthMocked) "Firestore Synced" else "Firebase Auth Disconnected", style = MaterialTheme.typography.bodySmall, color = SubTextColor)
              }
              if (isAuthMocked) {
                  Container(color = Color(0xFF003355), textColor = AccentColor, text = "SECURE")
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
                ProviderRow(icon = provider.icon, name = provider.name, keyPreview = provider.keyPreview, iconColor = provider.iconColor, isActive = provider.isActive)
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
                              providers = providers + ProviderData(
                                  icon = newProviderName.firstOrNull()?.uppercase() ?: "?",
                                  name = newProviderName,
                                  keyPreview = preview,
                                  iconColor = Color(0xFFE2E2E6),
                                  isActive = true
                              )
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
      var voiceInput by remember { mutableStateOf("") }
      var isListening by remember { mutableStateOf(true) }
      var processing by remember { mutableStateOf(false) }

      AlertDialog(
          onDismissRequest = { showVoiceDialog = false },
          containerColor = CardColor,
          titleContentColor = TextColor,
          textContentColor = TextColor,
          title = { Text(if (isListening) "Listening (A16 Hardware)..." else if (processing) "Processing..." else "Voice Command Recognized") },
          text = {
              Column {
                  Text(text = "Try saying: 'Run that shit turbo' or 'what up my nigga computer'", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                  Spacer(modifier = Modifier.height(16.dp))
                  OutlinedTextField(
                      value = voiceInput,
                      onValueChange = { voiceInput = it },
                      label = { Text("Simulate spoken text") },
                      colors = OutlinedTextFieldDefaults.colors(
                          unfocusedTextColor = TextColor, focusedTextColor = TextColor,
                          focusedBorderColor = AccentColor, unfocusedBorderColor = BorderColor,
                          focusedLabelColor = AccentColor, unfocusedLabelColor = SubTextColor
                      ),
                      modifier = Modifier.fillMaxWidth()
                  )
              }
          },
          confirmButton = {
              Button(
                  onClick = {
                      isListening = false
                      processing = true
                      scope.launch {
                          val normalizedInput = voiceInput.lowercase()
                          if (normalizedInput.contains("turbo") || normalizedInput.contains("diagnostics") || normalizedInput.contains("shit")) {
                              systemAlerts = systemAlerts + "Voice Command: Initiating deep diagnostics..."
                              // Call API...
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
                              } catch (e: Exception) {
                                  systemAlerts = systemAlerts + "Error: API Unreachable."
                              }
                          } else if (normalizedInput.contains("refresh") || normalizedInput.contains("computer")) {
                              systemAlerts = systemAlerts + "Voice Command: Refreshed connections."
                          } else {
                              systemAlerts = systemAlerts + "Voice Command: Unrecognized ($voiceInput)"
                          }
                          showVoiceDialog = false
                          processing = false
                      }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355))
              ) {
                  Text("Prcs. Command", fontWeight = FontWeight.Bold)
              }
          },
          dismissButton = {
              TextButton(onClick = { showVoiceDialog = false }) {
                  Text("Cancel", color = SubTextColor)
              }
          }
      )
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
                    onCheckedChange = { evezNexusActive = it },
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
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF0055), checkedTrackColor = Color(0xFF550022))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    evezNexusActive = true
                    evezartEngineActive = true
                    evezxSecurityActive = true
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
                Container(
                    color = if (isScanning) Color(0xFF003355) else BorderColor,
                    textColor = if (isScanning) AccentColor else SubTextColor,
                    text = if (isScanning) "SCANNING" else "READY"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (resourcesFound.isEmpty()) {
                Text("No resources scanned yet. Initiate discovery.", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
            } else {
                resourcesFound.forEach { res ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = res.name, style = MaterialTheme.typography.bodyMedium, color = TextColor)
                            Text(text = "Status: ${res.status}", style = MaterialTheme.typography.labelSmall, color = SubTextColor)
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
                                    contents = listOf(Content(parts = listOf(Part("You are an automated resource discovery system for Evez-OS and Google Cloud. Generate a realistic comma-separated list of 3-4 fictional cloud or edge resources you just discovered without introductory text.")), role = "user"))
                                )
                                val response = GeminiApi.service.generateContent(
                                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                                    apiKey = BuildConfig.GEMINI_API_KEY,
                                    request = request
                                )
                                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Compute VCM, Edge Node Alpha, Database Cluster"
                                val newResources = reply.split(",").map { it.trim() }
                                newResources.forEachIndexed { index, name ->
                                    database.resourceStateDao().insertState(ResourceState(
                                        id = "res_${System.currentTimeMillis()}_$index",
                                        name = name,
                                        status = "Active",
                                        metrics = "Normal"
                                    ))
                                }
                            } catch (e: Exception) {
                                // Fallback cached states are handled by Room DB
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
                drawCircle(color = AccentColor, radius = 6.dp.toPx(), center = devicePoint)

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
