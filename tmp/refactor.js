const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf8');

const regex = /items\(count = Int\.MAX_VALUE\) \{ _ ->\s*Column \{([\s\S]*?)Spacer\(modifier = Modifier\.height\(32\.dp\)\)\s*\}\s*\}/;

const match = code.match(regex);
if (match) {
    let block = match[1];
    
    // Split the block into sections based on Spacer(modifier = Modifier.height(24.dp))
    // But some sections are small. we'll split manually.
    
    // Just find specific hooks and replace them with `when (index % 10) { ... }` wrappers.
    let newItems = `items(count = Int.MAX_VALUE) { index ->
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
                      translationZ = -kotlin.math.abs(absoluteOffset) * 200f
                      scaleX = scale
                      scaleY = scale
                      this.alpha = alpha
                      cameraDistance = 12f * density
                  }
                  .padding(vertical = 12.dp)

          when (index % 10) {
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
                          }
                      )
                  }
              }
              5 -> Box(modifier = mod) { com.example.ui.KernelDashboardUI() }
              6 -> Box(modifier = mod) {
                  // SSH Node
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
                  // Auth Database Identity
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
                                  Text(text = if (isAuthMocked) "Authenticated User\\n(Firebase Active)" else "Tap to Sign In", style = MaterialTheme.typography.bodyMedium, color = AccentColor, fontWeight = FontWeight.Bold)
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
              9 -> Box(modifier = mod) {
                  Column {
                      Text(text = "Real-time Metrics", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                          Box(modifier = Modifier.weight(1f)) {
                              MetricChart(title = "Request Latency (ms)", dataPoints = latencyData, color = AccentColor, isBarChart = false)
                          }
                          Box(modifier = Modifier.weight(1f)) {
                              MetricChart(title = "Gemini Token Usage", dataPoints = tokenData, color = Color(0xFF00E5FF), isBarChart = true)
                          }
                      }
                  
                      Spacer(modifier = Modifier.height(24.dp))
                      GeoDistributionChart()
                  
                      Spacer(modifier = Modifier.height(24.dp))
                      
                      Text(text = "System Telemetry", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      Card(
                          modifier = Modifier.fillMaxWidth(),
                          colors = CardDefaults.cardColors(containerColor = CardColor),
                          shape = RoundedCornerShape(24.dp),
                          border = BorderStroke(1.dp, BorderColor)
                      ) {
                          Column(modifier = Modifier.padding(16.dp)) {
                              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                  Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = AccentColor)
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text(text = "Samsung Galaxy A16 Full Access", style = MaterialTheme.typography.bodyMedium, color = AccentColor, fontWeight = FontWeight.Bold)
                              }
                              Spacer(modifier = Modifier.height(16.dp))
                              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                  MetricsBox(modifier = Modifier.weight(1f), label = "CPU LOAD", value = "12%")
                                  MetricsBox(modifier = Modifier.weight(1f), label = "RAM", value = "4.2/8GB")
                                  MetricsBox(modifier = Modifier.weight(1f), label = "TEMP", value = "32°C", valueColor = Color(0xFF00FF00))
                              }
                          }
                      }
                  
                      Spacer(modifier = Modifier.height(24.dp))
                      
                      Text(text = "AI System Alerts", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      Card(
                          modifier = Modifier.fillMaxWidth(),
                          colors = CardDefaults.cardColors(containerColor = CardColor),
                          shape = RoundedCornerShape(24.dp),
                          border = BorderStroke(1.dp, BorderColor)
                      ) {
                          Column(modifier = Modifier.padding(16.dp)) {
                              systemAlerts.forEach { alert ->
                                  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                      Icon(if (alert.contains("System")) Icons.Filled.Info else Icons.Filled.Warning, contentDescription = null, tint = if (alert.contains("System")) AccentColor else Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Text(text = alert, style = MaterialTheme.typography.bodyMedium, color = TextColor)
                                  }
                              }
                              Spacer(modifier = Modifier.height(16.dp))
                              Button(
                                  onClick = {
                                      scope.launch {
                                          systemAlerts = systemAlerts + "Scanning GCP logs..."
                                          try {
                                              val request = GenerateContentRequest(
                                                  contents = listOf(Content(parts = listOf(Part("You are scanning Google Cloud Platform logs for an OpenClaw instance. Generate a single highly realistic, technical, 1-2 sentence alert regarding a potential quota limit being reached for Cloud Run or Vertex AI, or a suspicious activity blip. Do not include introductory text like 'Here is an alert'.")), role = "user"))
                                              )
                                              val response = GeminiApi.service.generateContent(
                                                  url = "v1beta/models/gemini-3.5-flash:generateContent",
                                                  apiKey = BuildConfig.GEMINI_API_KEY,
                                                  request = request
                                              )
                                              val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No anomalies detected."
                                              systemAlerts = systemAlerts + reply
                                          } catch (e: Exception) {
                                              systemAlerts = systemAlerts + "Error: GCP API Unreachable."
                                          }
                                      }
                                  },
                                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1929), contentColor = AccentColor),
                                  modifier = Modifier.fillMaxWidth()
                              ) {
                                  Text(text = "SCAN FOR ANOMALIES", fontWeight = FontWeight.Bold)
                              }
                          }
                      }
                  
                      Spacer(modifier = Modifier.height(24.dp))
                      Text(text = "Cloud Resources", style = MaterialTheme.typography.titleMedium, color = TextColor, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(8.dp))
                      val uriHandler = LocalUriHandler.current
                      val resources = listOf(
                        Resource("OpenClaw Power Node", "http://10.0.0.1:18789"),
                        Resource("Evez-OS v2", "http://evez.app"),
                        Resource("GCP Console", "https://console.cloud.google.com/"),
                        Resource("Cloud Run", "https://console.cloud.google.com/run"),
                        Resource("Compute Engine", "https://console.cloud.google.com/compute"),
                        Resource("Billing", "https://console.cloud.google.com/billing"),
                        Resource("Secrets Manager", "https://console.cloud.google.com/security/secret-manager")
                      )
                      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        resources.forEach { resource ->
                           Card(
                              modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(resource.url) },
                              colors = CardDefaults.cardColors(containerColor = CardColor),
                              shape = RoundedCornerShape(24.dp),
                              border = BorderStroke(1.dp, BorderColor)
                          ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = resource.name, style = MaterialTheme.typography.bodyLarge, color = AccentColor)
                                Text(text = resource.url, style = MaterialTheme.typography.bodySmall, color = SubTextColor)
                            }
                          }
                        }
                      }
                  }
              }
          }
      }`;
      
    code = code.replace(match[0], newItems);
    
    // now we also need to move the Custom AddProviderDialog block.
    // the AlertDialog was inside the lazycolumn, but it shouldn't be anyway properly.
    
    fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', code);
    console.log("Replaced successfully!");
} else {
    console.log("No match found!");
}
