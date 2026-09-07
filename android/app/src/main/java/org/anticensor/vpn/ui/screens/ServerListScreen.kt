package org.anticensor.vpn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anticensor.vpn.core.parser.ProxyConfig
import org.anticensor.vpn.core.subscription.Subscription
import org.anticensor.vpn.core.subscription.SubscriptionManager
import org.anticensor.vpn.ui.theme.*
import org.anticensor.vpn.ui.viewmodel.ServerViewModel

@Composable
fun ServerListScreen(
    viewModel: ServerViewModel,
    onServerSelected: () -> Unit
) {
    val context = LocalContext.current
    val servers by viewModel.servers.collectAsState()
    val selectedServerId by viewModel.selectedServerId.collectAsState()
    val latencies by viewModel.latencies.collectAsState()
    val isPingingAll by viewModel.isPingingAll.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val isUpdatingSubscriptions by viewModel.isUpdatingSubscriptions.collectAsState()
    val pingDiagnostics by viewModel.pingDiagnostics.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var qrShareServer by remember { mutableStateOf<ProxyConfig?>(null) }
    var importErrorDialogText by remember { mutableStateOf<Pair<String, String>?>(null) }
    var diagnosticDialogServer by remember { mutableStateOf<Pair<ProxyConfig, org.anticensor.vpn.core.network.PingResult?>?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = NeonCyan,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import Server Node or Subscription"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PROXY NODES",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceElevatedDark
                        ) {
                            Text(
                                text = "${servers.size}",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "ANTI-CENSORSHIP PROFILES & SUBSCRIPTIONS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Batch Ping Button
                OutlinedButton(
                    onClick = { viewModel.pingAllServers() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark,
                        contentColor = NeonCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Ping All",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isPingingAll) rotationAngle else 0f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPingingAll) "Pinging..." else "Ping All",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Smart Actions Row: Auto-Select, Sort, Clear Dead Nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto-Select Fastest Node Button
                Button(
                    onClick = {
                        val fastest = viewModel.autoSelectFastestServer()
                        if (fastest != null) onServerSelected()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceElevatedDark,
                        contentColor = NeonCyanBright
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Auto Select",
                        modifier = Modifier.size(15.dp),
                        tint = NeonCyanBright
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUTO SELECT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }

                // Sort by Latency Button
                OutlinedButton(
                    onClick = { viewModel.sortServersByLatency() },
                    modifier = Modifier.weight(0.9f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort by Ping",
                        modifier = Modifier.size(15.dp),
                        tint = NeonEmerald
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SORT PING",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Clear Dead Nodes Button
                OutlinedButton(
                    onClick = { viewModel.clearDeadNodes() },
                    modifier = Modifier.weight(0.9f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark,
                        contentColor = NeonRose
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Clean Unreachable",
                        modifier = Modifier.size(15.dp),
                        tint = NeonRose
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CLEAN DEAD",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Built-in Zero-Trust & Emergency Rescue Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.scanAndApplyCleanWarpIp() }
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = "WARP Clean IP", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Column {
                            Text("WARP Clean-IP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Scan Anycast Bypass", color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.restoreEmergencyPool() }
                        .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Emergency Mirrors", tint = NeonEmerald, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Emergency Pool", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Public Active Lifelines", color = NeonEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Server & Subscription List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subscriptions Card Section
                if (subscriptions.isNotEmpty()) {
                    item {
                        SubscriptionHeaderSection(
                            subscriptions = subscriptions,
                            isUpdating = isUpdatingSubscriptions,
                            onUpdateAll = { viewModel.updateAllSubscriptions(context) },
                            onUpdateSingle = { subId -> viewModel.updateSubscription(context, subId) },
                            onDelete = { subId -> viewModel.deleteSubscription(context, subId) }
                        )
                    }
                }

                items(servers, key = { it.id }) { server ->
                    val diagnostic = pingDiagnostics[server.id]
                    ServerCard(
                        server = server,
                        isSelected = server.id == selectedServerId,
                        latencyMs = latencies[server.id] ?: -1L,
                        diagnostic = diagnostic,
                        onSelect = {
                            viewModel.selectServer(server.id)
                            onServerSelected()
                        },
                        onPing = { viewModel.pingServer(server.id) },
                        onDelete = { viewModel.deleteServer(server.id) },
                        onShareQr = { qrShareServer = server },
                        onViewDiagnostic = {
                            diagnosticDialogServer = server to diagnostic
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showImportDialog) {
        ImportServerDialog(
            onDismiss = { showImportDialog = false },
            onImportUri = { rawUri ->
                val result = viewModel.addServerFromUri(rawUri)
                showImportDialog = false
                result.onFailure { error ->
                    val cause = when {
                        rawUri.isBlank() -> "Input link cannot be empty."
                        !rawUri.contains("://") && !rawUri.contains("[Interface]") -> "Missing valid protocol scheme (e.g. vless://, hysteria2://, tuic://, amneziawg://, ss://)."
                        rawUri.startsWith("vmess://") -> "VMess is an outdated protocol with distinctive packet signatures easily blocked by DPI. Please convert to VLESS Reality or Hysteria2."
                        error is IllegalArgumentException -> error.message ?: "Invalid configuration format."
                        else -> error.localizedMessage ?: "Unknown parsing failure."
                    }
                    importErrorDialogText = ("Link Import Failed" to cause)
                }
            },
            onImportSubscription = { url, name, ua ->
                viewModel.addSubscription(context, url, name, ua)
                showImportDialog = false
            }
        )
    }

    // Detailed Import Failure Alert Dialog
    importErrorDialogText?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { importErrorDialogText = null },
            containerColor = SurfaceDark,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = NeonRose,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = message,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevatedDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Supported Links: vless://, hy2://, tuic://, awg://, ss:// or WireGuard .conf snippets",
                            fontSize = 11.sp,
                            color = NeonRose,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { importErrorDialogText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "GOT IT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Node Connection Failure & Diagnostic Details Dialog
    diagnosticDialogServer?.let { (server, diag) ->
        AlertDialog(
            onDismissRequest = { diagnosticDialogServer = null },
            containerColor = SurfaceDark,
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Diagnostic",
                    tint = NeonRose,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Column {
                    Text(
                        text = "CONNECTION DIAGNOSTIC",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = server.name,
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevatedDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "TARGET ENDPOINT",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${server.server}:${server.port} (${server.protocol.displayName})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonRose.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ROOT CAUSE CATEGORY",
                                color = NeonRose,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = diag?.error ?: "Connection Timed Out / Dropped",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = diag?.detailedCause
                                    ?: "The remote host failed to complete TCP SYN-ACK handshake within the timeout threshold. Most likely caused by local ISP IP blacklisting, SNI filtering, or target server process is dead.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Remediation Suggestion
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevatedDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "SUGGESTED ACTIONS",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Switch to a different node or country\n• Check if your ISP is blocking this IP/port\n• Update subscription link to fetch fresh active nodes\n• Enable Anti-DPI Obfuscation or Reality transport",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sid = server.id
                        diagnosticDialogServer = null
                        viewModel.pingServer(sid)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "RE-TEST NODE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { diagnosticDialogServer = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(text = "CLOSE")
                }
            }
        )
    }

    // Node QR Code Share and Link Export Dialog
    qrShareServer?.let { server ->
        val shareLink = remember(server) { server.toShareLink() }
        val qrBitmap = remember(shareLink) {
            org.anticensor.vpn.core.util.QrCodeGenerator.createQrBitmap(shareLink, 480)
        }

        AlertDialog(
            onDismissRequest = { qrShareServer = null },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR Share", tint = NeonCyan)
                    Text(
                        text = "Share Node Configuration",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "${server.name} • ${server.protocol.displayName}",
                        fontSize = 13.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )

                    // QR Matrix Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Share Link Preview
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevatedDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "CONFIG URI",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = shareLink,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Proxy Node Link", shareLink)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Node Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        qrShareServer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "COPY LINK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { qrShareServer = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(text = "CLOSE")
                }
            }
        )
    }
}

@Composable
fun ServerCard(
    server: ProxyConfig,
    isSelected: Boolean,
    latencyMs: Long,
    diagnostic: org.anticensor.vpn.core.network.PingResult? = null,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit,
    onShareQr: () -> Unit = {},
    onViewDiagnostic: () -> Unit = {}
) {
    val (flagCode, countryName) = remember(server.server, server.name) {
        org.anticensor.vpn.core.network.IpGeoLookupService.detectCountry(server.server, server.name)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonCyan else BorderSubtle,
                shape = RoundedCornerShape(14.dp)
            ),
        color = if (isSelected) SurfaceElevatedDark else SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Selection Radio/Indicator
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (isSelected) NeonCyan else TextMuted,
                                CircleShape
                            )
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = org.anticensor.vpn.core.network.getCountryFlagEmoji(flagCode), fontSize = 16.sp)
                            Text(
                                text = server.name,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${server.server}:${server.port} • $countryName",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (server.subscriptionId != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonPurple.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "SUB",
                                color = NeonPurple,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    ProtocolBadge(protocol = server.protocol)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action row: Latency Pill & Ping / Share / Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latency Pill with clickable Error Diagnostic
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getPingColor(latencyMs).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, getPingColor(latencyMs).copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        if (latencyMs < 0 || diagnostic?.isSuccess == false) {
                            onViewDiagnostic()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(getPingColor(latencyMs))
                        )
                        Text(
                            text = if (latencyMs >= 0) "$latencyMs ms" else "Timeout (Tap Info)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = getPingColor(latencyMs)
                        )
                        if (latencyMs < 0 || diagnostic?.isSuccess == false) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error Info",
                                tint = NeonRose,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // QR Code & Link Share button
                    IconButton(
                        onClick = onShareQr,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Share QR / Link",
                            tint = NeonCyan.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onPing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Ping",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = NeonRose.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionHeaderSection(
    subscriptions: List<Subscription>,
    isUpdating: Boolean,
    onUpdateAll: () -> Unit,
    onUpdateSingle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUBSCRIPTIONS (${subscriptions.size})",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = onUpdateAll,
                enabled = !isUpdating,
                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Refresh All",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isUpdating) "UPDATING..." else "REFRESH ALL",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        subscriptions.forEach { sub ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = sub.url.substringBefore("?"),
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateSingle(sub.id) },
                                enabled = !isUpdating,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(sub.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = NeonRose.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Quota progress and details
                    sub.userInfo?.let { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        val ratio = (info.usedBytes.toFloat() / info.total.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = ratio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (ratio > 0.9f) NeonRose else NeonCyan,
                            trackColor = SurfaceElevatedDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${SubscriptionManager.formatBytes(info.usedBytes)} / ${SubscriptionManager.formatBytes(info.total)} (${info.usagePercentage.toInt()}%)",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (info.formattedExpiry.isNotBlank()) {
                                Text(
                                    text = "Exp: ${info.formattedExpiry}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportServerDialog(
    onDismiss: () -> Unit,
    onImportUri: (String) -> Unit,
    onImportSubscription: (String, String?, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Single Node URI, 1: Subscription URL
    var rawUriText by remember { mutableStateOf("") }

    // Subscription Tab State
    var subUrl by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }
    var selectedUaIndex by remember { mutableStateOf(0) }
    val uaOptions = listOf(
        "Karing" to SubscriptionManager.UA_KARING,
        "v2rayNG" to SubscriptionManager.UA_V2RAYNG,
        "ClashMeta" to SubscriptionManager.UA_CLASH_META
    )

    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Column {
                Text(
                    text = "IMPORT PROXY CONFIG",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Tab Row Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceElevatedDark,
                    contentColor = NeonCyan,
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "NODE LINK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTab == 0) NeonCyan else TextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "SUBSCRIPTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTab == 1) NeonCyan else TextMuted
                            )
                        }
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedTab == 0) {
                    // Single Node URI
                    Text(
                        text = "Paste vless://, hysteria2://, tuic://, naive+https://, amneziawg://, ss:// or WireGuard config:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = rawUriText,
                        onValueChange = { rawUriText = it },
                        placeholder = {
                            Text(
                                text = "vless://... or hysteria2://...",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceElevatedDark,
                            unfocusedContainerColor = SurfaceElevatedDark
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { rawUriText = it }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paste Clipboard",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = {
                                // Scan / Quick QR sample or auto-fill clipboard
                                val text = clipboardManager.getText()?.text ?: ""
                                if (text.isNotBlank()) {
                                    rawUriText = text
                                } else {
                                    rawUriText = "vless://auto-scanned-node@1.1.1.1:443?encryption=none&security=reality&sni=cloudflare.com&fp=chrome&pbk=sample#Scanned%20Node%20(SG)"
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonEmeraldBright)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan QR / Auto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Remote Subscription Link
                    Text(
                        text = "Enter subscription provider URL (Base64 or plain-text list):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        placeholder = {
                            Text(
                                text = "https://example.com/api/v1/client/subscribe?token=...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceElevatedDark,
                            unfocusedContainerColor = SurfaceElevatedDark
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        placeholder = {
                            Text(
                                text = "Alias (e.g., FastAir Premium)",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceElevatedDark,
                            unfocusedContainerColor = SurfaceElevatedDark
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Client User-Agent:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uaOptions.forEachIndexed { index, (label, _) ->
                            val isSelected = selectedUaIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedUaIndex = index },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { subUrl = it }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste URL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        onImportUri(rawUriText)
                    } else {
                        onImportSubscription(
                            subUrl,
                            subName.ifBlank { null },
                            uaOptions[selectedUaIndex].second
                        )
                    }
                },
                enabled = if (selectedTab == 0) rawUriText.isNotBlank() else subUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (selectedTab == 0) "IMPORT NODE" else "SUBSCRIBE & SYNC",
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text(text = "CANCEL")
            }
        }
    )
}
