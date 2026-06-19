package com.example

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BrowserTab(val id: Int, var url: String, var title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantumBrowserDialog(database: AppDatabase, onClose: () -> Unit) {
    var tabs by remember { mutableStateOf(listOf(BrowserTab(0, "https://google.com", "New Tab"))) }
    var currentTabId by remember { mutableIntStateOf(0) }
    var tabCounter by remember { mutableIntStateOf(1) }

    var inputUrl by remember { mutableStateOf("https://google.com") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var pageContent by remember { mutableStateOf("") }
    
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showGemini by remember { mutableStateOf(false) }
    var geminiResponse by remember { mutableStateOf("") }
    var isLoadingGemini by remember { mutableStateOf(false) }
    var customPrompt by remember { mutableStateOf("") }
    
    val autoPrompts = listOf(
        "Summarize the key information.",
        "What acts as the entropy void forming context here?",
        "Check for absent info and spectral synapsis.",
        "Cross-reference with previous history."
    )

    val scope = rememberCoroutineScope()
    val savedSessions by database.browserSessionDao().getAllSessions().collectAsStateWithLifecycle(initialValue = emptyList())

    fun loadTab(tab: BrowserTab) {
        currentTabId = tab.id
        inputUrl = tab.url
        webViewRef?.loadUrl(tab.url)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Bar
                Row(
                    modifier = Modifier.fillMaxWidth().background(BgColor).padding(horizontal = 8.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab.id == currentTabId
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CardColor else Color(0xFF1E2A38))
                                .clickable { loadTab(tab) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.title.take(15) + if (tab.title.length > 15) "..." else "", color = if (isSelected) AccentColor else SubTextColor, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = SubTextColor, modifier = Modifier.size(14.dp).clickable {
                                    tabs = tabs.filter { it.id != tab.id }
                                    if (tabs.isEmpty()) {
                                        val newTab = BrowserTab(tabCounter++, "https://google.com", "New Tab")
                                        tabs = listOf(newTab)
                                        loadTab(newTab)
                                    } else if (isSelected) {
                                        loadTab(tabs.last())
                                    }
                                })
                            }
                        }
                    }
                    IconButton(onClick = {
                        val newTab = BrowserTab(tabCounter++, "https://google.com", "New Tab")
                        tabs = tabs + newTab
                        loadTab(newTab)
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Tab", tint = AccentColor)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSessionsDialog = true }) {
                        Icon(Icons.Filled.Bookmarks, contentDescription = "Sessions", tint = AccentColor)
                    }
                }

                // Browser Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgColor)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { webViewRef?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AccentColor)
                    }
                    IconButton(onClick = { webViewRef?.goForward() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = AccentColor)
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = AccentColor)
                    }

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            var finalUrl = inputUrl
                            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                finalUrl = "https://$finalUrl"
                            }
                            // Update current tab
                            tabs = tabs.map { if (it.id == currentTabId) it.copy(url = finalUrl) else it }
                            webViewRef?.loadUrl(finalUrl)
                        })
                    )

                    IconButton(onClick = { showGemini = true }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Gemini", tint = Color(0xFFAA00FF))
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Browser", tint = Color(0xFFFF5252))
                    }
                }

                if (progress in 0.01f..0.99f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Color(0xFF00E5FF),
                        trackColor = Color.Transparent
                    )
                }

                // WebView Container
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.mediaPlaybackRequiresUserGesture = false

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                                        val url = request?.url.toString()
                                        // Simple heuristic adblocker for youtube/ads
                                        if (url.contains("googleads") || url.contains("doubleclick") || url.contains("adsystem") || url.contains("ads.") || url.contains("-ad-")) {
                                            return android.webkit.WebResourceResponse("text/plain", "UTF-8", null)
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url != null) {
                                            inputUrl = url
                                            tabs = tabs.map { if (it.id == currentTabId) it.copy(url = url, title = view?.title ?: url) else it }
                                        }
                                        
                                        // Sci-Fi Dark Mode Injection
                                        val css = "html { filter: invert(100%) hue-rotate(180deg) !important; background: black !important; }"
                                        val js = "var style = document.createElement('style'); style.type = 'text/css'; style.appendChild(document.createTextNode('$css')); document.head.appendChild(style);"
                                        view?.evaluateJavascript(js, null)
                                        
                                        // Extract page content for Gemini
                                        view?.evaluateJavascript("(function() { return document.body.innerText; })();") { result ->
                                            pageContent = result?.replace("\"", "") ?: ""
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress / 100f
                                    }
                                }

                                loadUrl(inputUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Cybernetic HUD Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .background(Color(0x88000000), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("Evez-OS Net-Tunnel: ACTIVE", color = Color(0xFF00E5FF), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text("Anti-Tracker: DEPLOYED", color = Color(0xFF00FF00), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text("Bandwidth: ${if (progress < 1f) (progress * 100).toInt() else "STEADY"} Gbps", color = Color.White, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }

    if (showSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showSessionsDialog = false },
            containerColor = CardColor,
            titleContentColor = TextColor,
            textContentColor = TextColor,
            title = { Text("Tab Sessions Manager") },
            text = {
                Column {
                    Button(
                        onClick = {
                            val jsonArray = JSONArray()
                            tabs.forEach { 
                                val obj = JSONObject()
                                obj.put("url", it.url)
                                obj.put("title", it.title)
                                jsonArray.put(obj)
                            }
                            scope.launch {
                                database.browserSessionDao().insertSession(
                                    BrowserSession(
                                        id = "sess_${System.currentTimeMillis()}",
                                        name = "Session ${savedSessions.size + 1}",
                                        tabsJson = jsonArray.toString()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Current Session", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saved Sessions", style = MaterialTheme.typography.titleSmall, color = AccentColor)
                    savedSessions.forEach { session ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(session.name, color = TextColor, modifier = Modifier.weight(1f).clickable {
                                try {
                                    val jsonArray = JSONArray(session.tabsJson)
                                    val newTabs = mutableListOf<BrowserTab>()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        newTabs.add(BrowserTab(tabCounter++, obj.getString("url"), obj.getString("title")))
                                    }
                                    if (newTabs.isNotEmpty()) {
                                        tabs = newTabs
                                        loadTab(newTabs.first())
                                        showSessionsDialog = false
                                    }
                                } catch (e: Exception) {}
                            })
                            IconButton(onClick = { scope.launch { database.browserSessionDao().deleteSessionById(session.id) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessionsDialog = false }) { Text("Close", color = AccentColor) }
            }
        )
    }

    if (showGemini) {
        AlertDialog(
            onDismissRequest = { showGemini = false },
            containerColor = CardColor,
            titleContentColor = TextColor,
            textContentColor = TextColor,
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFAA00FF)); Spacer(modifier = Modifier.width(8.dp)); Text("Gemini Page Intelligence") } },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("AutoNav Agent Commands:", color = SubTextColor, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    autoPrompts.forEach { prompt ->
                        TextButton(
                            onClick = { customPrompt = prompt },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentColor)
                        ) {
                            Text(prompt, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                        }
                    }
                    
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        label = { Text("Custom auto-nav prompt") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoadingGemini) {
                        CircularProgressIndicator(color = AccentColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (geminiResponse.isNotEmpty()) {
                        Text(geminiResponse, color = TextColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.background(BgColor, RoundedCornerShape(8.dp)).padding(8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoadingGemini = true
                        scope.launch {
                            try {
                                val shortContext = pageContent.take(3000)
                                val query = if (customPrompt.isNotBlank()) customPrompt else "Summarize the key information."
                                val request = GenerateContentRequest(
                                    contents = listOf(Content(parts = listOf(Part("Context from history can always form entropy void and info absent spectral synapsis. Using webpage content: $shortContext\n\nPrompt: $query")), role = "user"))
                                )
                                val response = GeminiApi.service.generateContent(
                                    url = "v1beta/models/gemini-3.5-flash:generateContent",
                                    apiKey = BuildConfig.GEMINI_API_KEY,
                                    request = request
                                )
                                geminiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insight generated."
                            } catch (e: Exception) {
                                geminiResponse = "Error: Quantum Intelligence offline."
                            }
                            isLoadingGemini = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF003355))
                ) {
                    Text("Execute", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGemini = false }) { Text("Close", color = SubTextColor) }
            }
        )
    }
}

