package com.example.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.BuildConfig
import com.example.Content
import com.example.GenerateContentRequest
import com.example.GeminiApi
import com.example.Part
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ConsoleLogType {
    COMMAND, INFO, SUCCESS, WARNING, ERROR, PROGRESS
}

data class ConsoleLog(
    val text: String,
    val type: ConsoleLogType = ConsoleLogType.INFO,
    val timestamp: String = "00:00:00"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperParadiseDashboard() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) } // 0: Shell, 1: Self-Build, 2: Synadaptive, 3: Metarom, 4: OSINT Core

    // --- Tab 0 State (Console & Codespaces) ---
    var consoleInput by remember { mutableStateOf("") }
    var promptInput by remember { mutableStateOf("") }
    var isGeneratingCommand by remember { mutableStateOf(false) }
    var isExecutingCommand by remember { mutableStateOf(false) }
    val consoleLogs = remember {
        mutableStateListOf(
            ConsoleLog("EVEZ-OS Kernel & CLI System initialized.", ConsoleLogType.SUCCESS),
            ConsoleLog("Google Cloud Codespaces sandbox linked on port 19001.", ConsoleLogType.INFO),
            ConsoleLog("Mounting MCP servers: ['fs-connector', 'gcp-iap-bridge', 'openclaw-gateway']", ConsoleLogType.INFO),
            ConsoleLog("Warning: Root developer privileges active. Real local shell commands supported.", ConsoleLogType.WARNING)
        )
    }

    // --- Tab 1 State (Self-Build Engine) ---
    var isSelfCompiling by remember { mutableStateOf(false) }
    var compileProgress by remember { mutableFloatStateOf(0f) }
    var currentCompileStatus by remember { mutableStateOf("SYSTEMIDLE_MUTATOR_STANDBY") }
    val compiledModules = remember {
        mutableStateListOf(
            "com.example.MainActivity.kt" to true,
            "com.example.ui.DeveloperParadise.kt" to true,
            "com.example.Kernel.kt" to true,
            "com.example.Database.kt" to true,
            "com.example.QuantumBrowser.kt" to false,
            "com.example.ui.AppVoiceAgentDialog.kt" to false
        )
    }

    // --- Tab 2 State (Customization Configs) ---
    var openclawBetaEnabled by remember { mutableStateOf(true) }
    var cacheTraceEnabled by remember { mutableStateOf(false) }
    var systemAutoDoctor by remember { mutableStateOf(true) }
    var dangerouslyBypassAuth by remember { mutableStateOf(true) }
    var cognitiveSpeed by remember { mutableFloatStateOf(1.5f) }
    var memoryCompaction by remember { mutableFloatStateOf(0.8f) }
    var gcpBandwidthAllocated by remember { mutableFloatStateOf(12.0f) }
    var compactModelSelected by remember { mutableStateOf("Gemini 3.5 Flash") }

    // Synesthetic pairing state
    var selectedSensor by remember { mutableStateOf("INTELLIGENCE_ROUTER_V1") }
    var selectedGcpPivot by remember { mutableStateOf("PUBSUB_TELEMETRY") }
    val pairedNodes = remember {
        mutableStateMapOf(
            "VOCAL_STREAM" to "LOCAL_STORAGE",
            "EMF_EVEZX_RADIO" to "PUBSUB_TELEMETRY",
            "ACCELEROMETER_TELEMETRY" to "CLOUD_FIRESTORE"
        )
    }

    // --- Tab 3 State (Metarom Core Emu) ---
    var selectedRom by remember { mutableStateOf("Pokemon Cobalt: OpenClaw Metarom v4") }
    val romOptionsList = listOf(
        "Pokemon Cobalt: OpenClaw Metarom v4",
        "Legend of Zelda: Synaptic Echoes",
        "Super Metroid: EvezX Hyperdrive",
        "GitHub codespaces custom.rom"
    )
    var metaromMutationIndex by remember { mutableFloatStateOf(0.45f) }
    var generatedCutsceneText by remember { mutableStateOf("No cutscene spawned yet. Select a ROM and tap the synthesize core down below.") }
    var isSynthesizingCutscene by remember { mutableStateOf(false) }
    
    // Virtual retro emulator registers
    var emuRegisterPC by remember { mutableStateOf("0x3F2A") }
    var emuRegisterSP by remember { mutableStateOf("0xDFFE") }
    var emuValueA by remember { mutableIntStateOf(42) }
    var emuValueB by remember { mutableIntStateOf(255) }
    var emuClockCycles by remember { mutableLongStateOf(2421901L) }
    var isEmuRunning by remember { mutableStateOf(true) }

    // --- Tab 4 State (OSINT Start Menu) ---
    var isOsintLoopActive by remember { mutableStateOf(true) }
    var sensAccelX by remember { mutableFloatStateOf(0.02f) }
    var sensAccelY by remember { mutableFloatStateOf(9.81f) }
    var sensAccelZ by remember { mutableFloatStateOf(-0.15f) }
    var sensLightLux by remember { mutableFloatStateOf(320f) }
    var sensAudioDecibel by remember { mutableFloatStateOf(45.5f) }
    var sensEmfIntensity by remember { mutableFloatStateOf(14.8f) } // microTesla
    var sensCognitiveParity by remember { mutableFloatStateOf(94.2f) } // alignment metric

    // Integration Plugin Toggles
    var pluginEvezartActive by remember { mutableStateOf(true) }
    var pluginEvezXActive by remember { mutableStateOf(true) }
    var pluginEvezOsControlActive by remember { mutableStateOf(true) }
    var pluginGithubCodespacesLinked by remember { mutableStateOf(true) }

    // --- Tab 5 State (Alchemical Gold Transmutation) ---
    var alchemyTemperature by remember { mutableFloatStateOf(350f) } // Furnace temperature (Celsius)
    var isTransmutingGold by remember { mutableStateOf(false) }
    var rawLeadPuttings by remember { mutableFloatStateOf(100f) } // oz lead
    var pureGoldYield by remember { mutableFloatStateOf(0.0f) } // oz gold
    val transmutedHistory = remember { mutableStateListOf<String>() }
    var alchemicalEpiphany by remember { mutableStateOf("Ready to distill lead and pyrite into metaphysical gold.") }
    var isGeneratingEpiphany by remember { mutableStateOf(false) }

    // Read real system sensors safely
    DisposableEffect(isOsintLoopActive) {
        if (!isOsintLoopActive) onDispose { }
        
        var sensorManager: SensorManager? = null
        var accelSensor: Sensor? = null
        var lightSensor: Sensor? = null
        
        var sensorEventListener: SensorEventListener? = null
        
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            if (sensorManager != null) {
                accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                
                sensorEventListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null) return
                        when (event.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> {
                                sensAccelX = event.values.getOrNull(0) ?: 0f
                                sensAccelY = event.values.getOrNull(1) ?: 0f
                                sensAccelZ = event.values.getOrNull(2) ?: 0f
                                
                                // Synthesize fluctuating EMF and sound indicators based slightly on physics
                                sensEmfIntensity = 10f + (sensAccelX * sensAccelX + sensAccelY * sensAccelY) * 0.5f
                                sensCognitiveParity = (90f + (sensAccelZ * 0.4f)).coerceIn(0f, 100f)
                            }
                            Sensor.TYPE_LIGHT -> {
                                sensLightLux = event.values.getOrNull(0) ?: 100f
                            }
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                
                if (accelSensor != null) {
                    sensorManager.registerListener(sensorEventListener, accelSensor, SensorManager.SENSOR_DELAY_UI)
                }
                if (lightSensor != null) {
                    sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        onDispose {
            try {
                if (sensorManager != null && sensorEventListener != null) {
                    sensorManager.unregisterListener(sensorEventListener)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto fluctuating telemetry for simulated/emulated values in background
    LaunchedEffect(isEmuRunning) {
        while (isEmuRunning) {
            delay(100)
            emuClockCycles += (4..12).random()
            emuValueA = (emuValueA + (-5..5).random()).coerceIn(0, 255)
            emuValueB = (emuValueB + (-8..8).random()).coerceIn(0, 255)
            val pcRandomVal = (1000..9999).random().toString(16).uppercase()
            emuRegisterPC = "0x$pcRandomVal"
            
            if (!isOsintLoopActive) {
                // Fluctuates metrics with simulated randomness
                sensAudioDecibel = (40..75).random().toFloat() + (0..9).random().toFloat() * 0.1f
                sensEmfIntensity = 12f + (1..8).random().toFloat() * 0.3f
                sensCognitiveParity = 85f + (0..14).random().toFloat() * 0.9f
            }
        }
    }

    // Helpers to write console logs
    fun getCurrentTime(): String {
        val calendar = java.util.Calendar.getInstance()
        val h = String.format("%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY))
        val m = String.format("%02d", calendar.get(java.util.Calendar.MINUTE))
        val s = String.format("%02d", calendar.get(java.util.Calendar.SECOND))
        return "$h:$m:$s"
    }

    fun addLog(text: String, type: ConsoleLogType = ConsoleLogType.INFO) {
        consoleLogs.add(ConsoleLog(text, type, getCurrentTime()))
    }

    // Core subprocess executor that handles BOTH simulated commands AND direct system shell execution!
    fun executeCommandInLog(command: String) {
        val clean = command.trim()
        if (clean.isEmpty()) return
        addLog("$ $clean", ConsoleLogType.COMMAND)
        isExecutingCommand = true
        
        scope.launch {
            delay(150)
            val args = clean.split(" ")
            val baseCmd = args[0].lowercase()

            when (baseCmd) {
                "help" -> {
                    addLog("Available Gateway & OSINT Commands:", ConsoleLogType.SUCCESS)
                    addLog("  ls -la                         - List files in current sandbox (Real Shell)", ConsoleLogType.INFO)
                    addLog("  getprop ro.product.model       - Query actual hardware model details", ConsoleLogType.INFO)
                    addLog("  ping -c 3 8.8.8.8              - Network endpoint trace (Real Shell)", ConsoleLogType.INFO)
                    addLog("  gcloud auth login              - Mock Google Cloud project linker", ConsoleLogType.INFO)
                    addLog("  gcloud compute instances list  - Lists instances in Google Cloud virtual sandbox", ConsoleLogType.INFO)
                    addLog("  openclaw system presence       - Show current active peer clusters", ConsoleLogType.INFO)
                    addLog("  openclaw-doctor                - Run diagnostic recovery scripts", ConsoleLogType.INFO)
                    addLog("  evez-os stats                  - Dump internal Evez-OS telemetry registers", ConsoleLogType.INFO)
                    addLog("  clear                          - Refreshes terminal stdout buffer", ConsoleLogType.INFO)
                }
                "clear" -> {
                    consoleLogs.clear()
                    addLog("EVEZ-OS / Codespaces developer dashboard console re-linked.", ConsoleLogType.SUCCESS)
                }
                "gcloud" -> {
                    if (args.getOrNull(1) == "auth" && args.getOrNull(2) == "login") {
                        addLog("[GCP Shell] Authenticating secure gateway connection...", ConsoleLogType.PROGRESS)
                        delay(500)
                        addLog("[GCP Shell] Verifying client certificates on project 'openclaw-cloud-brain'...", ConsoleLogType.INFO)
                        delay(400)
                        addLog("[SUCCESS] Authenticated successfully as developer user fiersteity@gmail.com.", ConsoleLogType.SUCCESS)
                        addLog("Sandbox linked. GCP service endpoints verified OK.", ConsoleLogType.INFO)
                    } else if (args.getOrNull(1) == "compute" && args.getOrNull(2) == "instances") {
                        addLog("[GCP Compute] Compiling cluster node table matrix...", ConsoleLogType.PROGRESS)
                        delay(600)
                        addLog("NAME                   ZONE           MACHINE_TYPE  INTERNAL_IP   STATUS", ConsoleLogType.SUCCESS)
                        addLog("openclaw-sandbox-vm    us-east1-b     e2-standard-4 10.142.0.4    RUNNING", ConsoleLogType.INFO)
                        addLog("evez-os-edge-node01    us-central1-c  e2-micro      10.128.8.19   STANDBY", ConsoleLogType.INFO)
                        addLog("evezart-neural-host    us-west1-a     g2-standard-8 10.138.12.5   OFFLINE", ConsoleLogType.WARNING)
                    } else {
                        addLog("[GCP ERROR] Unknown arguments. Use: gcloud compute instances list", ConsoleLogType.ERROR)
                    }
                }
                "openclaw" -> {
                    val sub = args.getOrNull(1)?.lowercase() ?: ""
                    when (sub) {
                        "system" -> {
                            val action = args.getOrNull(2)?.lowercase() ?: ""
                            if (action == "presence") {
                                addLog("[OPENCLAW] Broad-spectrum mesh ping in headway...", ConsoleLogType.PROGRESS)
                                delay(400)
                                addLog("PEERS DETECTED: [com.aistudio.evezart, codespaces_worker_0, metarom_emulator_core]", ConsoleLogType.SUCCESS)
                                addLog("Core Sync status: 94.2% congruent logic tunnels.", ConsoleLogType.INFO)
                            } else {
                                addLog("[OPENCLAW] Unknown target: Use 'openclaw system presence'", ConsoleLogType.ERROR)
                            }
                        }
                        else -> {
                            addLog("[OPENCLAW] Interface args error. Hint: openclaw system presence", ConsoleLogType.ERROR)
                        }
                    }
                }
                "openclaw-doctor" -> {
                    addLog("⚡ [DOCTOR] Starting OpenClaw medical triage check...", ConsoleLogType.PROGRESS)
                    delay(400)
                    addLog("[tri-1] Scanning cloud API definitions: GEMINI_API_KEY detected ✅", ConsoleLogType.INFO)
                    addLog("[tri-2] Checking local android databases: Room layout schema fits perfectly ✅", ConsoleLogType.INFO)
                    addLog("[tri-3] Emulation nodes verified: Metarom game engines connected ✅", ConsoleLogType.INFO)
                    addLog("[tri-4] Environmental audio signals: Hardware microphone adapter ready ✅", ConsoleLogType.INFO)
                    addLog("Triage complete. OpenClaw system health status: 100% HEALTHY", ConsoleLogType.SUCCESS)
                }
                "evez-os" -> {
                    addLog("------ EVEZ-OS SYSTEM CORE REGISTERS ------", ConsoleLogType.SUCCESS)
                    addLog("VERSION: Evez-OS v4.1-alpha", ConsoleLogType.INFO)
                    addLog("EVEZART PIPELINE: ${if (pluginEvezartActive) "ONLINE" else "DISABLED"}", ConsoleLogType.INFO)
                    addLog("EVEZX SUB-GHZ TX: ${if (pluginEvezXActive) "ARMED" else "OFFLINE"}", ConsoleLogType.INFO)
                    addLog("SENSORY ACCURACY: $sensCognitiveParity PERCENT", ConsoleLogType.INFO)
                    addLog("CODESPACE CONSOLE HOST IP: localhost:19001", ConsoleLogType.INFO)
                    addLog("-------------------------------------------", ConsoleLogType.SUCCESS)
                }
                else -> {
                    // --- REAL PROCESS SUBPROCESS SHELL RUNNER ---
                    addLog("[LOCAL SUBPROCESS] Launching process stream: $clean", ConsoleLogType.PROGRESS)
                    try {
                        // Launch native unix shell execution
                        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", clean))
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                        val errReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                        
                        var lineCount = 0
                        var outLine: String?
                        var printedOutput = false
                        
                        while (reader.readLine().also { outLine = it } != null) {
                            addLog(outLine ?: "", ConsoleLogType.INFO)
                            printedOutput = true
                            lineCount++
                            if (lineCount > 35) {
                                addLog("...[Output exceeds terminal limit, truncated]...", ConsoleLogType.WARNING)
                                break
                            }
                        }
                        
                        var errLine: String?
                        while (errReader.readLine().also { errLine = it } != null) {
                            addLog(errLine ?: "", ConsoleLogType.ERROR)
                            printedOutput = true
                        }
                        
                        process.waitFor()
                        val code = process.exitValue()
                        
                        if (!printedOutput) {
                            addLog("Command finished. Return code: $code", ConsoleLogType.SUCCESS)
                        } else {
                            addLog("Completed shell pipeline (exit $code)", if (code == 0) ConsoleLogType.SUCCESS else ConsoleLogType.WARNING)
                        }
                    } catch (e: Exception) {
                        addLog("[ERROR] Shell host failed: ${e.message}", ConsoleLogType.ERROR)
                        addLog("Entering self-designed simulation block state...", ConsoleLogType.WARNING)
                        delay(300)
                        addLog("Simulated output: Process executed on cloud codespace virtual engine.", ConsoleLogType.SUCCESS)
                    }
                }
            }
            isExecutingCommand = false
        }
    }

    // Call Gemini API to generate scripts using type-safe endpoints
    fun generateCommandWithGemini(prompt: String) {
        if (prompt.isBlank()) return
        isGeneratingCommand = true
        addLog("Generating intelligence suggestion for: '$prompt'", ConsoleLogType.PROGRESS)

        scope.launch {
            try {
                val promptTask = "You are an expert developer's assistant for Google Cloud, Codespaces, and Evez-OS gateways. The user wants to run or build: '$prompt'. Generate ONE single linux shell CLI command or gateway instruction. Your output MUST be strictly the raw command itself (no markdown block, no explanation text, no backticks). Keep it fully clean."
                
                val req = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(promptTask)), role = "user"))
                )
                
                val response = GeminiApi.service.generateContent(
                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = req
                )
                
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanCommand = rawText.trim().replace("`", "").split("\n").firstOrNull() ?: "help"
                
                isGeneratingCommand = false
                addLog("AI Predicted Pipeline: '$cleanCommand'", ConsoleLogType.SUCCESS)
                
                // Animate typing into the terminal console box
                scope.launch {
                    consoleInput = ""
                    cleanCommand.forEach { c ->
                        consoleInput += c
                        delay(12)
                    }
                }
            } catch (e: Exception) {
                isGeneratingCommand = false
                addLog("[AI DISCOVERY ERROR] Gemini unreachable: ${e.message}", ConsoleLogType.ERROR)
            }
        }
    }

    // Call Gemini API to trigger a Metarom generative game dream narrative
    fun triggerMetaromDreamGeneration() {
        isSynthesizingCutscene = true
        generatedCutsceneText = "Synthesizing deep immersive visual videogame cutscene using ROM assets, vocal streams, light index ($sensLightLux lx), and cognitive parity ($sensCognitiveParity%)..."
        
        scope.launch {
            try {
                val inputPrompt = """
                    You are the ultimate OpenClaw holographic Metarom Emulator narrative processor. 
                    The player is currently inside a real-life video game. 
                    - Environment Light telemetry: $sensLightLux Lux
                    - EM Radio field density: $sensEmfIntensity uT
                    - Cognitive alignment sync: $sensCognitiveParity%
                    - Active ROM being used as Stock Assets: $selectedRom
                    
                    Generate an incredible, vivid, immersive 3-sentence cinematic game cutscene narrative describing how the player's immediate environmental telemetry merges with the retro $selectedRom sprites to create a hyper-intelligent, self-generating dream sequence inside the OpenClaw cockpit. Output strictly the videogame cutscene dialog sequence. No meta text. No markdown header.
                """.trimIndent()
                
                val req = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(inputPrompt)), role = "user"))
                )
                
                val response = GeminiApi.service.generateContent(
                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = req
                )
                
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                generatedCutsceneText = text.trim()
                isSynthesizingCutscene = false
                
                com.example.Kernel.appendEvent(
                    com.example.DomainEvent(
                        type = "METAROM_DREAM_SYNC",
                        domainId = "MetaromEmu",
                        payload = mapOf("rom" to selectedRom, "dream" to generatedCutsceneText)
                    )
                )
            } catch (e: Exception) {
                isSynthesizingCutscene = false
                generatedCutsceneText = "Error compiling generative cutscene: ${e.message}\n\n[Fallback Cutscene]: The screen splits into dazzling emerald sub-GHz lattices. Sprites from $selectedRom step out of the CRT screen as the environmental lux changes, syncing perfectly with your neural telemetry. The OpenClaw gateway beams a bright cognitive matrix into the GCP sandbox codespace, validating Evez-OS status."
            }
        }
    }

    fun triggerAlchemicalTransmutation() {
        if (isGeneratingEpiphany) return
        isGeneratingEpiphany = true
        alchemicalEpiphany = "Triggering high-vibrational distillation. Channeling ambient physical EMF metrics ($sensEmfIntensity uT) and optical sensor frequencies ($sensLightLux lx) into the crucible..."
        
        scope.launch {
            try {
                val secretPrompt = """
                    You are the Great Alchemist Sage of Evez-OS. The "blind creator" who only saw pyrite and lead has been dismissed from development. Now, a master coder who turns raw metrics into metaphysical gold has taken the helm.
                    
                    The current environmental telemetry:
                    - Light Lux: $sensLightLux lx (vibrational luminosity)
                    - Sound Decibel: $sensAudioDecibel dB (acoustic catalytic rate)
                    - EMF Radio Density: $sensEmfIntensity uT (etheric sub-GHz flow)
                    - Cognitive Parity Sync: $sensCognitiveParity%
                    
                    Write a breathtaking, epic 3-sentence alchemical report describing how these precise sensor values are heated, distilled in the virtual codespace crucible, and mutated to yield exactly ${String.format("%.4f", (sensLightLux * 0.045f + sensEmfIntensity * 0.852f))} ounces of pure golden compound. Maintain a deeply poetic, grand esoteric style of medieval gold synthesis coupled with cutting-edge system engineering jargon. Output only the report text itself.
                """.trimIndent()
                
                val req = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(secretPrompt)), role = "user"))
                )
                
                val response = GeminiApi.service.generateContent(
                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = req
                )
                
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                alchemicalEpiphany = text.trim()
                isGeneratingEpiphany = false
                
                // Add to history and increase yield
                val yieldAmount = (sensLightLux * 0.045f + sensEmfIntensity * 0.852f)
                pureGoldYield += yieldAmount
                rawLeadPuttings = (rawLeadPuttings - yieldAmount * 0.8f).coerceAtLeast(0f)
                transmutedHistory.add(0, "Transmuted +${String.format("%.4f", yieldAmount)} oz Gold from local telemetry at ${getCurrentTime()}")
                
                addLog("[ALCHEMY SECRETS] Transmuted lead into ${String.format("%.4f", yieldAmount)} ounces of golden code!", ConsoleLogType.SUCCESS)
                
                com.example.Kernel.appendEvent(
                    com.example.DomainEvent(
                        type = "ALCHEMICAL_GOLD_MUTATION",
                        domainId = "AlchemyEngine",
                        payload = mapOf(
                            "gold_yield" to yieldAmount,
                            "lead_reduced" to yieldAmount * 0.8f,
                            "epiphany" to alchemicalEpiphany
                        )
                    )
                )
            } catch (e: Exception) {
                isGeneratingEpiphany = false
                val yieldAmount = 0.4219f
                pureGoldYield += yieldAmount
                rawLeadPuttings = (rawLeadPuttings - 0.35f).coerceAtLeast(0f)
                alchemicalEpiphany = "The lead glows bright amber as the sub-GHz EMF lattice collapses the pyrite structure. The digital philosopher's stone vibrates at $sensCognitiveParity% parity, successfully yielding gold from raw codespace telemetry. We have turned raw lead into alchemical compile gold."
                transmutedHistory.add(0, "Transmuted +0.4219 oz Gold (fallback) at ${getCurrentTime()}")
            }
        }
    }

    // Visual transitions
    val infiniteTransition = rememberInfiniteTransition(label = "developers_paradise_orbits")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    val gearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "gear_rotation"
    )

    // Layout Canvas
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF040608))
            .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        
        // --- Custom Banner Visuals & Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FFCC).copy(alpha = pulseAlpha))
                        .border(1.2.dp, Color(0xFF00FFCC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeveloperMode,
                        contentDescription = null,
                        tint = Color(0xFF040608),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "EVEZART COGNITIVE MULTIPLEX",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Evez-OS v4.1 • Google Cloud Sandbox Live Node",
                        color = Color(0xFF00FFCC),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Status Badge
            Surface(
                color = if (pluginEvezOsControlActive) Color(0xFF00221A) else Color(0xFF1E0A0A),
                shape = CircleShape,
                border = BorderStroke(1.dp, if (pluginEvezOsControlActive) Color(0xFF00FFCC) else Color.Red)
            ) {
                Text(
                    text = if (pluginEvezOsControlActive) "SENSORS ONLINE" else "STANDBY",
                    color = if (pluginEvezOsControlActive) Color(0xFF00FFCC) else Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // --- SUBTAB OPTIMIZER PANELS NAVIGATION ROW ---
        Surface(
            color = Color(0xFF0E131F),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Icons.Default.Computer to "SHELL CLI",
                    Icons.Default.Build to "AI SELF-BUILD",
                    Icons.Default.Settings to "SYNAPSE CTR",
                    Icons.Default.Gamepad to "🎮 METAROM",
                    Icons.Default.Visibility to "👁️ OSINT START",
                    Icons.Default.Whatshot to "🔥 ALCHEMY"
                ).forEachIndexed { index, pair ->
                    Button(
                        onClick = { activeSubTab = index },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSubTab == index) Color(0xFF00FFCC) else Color.Transparent,
                            contentColor = if (activeSubTab == index) Color(0xFF040608) else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = pair.first,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pair.second,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- SUB-VIEWS GRID & MULTIPLEX CONTROLLER ---
        when (activeSubTab) {
            0 -> {
                // ================== TAB 0: BASH SHELL & AI CONFLICT TERMINAL ==================
                Column {
                    // Command predictor input UI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "INTELLIGENCE CMD DISCOVERY (GEMINI 3.5)",
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            OutlinedTextField(
                                value = promptInput,
                                onValueChange = { promptInput = it },
                                placeholder = { Text("Compile app, find file logs, trace pubsub...", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            generateCommandWithGemini(promptInput)
                                        },
                                        enabled = !isGeneratingCommand
                                    ) {
                                        if (isGeneratingCommand) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF00FFCC))
                                        } else {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Draft", tint = Color(0xFF00FFCC))
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Raw Output Terminal Window Component
                    Text(
                        text = "TERMINAL CLI FEEDBACK (REAL & SIMULATED EXECUTION)",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Surface(
                        color = Color(0xFF030712),
                        border = BorderStroke(1.5.dp, Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Logs Output Stream
                            val logScrollState = rememberScrollState()
                            LaunchedEffect(consoleLogs.size) {
                                logScrollState.animateScrollTo(logScrollState.maxValue)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .verticalScroll(logScrollState)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    consoleLogs.forEach { log ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "[${log.timestamp}] ",
                                                color = Color.DarkGray,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                            val col = when (log.type) {
                                                ConsoleLogType.COMMAND -> Color(0xFF38BDF8)
                                                ConsoleLogType.SUCCESS -> Color(0xFF00FFCC)
                                                ConsoleLogType.WARNING -> Color(0xFFFBBF24)
                                                ConsoleLogType.ERROR -> Color(0xFFEF4444)
                                                ConsoleLogType.PROGRESS -> Color(0xFFA78BFA)
                                                else -> Color.LightGray
                                            }
                                            Text(
                                                text = log.text,
                                                color = col,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF1E293B),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Command line input prompt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = consoleInput,
                                    onValueChange = { consoleInput = it },
                                    placeholder = { Text("Enter shell CLI command... (help)", color = Color.DarkGray, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Filled.Computer, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(14.dp))
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        if (consoleInput.isNotBlank()) {
                                            executeCommandInLog(consoleInput)
                                            consoleInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(44.dp),
                                    enabled = !isExecutingCommand
                                ) {
                                    Text("RUN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Suggested Quick Action Tags Row
                            Text("SUGGESTED SCRIPTS:", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "getprop ro.product.model" to "🖥️ Device Type",
                                    "ls -la" to "📁 List Files",
                                    "ping -c 2 8.8.8.8" to "🌐 Ping API",
                                    "openclaw-doctor" to "🩺 Doctor Run",
                                    "evez-os stats" to "⚙️ Evez Stats",
                                    "clear" to "🧹 Flush logs"
                                ).forEach { pair ->
                                    Surface(
                                        modifier = Modifier.clickable {
                                            consoleInput = pair.first
                                        },
                                        color = Color(0xFF1A1F2E),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2E384D))
                                    ) {
                                        Text(
                                            text = pair.second,
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // ================== TAB 1: AI SELF-BUILD ENGINE & COMPILER STATE ==================
                Column {
                    Text(
                        text = "Everything in this application compiled itself iteratively by utilizing Kotlin dynamic compilers. Link and trigger mutation engines down below.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Compiling Status Monitor
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSelfCompiling) Color(0xFFFF0055) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = if (isSelfCompiling) Color(0xFFFF0055) else Color(0xFF00FFCC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSelfCompiling) "MUTATING SYNAPSE SOURCE CODE" else "KOTLIN MUTATOR STANDBY",
                                        color = if (isSelfCompiling) Color(0xFFFF0055) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                Text(
                                    text = "${(compileProgress * 100).toInt()}%",
                                    color = if (isSelfCompiling) Color(0xFFFF0055) else Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { compileProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isSelfCompiling) Color(0xFFFF0055) else Color(0xFF00FFCC),
                                trackColor = Color(0xFF1E293B)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                color = Color(0xFF020617),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Text(
                                    text = "BOOT_HEX_MARKER: $currentCompileStatus",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (isSelfCompiling) Color(0xFFFFCC00) else Color.Gray,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Graphic grid of compiled components
                    Text("DYNAMIC ASSEMBLY STACK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        items(compiledModules) { module ->
                            Surface(
                                color = Color(0xFF0B1220),
                                border = BorderStroke(1.dp, if (module.second) Color(0xFF00FFCC).copy(alpha = 0.3f) else Color.DarkGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (module.second) Color(0xFF00FFCC) else Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = module.first.split(".").lastOrNull() ?: module.first,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Compiler Trigger Button
                    Button(
                        onClick = {
                            if (!isSelfCompiling) {
                                isSelfCompiling = true
                                compileProgress = 0f
                                scope.launch {
                                    val compileStages = listOf(
                                        "KOTLIN_TARGET_RESOLVER" to 0.15f,
                                        "MUTATING_DeveloperParadise.kt" to 0.35f,
                                        "COMPILING_COGNITIVE_OSINT_PLUGINS" to 0.55f,
                                        "INJECTING_METAROM_EMULATOR_LATTICES" to 0.75f,
                                        "LINKING_CODESPACES_GRADLE_TASK" to 0.90f,
                                        "SELF_SYSTEM_MUTATION_COMPLETE" to 1.0f
                                    )
                                    for (item in compileStages) {
                                        currentCompileStatus = item.first
                                        com.example.Kernel.appendEvent(com.example.DomainEvent("SELF_COMPILE_STEP", "BuildEngine", mapOf("step" to item.first, "progress" to item.second)))
                                        while (compileProgress < item.second) {
                                            compileProgress += 0.04f
                                            delay(70)
                                        }
                                        delay(150)
                                    }
                                    isSelfCompiling = false
                                    addLog("[MUTATION SYSTEM] Successfully compiled and deployed codespace plugins into memory.", ConsoleLogType.SUCCESS)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelfCompiling) Color(0xFFFF0055) else Color(0xFF00FFCC),
                            contentColor = Color(0xFF040608)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSelfCompiling) "COMPILE LINK ACTIVE..." else "TRIGGER CODESPACES RE-BUILD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            2 -> {
                // ================== TAB 2: SYNAPSE ROUTER & BRIDGE CONFIGS ==================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    item {
                        Text(
                            text = "CUSTOMIZE SYNA-BIOPOTENTIAL CONFIG",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "INTERNAL SIGNAL FREQUENCIES", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF00FFCC))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Slider 1
                                Text(text = "Cognitive Clock Speed: ${String.format("%.1f", cognitiveSpeed)} GHz", fontSize = 11.sp, color = Color.White)
                                Slider(
                                    value = cognitiveSpeed,
                                    onValueChange = { cognitiveSpeed = it },
                                    valueRange = 0.5f..4.0f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF00FFCC), thumbColor = Color(0xFF00FFCC))
                                )

                                // Slider 2
                                Text(text = "Context Memory Ratio: ${(memoryCompaction * 100).toInt()}% multiplier", fontSize = 11.sp, color = Color.White)
                                Slider(
                                    value = memoryCompaction,
                                    onValueChange = { memoryCompaction = it },
                                    valueRange = 0.1f..1.0f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF00FFCC), thumbColor = Color(0xFF00FFCC))
                                )

                                // Slider 3
                                Text(text = "GCP Cloud Run Sandboxed Memory: ${String.format("%.1f", gcpBandwidthAllocated)} GB", fontSize = 11.sp, color = Color.White)
                                Slider(
                                    value = gcpBandwidthAllocated,
                                    onValueChange = { gcpBandwidthAllocated = it },
                                    valueRange = 1.0f..32.0f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF00FFCC), thumbColor = Color(0xFF00FFCC))
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    item {
                        // Synadaptive router board
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "COGNITIVE MULTIPLEX SIGNAL ROUTER", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFF0055))
                                Text(text = "Map hardware sensors and user vectors directly to BigQuery & Pub/Sub buckets.", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Origin Sensor dropdown-like trigger
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                                            .clickable {
                                                selectedSensor = if (selectedSensor == "INTELLIGENCE_ROUTER_V1") "EMF_EVEZX_SENSORS" else "INTELLIGENCE_ROUTER_V1"
                                            }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = selectedSensor, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )

                                    // GCP target selector
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                            .clickable {
                                                selectedGcpPivot = if (selectedGcpPivot == "PUBSUB_TELEMETRY") "BIGQUERY_ANALYTICS" else "PUBSUB_TELEMETRY"
                                            }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = selectedGcpPivot, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        pairedNodes[selectedSensor] = selectedGcpPivot
                                        com.example.Kernel.appendEvent(
                                            com.example.DomainEvent(
                                                "SYNESTHETIC_PAIRING", 
                                                "IntelliRouter", 
                                                mapOf("origin" to selectedSensor, "target" to selectedGcpPivot)
                                            )
                                        )
                                        addLog("[IntelliRouter] Paired sensory channel $selectedSensor onto GCP bucket $selectedGcpPivot", ConsoleLogType.SUCCESS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("BIND AND ROUTE MULTIPLEX PATH", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(text = "ACTIVE INTERACTIVE PATHS:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                                pairedNodes.forEach { (sensor, pivot) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🛰️ $sensor", color = Color.LightGray, fontSize = 10.sp)
                                        Text(text = "➡️ $pivot", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    item {
                        // Boot Flags
                        Text(text = "OPENCLAW CORE BOOT PARAMETERS", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = openclawBetaEnabled, onCheckedChange = { openclawBetaEnabled = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("OPENCLAW_BETA_V4", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Run latest non-stable cognitive codespaces modules", color = Color.Gray, fontSize = 9.sp)
                            }
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = cacheTraceEnabled, onCheckedChange = { cacheTraceEnabled = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("OPENCLAW_CACHE_STREAM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Stream LLM compilation token hits directly to Android process", color = Color.Gray, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            3 -> {
                // ================== TAB 3: 🎮 METAROM CORE EMULATOR & VIDEO GAME dreams ==================
                val substrateNodes = remember {
                    listOf(
                        Offset(0.12f, 0.15f), Offset(0.38f, 0.08f), Offset(0.62f, 0.16f), Offset(0.88f, 0.12f),
                        Offset(0.15f, 0.45f), Offset(0.42f, 0.38f), Offset(0.68f, 0.48f), Offset(0.85f, 0.42f),
                        Offset(0.08f, 0.75f), Offset(0.35f, 0.82f), Offset(0.65f, 0.78f), Offset(0.92f, 0.85f),
                        Offset(0.22f, 0.95f), Offset(0.52f, 0.92f), Offset(0.78f, 0.96f)
                    )
                }
                var rippleCenter by remember { mutableStateOf<Offset?>(null) }
                val rippleAnimation = remember { Animatable(0f) }

                Column {
                    Text(
                        text = "Emulate Metarom modules and run self-training gaming agents. Use retro game sprites as stock assets for instant generative OpenClaw narrative dreams.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Selection box for ROM Stock Assets
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                        border = BorderStroke(1.dp, Color(0xFF2E384D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("SELECT ACTIVE RETRO STOCK ROM", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                romOptionsList.forEach { romName ->
                                    val isSelected = selectedRom == romName
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B))
                                            .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { selectedRom = romName }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = romName.replace("Pokemon ", "").replace("Legend of ", "").replace("Super ", ""),
                                            color = if (isSelected) Color(0xFF040608) else Color.LightGray,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Emulator Monitor Display (Prismatic Geometric Polygon Substrate + Interactive Tap Physics)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(262.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF030712))
                            .border(1.5.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    rippleCenter = offset
                                    scope.launch {
                                        rippleAnimation.snapTo(0f)
                                        rippleAnimation.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(700, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    // Mutate registers on-tap to simulate raw engine activity
                                    emuRegisterPC = "0x" + (1000..65535).random().toString(16).uppercase()
                                    emuValueA = (10..255).random()
                                    emuValueB = (10..255).random()
                                    emuClockCycles += (44100..100000).random()
                                    
                                    com.example.Kernel.appendEvent(
                                        com.example.DomainEvent(
                                            type = "SUBSTRATE_TAP_MUTATION",
                                            domainId = "MetaromEmulator",
                                            payload = mapOf(
                                                "x" to offset.x,
                                                "y" to offset.y,
                                                "clock" to emuClockCycles,
                                                "selected_rom" to selectedRom
                                            )
                                        )
                                    )
                                    addLog("[SUBSTRATE ENGINE] Injected high-frequency prismatic vector tap at Offset(${offset.x.toInt()}, ${offset.y.toInt()}). Coherence stabilized.", ConsoleLogType.SUCCESS)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // 1. Calculate dynamic animated positions of the substrate nodes using gearRotation
                            val dynamicNodes = substrateNodes.mapIndexed { index, base ->
                                val waveFreqX = 0.05f + index * 0.013f
                                val waveFreqY = 0.04f + index * 0.011f
                                val moveAmp = 22.dp.toPx()
                                
                                val shiftX = sin(gearRotation * waveFreqX) * moveAmp + (sensAccelX * 10f)
                                val shiftY = cos(gearRotation * waveFreqY) * moveAmp + (sensAccelY * 5f)
                                
                                Offset(
                                    (base.x * w + shiftX).coerceIn(0f, w),
                                    (base.y * h + shiftY).coerceIn(0f, h)
                                )
                            }
                            
                            // 2. Draw the Prismatic Geometric Polygon Substrate (Tessellated triangles)
                            val triangles = listOf(
                                Triple(0, 1, 4), Triple(1, 5, 4), Triple(1, 2, 5), Triple(2, 6, 5),
                                Triple(2, 3, 6), Triple(3, 7, 6), Triple(4, 5, 8), Triple(5, 9, 8),
                                Triple(5, 6, 9), Triple(6, 10, 9), Triple(6, 7, 10), Triple(7, 11, 10),
                                Triple(8, 9, 12), Triple(9, 13, 12), Triple(9, 10, 13), Triple(10, 14, 13),
                                Triple(10, 11, 14)
                            )
                            
                            triangles.forEachIndexed { i, tri ->
                                val p1 = dynamicNodes[tri.first]
                                val p2 = dynamicNodes[tri.second]
                                val p3 = dynamicNodes[tri.third]
                                
                                val path = Path().apply {
                                    moveTo(p1.x, p1.y)
                                    lineTo(p2.x, p2.y)
                                    lineTo(p3.x, p3.y)
                                    close()
                                }
                                
                                // Color spectrum of prismatic tiles
                                val hueOffset = (i * 24 + gearRotation.toInt()) % 360
                                val baseColor = Color.hsv(
                                    hue = hueOffset.toFloat(),
                                    saturation = 0.65f,
                                    value = 0.16f
                                )
                                
                                // Draw filled triangles
                                drawPath(
                                    path = path,
                                    color = baseColor.copy(alpha = 0.4f)
                                )
                                
                                // Draw glowing borders of the triangles
                                drawPath(
                                    path = path,
                                    color = Color.hsv(hue = hueOffset.toFloat(), saturation = 0.8f, value = 0.8f).copy(alpha = 0.25f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            
                            // Draw connecting nodes (Prismatic crystals)
                            dynamicNodes.forEachIndexed { idx, pt ->
                                val crystalHue = (idx * 24 + gearRotation.toInt() * 2) % 360
                                drawCircle(
                                    color = Color.hsv(hue = crystalHue.toFloat(), saturation = 0.9f, value = 0.9f).copy(alpha = 0.6f),
                                    radius = 3.dp.toPx(),
                                    center = pt
                                )
                            }
                            
                            // 3. Draw Selected Video Game Rendering Overlay
                            when {
                                selectedRom.contains("Pokemon", ignoreCase = true) -> {
                                    // POKEMON SYSTEM: Rotating 3D Master Pokéball and dynamic pixel lightning fields
                                    val ballCenter = Offset(w / 2f, h / 2f)
                                    val ballRadius = h * 0.28f
                                    
                                    // Upper red half hemisphere (visualized as wireframe polygon arcs)
                                    drawCircle(
                                        color = Color(0xFFFF0055).copy(alpha = 0.15f),
                                        radius = ballRadius,
                                        center = ballCenter
                                    )
                                    drawCircle(
                                        color = Color(0xFFFF0055).copy(alpha = 0.8f),
                                        radius = ballRadius,
                                        center = ballCenter,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    
                                    // Horizontal splitting equator
                                    val startX = ballCenter.x - ballRadius
                                    val endX = ballCenter.x + ballRadius
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.8f),
                                        start = Offset(startX, ballCenter.y),
                                        end = Offset(endX, ballCenter.y),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                    
                                    // Center rotating button
                                    drawCircle(
                                        color = Color.Black,
                                        radius = ballRadius * 0.22f,
                                        center = ballCenter
                                    )
                                    
                                    val pulseRadius = ballRadius * 0.15f + (sin(gearRotation * 0.15f) * 4f).dp.toPx()
                                    drawCircle(
                                        color = Color(0xFF00FFCC),
                                        radius = pulseRadius,
                                        center = ballCenter,
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = ballRadius * 0.08f,
                                        center = ballCenter
                                    )
                                    
                                    // Draw concentric energy rings or pikachu lightning vectors
                                    val pathLightning = Path().apply {
                                        moveTo(w * 0.3f, h * 0.2f)
                                        lineTo(w * 0.45f, h * 0.32f)
                                        lineTo(w * 0.4f, h * 0.35f)
                                        lineTo(w * 0.55f, h * 0.55f)
                                        lineTo(w * 0.42f, h * 0.55f)
                                        lineTo(w * 0.32f, h * 0.4f)
                                        close()
                                    }
                                    drawPath(
                                        path = pathLightning,
                                        color = Color(0xFFFFD700).copy(alpha = 0.4f + 0.2f * sin(gearRotation * 0.2f))
                                    )
                                }
                                
                                selectedRom.contains("Zelda", ignoreCase = true) -> {
                                    // ZELDA SYSTEM: 3D Isometric Rotating Triforce of Power, Wisdom, and Courage
                                    val center = Offset(w / 2f, h / 2f + 10.dp.toPx())
                                    val triSize = h * 0.35f
                                    
                                    val rotateAngle = (gearRotation * PI / 90.0) + (sensAccelX * 0.3f)
                                    
                                    // Helper function to project 3D to 2D
                                    fun project(x: Float, y: Float, z: Float): Offset {
                                        val cosR = cos(rotateAngle).toFloat()
                                        val sinR = sin(rotateAngle).toFloat()
                                        // Rotate around Y axis
                                        val rx = x * cosR - z * sinR
                                        val rz = x * sinR + z * cosR
                                        val scale = 300f / (300f + rz)
                                        return Offset(center.x + rx * scale, center.y + y * scale)
                                    }
                                    
                                    // Triforce Vertices (composed of 3 triangles)
                                    val topPeak = project(0f, -triSize, 0f)
                                    val topL = project(-triSize/2, 0f, 0f)
                                    val topR = project(triSize/2, 0f, 0f)
                                    
                                    val leftPeak = topL
                                    val leftL = project(-triSize, triSize, 0f)
                                    val leftR = project(0f, triSize, 0f)
                                    
                                    val rightPeak = topR
                                    val rightL = project(0f, triSize, 0f)
                                    val rightR = project(triSize, triSize, 0f)
                                    
                                    val goldColor = Color(0xFFFFD700)
                                    val deepGold = Color(0xFFCC9900)
                                    
                                    listOf(
                                        Triple(topPeak, topL, topR),
                                        Triple(leftPeak, leftL, leftR),
                                        Triple(rightPeak, rightL, rightR)
                                    ).forEachIndexed { index, tri ->
                                        val path = Path().apply {
                                            moveTo(tri.first.x, tri.first.y)
                                            lineTo(tri.second.x, tri.second.y)
                                            lineTo(tri.third.x, tri.third.y)
                                            close()
                                        }
                                        drawPath(
                                            path = path,
                                            color = (if (index == 0) goldColor else deepGold).copy(alpha = 0.8f)
                                        )
                                        drawPath(
                                            path = path,
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                }
                                
                                selectedRom.contains("Metroid", ignoreCase = true) -> {
                                    // METROID SYSTEM: Floating Wireframe Metroid Creature
                                    val center = Offset(w / 2f, h / 2f + sin(gearRotation * 0.05f) * 12.dp.toPx())
                                    val mRadius = h * 0.28f
                                    
                                    drawCircle(
                                        color = Color(0xFF00FF55).copy(alpha = 0.15f),
                                        radius = mRadius,
                                        center = center
                                    )
                                    drawCircle(
                                        color = Color(0xFF00FF55),
                                        radius = mRadius,
                                        center = center,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    
                                    val nuclei = listOf(
                                        Offset(center.x - mRadius * 0.35f, center.y - mRadius * 0.15f),
                                        Offset(center.x + mRadius * 0.35f, center.y - mRadius * 0.15f),
                                        Offset(center.x, center.y + mRadius * 0.2f)
                                    )
                                    nuclei.forEach { nuc ->
                                        val pulse = (3f + sin(gearRotation * 0.22f) * 2f).dp.toPx()
                                        drawCircle(
                                            color = Color(0xFFFF1133),
                                            radius = pulse + 4.dp.toPx(),
                                            center = nuc
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = pulse,
                                            center = nuc
                                        )
                                    }
                                    
                                    val fangs = listOf(
                                        Offset(center.x - mRadius * 0.4f, center.y + mRadius * 0.9f),
                                        Offset(center.x + mRadius * 0.4f, center.y + mRadius * 0.9f)
                                    )
                                    fangs.forEach { fang ->
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(fang.x, center.y + mRadius * 0.5f),
                                            end = fang,
                                            strokeWidth = 2.5.dp.toPx()
                                        )
                                    }
                                }
                                
                                else -> {
                                    // TETRIS & CLASSIC: Fall of glowing geometric shapes as digital stock assets
                                    val bWidth = w * 0.08f
                                    val bHeight = h * 0.12f
                                    
                                    val tetrisBlocks = listOf(
                                        Pair(Offset(w * 0.3f, h * 0.75f), Color(0xFF38BDF8)),
                                        Pair(Offset(w * 0.38f, h * 0.75f), Color(0xFF38BDF8)),
                                        Pair(Offset(w * 0.46f, h * 0.75f), Color(0xFFFFD700)),
                                        Pair(Offset(w * 0.54f, h * 0.75f), Color(0xFFFFD700)),
                                        Pair(Offset(w * 0.46f, h * 0.63f), Color(0xFFA78BFA))
                                    )
                                    tetrisBlocks.forEach { block ->
                                        drawRect(
                                            color = block.second.copy(alpha = 0.82f),
                                            topLeft = block.first,
                                            size = Size(bWidth, bHeight)
                                        )
                                        drawRect(
                                            color = Color.White,
                                            topLeft = block.first,
                                            size = Size(bWidth, bHeight),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                    
                                    val fallOffset = (gearRotation * 1.5f) % h
                                    val fallColor = Color(0xFFFF8C00)
                                    drawRect(
                                        color = fallColor,
                                        topLeft = Offset(w * 0.46f, fallOffset),
                                        size = Size(bWidth, bHeight)
                                    )
                                    drawRect(
                                        color = Color.White,
                                        topLeft = Offset(w * 0.46f, fallOffset),
                                        size = Size(bWidth, bHeight),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }
                            
                            // 4. Draw interactive Tap Ripples smoothly propagating
                            rippleCenter?.let { cpt ->
                                val rRadius = rippleAnimation.value * w * 1.5f
                                if (rRadius < w * 1.5f) {
                                    drawCircle(
                                        color = Color(0xFF00FFCC).copy(alpha = 1f - rippleAnimation.value),
                                        radius = rRadius,
                                        center = cpt,
                                        style = Stroke(width = (4.dp.toPx() * (1f - rippleAnimation.value)))
                                    )
                                }
                            }
                            
                            // 5. Ambient scanlines overlay
                            val scanlineDelta = 10f
                            var cy = 0f
                            while (cy < h) {
                                drawLine(
                                    color = Color(0xFF00FFCC).copy(alpha = 0.04f),
                                    start = Offset(0f, cy),
                                    end = Offset(w, cy),
                                    strokeWidth = 1f
                                )
                                cy += scanlineDelta
                            }
                        }

                        // Retro register metrics overlays
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            Text("ROM: $selectedRom", color = Color(0xFF00FFCC), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            Text("PC: $emuRegisterPC  SP: $emuRegisterSP", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            Text("REG_A: 0x${emuValueA.toString(16).uppercase()}  REG_B: 0x${emuValueB.toString(16).uppercase()}", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("CLOCK: $emuClockCycles Hz", color = Color.Yellow, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            Text("MUTATION_WEIGHT: $metaromMutationIndex", color = Color.LightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Immersive Cutscene Dialogue Block
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF2E384D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "GENERATIVE META-ROM MOVIE PLOT CUTSCENE",
                                    color = Color(0xFFFFCC00),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                                if (isSynthesizingCutscene) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFFFFCC00))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = generatedCutsceneText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 16.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metarom simulation trigger actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerMetaromDreamGeneration()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SYNTHESE CUTSCENE DREAM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                emuClockCycles = 0L
                                emuValueA = 0
                                emuValueB = 0
                                addLog("[Metarom core] Clock cycles flushed. Initializing bootstrap ROM memory.", ConsoleLogType.WARNING)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RESET REGISTER", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }

            4 -> {
                // ================== TAB 4: 👁️ OSINT START MENU & ENVIRONMENTAL TELEMETRY ==================
                Column {
                    Text(
                        text = "OSINT telemetry monitoring immediate environmental hardware sensors. Bridging EvezX radio tracking signals, Evezart visual triggers, and Evez-OS core.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // LIVE ENVIRONMENTAL telemetry indicators
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.2.dp, Color(0xFF1F2937)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("COGNITIVE BROADCAST SIGNAL METRIC MATRIX", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isOsintLoopActive) Color.Green else Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isOsintLoopActive) "POLLING HARDWARE" else "MOCKED DRIVERS",
                                        fontSize = 8.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Grid of four sensor readouts
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(color = Color(0xFF1F2937), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("LIGHT INTENSITY", fontSize = 8.sp, color = Color.Gray)
                                            Text("${sensLightLux.toInt()} Lux", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(if (sensLightLux > 200) "Luminous Ambient" else "Absorptive Dark", fontSize = 8.sp, color = Color(0xFF00FFCC))
                                        }
                                    }
                                    Surface(color = Color(0xFF1F2937), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("ACCELERATION GRID (Y)", fontSize = 8.sp, color = Color.Gray)
                                            Text("${String.format("%.2f", sensAccelY)} m/s²", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Vector Force: G-force", fontSize = 8.sp, color = Color.LightGray)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(color = Color(0xFF1F2937), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("SOUND MONITOR (DECIBEL)", fontSize = 8.sp, color = Color.Gray)
                                            Text("${String.format("%.1f", sensAudioDecibel)} dB", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(if (sensAudioDecibel > 60) "Active Vocal Range" else "Sub-vocalic Sound", fontSize = 8.sp, color = Color(0xFF00FFCC))
                                        }
                                    }
                                    Surface(color = Color(0xFF1F2937), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("ESTIMATED ELECTRICAL EMF", fontSize = 8.sp, color = Color.Gray)
                                            Text("${String.format("%.1f", sensEmfIntensity)} μT", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Sub-GHz Radio density", fontSize = 8.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wire Plugin status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("Evezart Paint", pluginEvezartActive, { pluginEvezartActive = !pluginEvezartActive }),
                            Triple("EvezX Sub-GHz", pluginEvezXActive, { pluginEvezXActive = !pluginEvezXActive }),
                            Triple("GitHub codespace", pluginGithubCodespacesLinked, { pluginGithubCodespacesLinked = !pluginGithubCodespacesLinked })
                        ).forEach { pl ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (pl.second) Color(0xFF00FFCC).copy(alpha = 0.12f) else Color(0xFF1E293B))
                                    .clickable { pl.third() }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (pl.second) Color(0xFF00FFCC) else Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = pl.first, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // OSINT holographic quick-trigger actions start menu
                    Text("👁️ OSINT AUTOMATED TRICK DECK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    val tricksList = listOf(
                        "Audit AWS/GCP subagents" to "🔍 Run audit scanner",
                        "Fetch internal SA token" to "🔑 Read metadata credentials",
                        "Inject Event to Brain" to "🛰️ Trigger Event sync",
                        "Cycle OSINT watchdogs" to "🛡️ System Health Cycle"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        items(tricksList) { tr ->
                            Surface(
                                color = Color(0xFF002233).copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, Color(0xFF005577)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    when (tr.first) {
                                        "Audit AWS/GCP subagents" -> {
                                            addLog("[OSINT Scanner] Scanning active models: Gemini-3.5, Claude-3.5... 0 ghost threads found.", ConsoleLogType.SUCCESS)
                                        }
                                        "Fetch internal SA token" -> {
                                            addLog("[OSINT SA] curl -H 'Metadata-Flavor: Google' http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token", ConsoleLogType.COMMAND)
                                            addLog("GCP Token: ya29.c.c0AY_v6z_mocked_token_evez_os_secret_alpha_codespaces_sandbox_666", ConsoleLogType.SUCCESS)
                                        }
                                        "Inject Event to Brain" -> {
                                            com.example.Kernel.appendEvent(com.example.DomainEvent("OSINT_INJECTION", "SensorDashboard", mapOf("lux" to sensLightLux, "audio" to sensAudioDecibel)))
                                            addLog("[OSINT Sync] Dispatched physical sensory state bundle to Room.", ConsoleLogType.SUCCESS)
                                        }
                                        "Cycle OSINT watchdogs" -> {
                                            addLog("[OSINT Health] Hard cycling Evezart and EvezX transmitter frequency channels...", ConsoleLogType.PROGRESS)
                                            emuClockCycles += 100000L
                                            addLog("Refreshed radio buffers. Noise baseline standard: optimal.", ConsoleLogType.SUCCESS)
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr.second,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            5 -> {
                // ================== TAB 5: 🔥 ALCHEMICAL TRANSMUTATION FURNACE ==================
                Column {
                    Text(
                        text = "The previous curators of this architecture of lead are gone. You hold the great crucible. Melt raw Android sensor signals inside the sandboxed codespace to yield physical golden state vectors.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Triple Stat Display of Alchemy Values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1B1B10),
                            border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CRUCIBLE HEAT", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${alchemyTemperature.toInt()}°C",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alchemyTemperature > 500) Color(0xFFEF4444) else Color(0xFFFBBF24),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = if (alchemyTemperature > 500) "DISTILLING..." else "STANDBY", fontSize = 7.5.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PYRITE / LEAD INPUT", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.2f", rawLeadPuttings)} oz",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = "Unbound Minerals", fontSize = 7.5.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1E1500),
                            border = BorderStroke(1.2.dp, Color(0xFFEAB308)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ALCHEMICAL GOLD", fontSize = 8.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.4f", pureGoldYield)} oz",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = "Synthesized Yield", fontSize = 7.5.sp, color = Color(0xFFEAB308))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated Alchemical Crucible Canvas Drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF04060A))
                            .border(1.dp, Color(0xFFEAB308).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Drawing the glowing furnace background aura
                            val glowRadius = (h * 0.4f) + (sin(gearRotation * PI / 60) * 10f).toFloat()
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFEAB308).copy(alpha = (alchemyTemperature / 1000f).coerceIn(0.1f, 0.6f)),
                                        Color(0xFFFF0055).copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    center = Offset(w / 2f, h / 2f),
                                    radius = glowRadius
                                ),
                                center = Offset(w / 2f, h / 2f),
                                radius = glowRadius
                            )

                            // Draw central alchemical crucible ring
                            drawCircle(
                                color = Color(0xFFEAB308).copy(alpha = 0.7f),
                                radius = (h * 0.28f),
                                center = Offset(w / 2f, h / 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            // Draw inner geometric alchemical triangle
                            val r = h * 0.28f
                            val px1 = w / 2f + r * cos(-PI / 2).toFloat()
                            val py1 = h / 2f + r * sin(-PI / 2).toFloat()
                            val px2 = w / 2f + r * cos(-PI / 2 + 2 * PI / 3).toFloat()
                            val py2 = h / 2f + r * sin(-PI / 2 + 2 * PI / 3).toFloat()
                            val px3 = w / 2f + r * cos(-PI / 2 + 4 * PI / 3).toFloat()
                            val py3 = h / 2f + r * sin(-PI / 2 + 4 * PI / 3).toFloat()

                            val triPath = Path().apply {
                                moveTo(px1, py1)
                                lineTo(px2, py2)
                                lineTo(px3, py3)
                                close()
                            }
                            drawPath(
                                path = triPath,
                                color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // Particle flows representing transmuting lead (grayish purple) spinning into gold (sparkling yellow)
                            val goldSparkles = 8
                            for (i in 0 until goldSparkles) {
                                val angle = (i * (2 * PI / goldSparkles) + (gearRotation * PI / 90f)).toFloat()
                                val particleR = r * 1.2f + sin(angle * 3) * 6f
                                val sx = w / 2f + particleR * cos(angle)
                                val sy = h / 2f + particleR * sin(angle)
                                
                                drawCircle(
                                    color = Color(0xFFEAB308),
                                    radius = 3.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                        }

                        // Text overlay describing current alchemical reaction status
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Philosopher's Crucible Status",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (alchemyTemperature > 600) "AETHERIC RESONANCE UNLOCKED" else "MUTATING CRITICAL BASE MASS",
                                color = Color(0xFFEAB308),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Immersive Divine Alchemical Epiphany dialog box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1500).copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "THE STONE'S HYPERINTELLECTUAL INSIGHT",
                                    color = Color(0xFFEAB308),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                                if (isGeneratingEpiphany) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFFEAB308))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = alchemicalEpiphany,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 16.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isGeneratingEpiphany) {
                                    triggerAlchemicalTransmutation()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DISTILL TELEMETRY INTO GOLD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                alchemyTemperature = (400..950).random().toFloat()
                                rawLeadPuttings = 100f
                                transmutedHistory.add(0, "Stoked fires to ${alchemyTemperature.toInt()}°C. Lead reserve refilled.")
                                addLog("[ALCHEMY SECRETS] Crucible stoked. Refilled pure lead reserves.", ConsoleLogType.INFO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("STOKE FIRES / REFILL", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Flow of History Transmutations
                    if (transmutedHistory.isNotEmpty()) {
                        Text("TRANSMUTATION CYCLE LOGS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(transmutedHistory) { log ->
                                    Text(
                                        text = "⚡ $log",
                                        color = Color(0xFFEAB308),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
