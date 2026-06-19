package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.Kernel
import com.example.DomainEvent

val KernelCodeFamily = FontFamily.Monospace

@Composable
fun KernelDashboardUI() {
    val events by Kernel.eventLog.collectAsStateWithLifecycle()
    var projections by remember { mutableStateOf(emptyList<Pair<String, Any?>>()) }

    LaunchedEffect(events) {
        val domains = Kernel.getRegisteredDomains()
        val latestProjections = domains.map { domain ->
            domain to Kernel.getState(domain)
        }
        projections = latestProjections
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "SYSTEM KERNEL DASHBOARD (NATIVE)",
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real-time Event Log Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "REAL-TIME EVENT LOG (APPEND-ONLY)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF334155))
                        .padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    reverseLayout = true
                ) {
                    val reversedEvents = events.reversed().take(50)
                    items(reversedEvents) { event ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color.Transparent, // Optional row border
                                )
                        ) {
                            Row {
                                Text("[${event.sequenceNumber}] ", color = Color(0xFF64748B), fontFamily = KernelCodeFamily, fontSize = 11.sp)
                                Text("${event.type} ", color = Color(0xFFF472B6), fontFamily = KernelCodeFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("→ ", color = Color(0xFFE2E8F0), fontFamily = KernelCodeFamily, fontSize = 11.sp)
                                Text(event.domainId, color = Color(0xFFA3E635), fontFamily = KernelCodeFamily, fontSize = 11.sp)
                            }
                            if (event.payload != null) {
                                Text(
                                    text = event.payload.toString(),
                                    color = Color(0xFF94A3B8),
                                    fontFamily = KernelCodeFamily,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    if (events.isEmpty()) {
                        item {
                            Text("No events logged yet.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Projections Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "ACTIVE PROJECTIONS (CQRS)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF334155))
                        .padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    items(projections) { proj ->
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(proj.first, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(" (Subscribed)", color = Color(0xFFFACC15), fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF020617))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = proj.second.toString(),
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = KernelCodeFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    if (projections.isEmpty()) {
                        item {
                            Text("No active projections.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
