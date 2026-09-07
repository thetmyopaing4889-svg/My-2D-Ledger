package org.anticensor.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.anticensor.vpn.ui.theme.*
import org.anticensor.vpn.ui.viewmodel.LogEntry
import org.anticensor.vpn.ui.viewmodel.LogLevel
import org.anticensor.vpn.ui.viewmodel.ServerViewModel

@Composable
fun LogViewerScreen(
    viewModel: ServerViewModel
) {
    val logs by viewModel.logs.collectAsState()
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    var autoScroll by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.level == selectedFilter }
    }

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(filteredLogs.size) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Terminal Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(NeonEmerald, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "DAEMON LOG STREAM",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "LIVE STDOUT/STDERR FROM :vpn_core PROCESS",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quick Actions: Copy and Clear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val allLogText = logs.joinToString("\n") {
                            "[${it.formattedTime}] [${it.level}] [${it.tag}] ${it.message}"
                        }
                        clipboardManager.setText(AnnotatedString(allLogText))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy All Logs",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = NeonRose.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filters and Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterPill(
                    label = "ALL",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    activeColor = NeonCyan
                )
                FilterPill(
                    label = "INFO",
                    isSelected = selectedFilter == LogLevel.INFO,
                    onClick = { selectedFilter = LogLevel.INFO },
                    activeColor = NeonCyanBright
                )
                FilterPill(
                    label = "WARN",
                    isSelected = selectedFilter == LogLevel.WARN,
                    onClick = { selectedFilter = LogLevel.WARN },
                    activeColor = NeonAmber
                )
                FilterPill(
                    label = "ERR",
                    isSelected = selectedFilter == LogLevel.ERROR,
                    onClick = { selectedFilter = LogLevel.ERROR },
                    activeColor = NeonRose
                )
            }

            // Auto-scroll toggle
            TextButton(
                onClick = { autoScroll = !autoScroll },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.Pause,
                    contentDescription = "Toggle Auto-scroll",
                    tint = if (autoScroll) NeonCyan else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (autoScroll) "Auto-scroll ON" else "Paused",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (autoScroll) NeonCyan else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Monospace Terminal Output Box
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            color = Color(0xFF080C14),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No logs for selected filter",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { entry ->
                        LogLineItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.2f) else SurfaceElevatedDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor.copy(alpha = 0.6f) else BorderSubtle
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) activeColor else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LogLineItem(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.INFO -> NeonCyan
        LogLevel.WARN -> NeonAmber
        LogLevel.ERROR -> NeonRose
        LogLevel.DEBUG -> NeonViolet
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = entry.formattedTime,
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Level Tag
        Text(
            text = "[${entry.level.name}]",
            color = levelColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Tag
        Text(
            text = "<${entry.tag}>",
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Message
        Text(
            text = entry.message,
            color = if (entry.level == LogLevel.ERROR) NeonRose else Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
