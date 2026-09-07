package org.anticensor.vpn.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anticensor.vpn.core.parser.ProxyProtocol
import org.anticensor.vpn.ui.theme.*
import org.anticensor.vpn.ui.viewmodel.ConnectionStatus
import org.anticensor.vpn.ui.viewmodel.ServerViewModel

@Composable
fun MainScreen(
    viewModel: ServerViewModel,
    onNavigateToServerList: () -> Unit,
    onConnectRequested: () -> Unit
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val latencies by viewModel.latencies.collectAsState()

    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isConnecting = connectionStatus is ConnectionStatus.Connecting

    val activeLatency = selectedServer?.id?.let { latencies[it] } ?: -1L
    val pingDiagnostics by viewModel.pingDiagnostics.collectAsState()
    val ipGeoInfo by viewModel.ipGeoInfo.collectAsState()
    val isCheckingGeoIp by viewModel.isCheckingGeoIp.collectAsState()
    var showActiveDiagnosticDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterCenter
    ) {
        // App Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AEGIS VPN",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "ANTI-CENSORSHIP TUNNEL",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isConnected) NeonEmerald.copy(alpha = 0.15f) else SurfaceElevatedDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isConnected) NeonEmerald.copy(alpha = 0.4f) else BorderSubtle
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isConnected -> NeonEmerald
                                    isConnecting -> NeonAmber
                                    else -> TextMuted
                                }
                            )
                    )
                    Text(
                        text = when {
                            isConnected -> "SECURE"
                            isConnecting -> "CONNECTING"
                            else -> "STANDBY"
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isConnected -> NeonEmeraldBright
                            isConnecting -> NeonAmber
                            else -> TextSecondary
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Big Pulsing Connect Button
        PulsingConnectButton(
            connectionStatus = connectionStatus,
            onClick = {
                if (isConnected || isConnecting) {
                    viewModel.disconnect(context)
                } else {
                    onConnectRequested()
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Active Server Profile Card
        selectedServer?.let { server ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToServerList() }
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                color = SurfaceDark
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE ENDPOINT",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Change",
                                fontSize = 12.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Switch Server",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (flag, country) = org.anticensor.vpn.core.network.IpGeoLookupService.detectCountry(server.server, server.name)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = org.anticensor.vpn.core.network.getCountryFlagEmoji(flag), fontSize = 16.sp)
                                Text(
                                    text = server.name,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${server.server}:${server.port} • $country",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Protocol Badge
                        ProtocolBadge(protocol = server.protocol)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-Time IP Geolocation Status Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevatedDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) NeonEmeraldBright.copy(alpha = 0.3f) else BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = ipGeoInfo?.flagEmoji ?: "🌐",
                                    fontSize = 18.sp
                                )
                                Column {
                                    Text(
                                        text = if (isConnected) "VERIFIED EXIT IP" else "CURRENT PUBLIC IP",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isConnected) NeonEmeraldBright else TextMuted
                                    )
                                    Text(
                                        text = ipGeoInfo?.let { "${it.ip} (${it.countryName})" } ?: "Querying GeoIP...",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.fetchGeoIp(isVpnTunnel = isConnected) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh GeoIP",
                                    tint = if (isCheckingGeoIp) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ping Latency Row with TCP vs HTTP-204 check
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevatedDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Ping",
                                tint = getPingColor(activeLatency),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isConnected) "HTTP-204 Web Latency" else "TCP Handshake RTT",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (activeLatency >= 0) "$activeLatency ms" else "Timeout",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = getPingColor(activeLatency)
                            )
                            if (activeLatency < 0) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View Error Diagnosis",
                                    tint = NeonRose,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showActiveDiagnosticDialog = true }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Test Latency",
                                tint = NeonCyan,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        viewModel.pingServer(server.id, useHttp204 = isConnected)
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Active Server Connection Diagnostic Modal Dialog
        if (showActiveDiagnosticDialog && selectedServer != null) {
            val s = selectedServer!!
            val diag = pingDiagnostics[s.id]
            AlertDialog(
                onDismissRequest = { showActiveDiagnosticDialog = false },
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
                            text = "DETAILED ERROR DIAGNOSTIC",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = s.name,
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
                                    text = "${s.server}:${s.port} (${s.protocol.displayName})",
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
                                    text = "FAILURE CATEGORY",
                                    color = NeonRose,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = diag?.error ?: "Connection Timed Out / RST",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = diag?.detailedCause
                                        ?: "The server failed to complete the handshake. This typically indicates that local ISP firewalls are intercepting packets, SNI filtering is triggered, or the IP address is blocked.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showActiveDiagnosticDialog = false
                            viewModel.pingServer(s.id, useHttp204 = isConnected)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "RETRY TEST", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showActiveDiagnosticDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text(text = "CLOSE")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Realtime Throughput & Traffic Meters
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            color = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NETWORK THROUGHPUT",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "UPTIME: ${viewModel.formatUptime(metrics.uptimeSeconds)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isConnected) NeonCyan else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Download & Upload Speed Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpeedMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "DOWNLOAD",
                        rate = "${viewModel.formatBytes(metrics.downloadSpeedBps)}/s",
                        total = viewModel.formatBytes(metrics.totalDownloadBytes),
                        icon = Icons.Default.ArrowDownward,
                        accentColor = NeonCyan
                    )

                    SpeedMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "UPLOAD",
                        rate = "${viewModel.formatBytes(metrics.uploadSpeedBps)}/s",
                        total = viewModel.formatBytes(metrics.totalUploadBytes),
                        icon = Icons.Default.ArrowUpward,
                        accentColor = NeonEmerald
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Realtime Throughput Waveform Canvas
                ThroughputGraph(
                    history = metrics.speedHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(SurfaceElevatedDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Anti-Censorship Security Indicators
        Text(
            text = "ACTIVE DEFENSES",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecurityDefensePill(
                modifier = Modifier.weight(1f),
                title = "Anti-DPI",
                subtitle = "Active Obfuscation",
                icon = Icons.Default.Shield,
                color = NeonCyan
            )
            SecurityDefensePill(
                modifier = Modifier.weight(1f),
                title = "Process",
                subtitle = ":vpn_core Isolated",
                icon = Icons.Default.Memory,
                color = NeonViolet
            )
            SecurityDefensePill(
                modifier = Modifier.weight(1f),
                title = "DNS Guard",
                subtitle = "1.1.1.1 Encrypted",
                icon = Icons.Default.VpnLock,
                color = NeonEmerald
            )
        }
    }
}

@Composable
fun PulsingConnectButton(
    connectionStatus: ConnectionStatus,
    onClick: () -> Unit
) {
    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isConnecting = connectionStatus is ConnectionStatus.Connecting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isConnected || isConnecting) 0.05f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> NeonEmerald
            isConnecting -> NeonAmber
            else -> SurfaceElevatedDark
        },
        animationSpec = tween(400),
        label = "buttonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // Outer Glowing Pulse Ring
        if (isConnected || isConnecting) {
            Box(
                modifier = Modifier
                    .size(190.dp * pulseScale)
                    .clip(CircleShape)
                    .background(
                        (if (isConnected) NeonEmerald else NeonAmber).copy(alpha = pulseAlpha)
                    )
            )
        }

        // Middle Glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isConnected -> NeonEmerald.copy(alpha = 0.15f)
                        isConnecting -> NeonAmber.copy(alpha = 0.15f)
                        else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                    }
                )
        )

        // Core Action Button
        Surface(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .clickable { onClick() }
                .shadow(
                    elevation = if (isConnected) 16.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = if (isConnected) NeonEmerald else Color.Transparent
                )
                .border(
                    width = 2.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.8f),
                            buttonColor.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.PowerSettingsNew
                        isConnecting -> Icons.Default.Sync
                        else -> Icons.Default.PowerSettingsNew
                    },
                    contentDescription = "Connect Button",
                    tint = buttonColor,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        isConnected -> "DISCONNECT"
                        isConnecting -> "CONNECTING"
                        else -> "CONNECT"
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = buttonColor
                )
            }
        }
    }
}

@Composable
fun SpeedMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    rate: String,
    total: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = rate,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Total: $total",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

@Composable
fun ThroughputGraph(
    history: List<Pair<Long, Long>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val stepX = width / (history.size.coerceAtLeast(2) - 1).toFloat()

        val maxVal = history.maxOfOrNull { maxOf(it.first, it.second) }?.coerceAtLeast(1024 * 1024L) ?: (1024 * 1024L)

        // Draw Download Speed Path (NeonCyan)
        val downPath = Path()
        history.forEachIndexed { i, pair ->
            val x = i * stepX
            val y = height - ((pair.first.toFloat() / maxVal.toFloat()) * height * 0.85f)
            if (i == 0) downPath.moveTo(x, y) else downPath.lineTo(x, y)
        }
        drawPath(
            path = downPath,
            color = NeonCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Upload Speed Path (NeonEmerald)
        val upPath = Path()
        history.forEachIndexed { i, pair ->
            val x = i * stepX
            val y = height - ((pair.second.toFloat() / maxVal.toFloat()) * height * 0.85f)
            if (i == 0) upPath.moveTo(x, y) else upPath.lineTo(x, y)
        }
        drawPath(
            path = upPath,
            color = NeonEmerald,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ProtocolBadge(protocol: ProxyProtocol) {
    val (label, bg, border, text) = when (protocol) {
        ProxyProtocol.VLESS_REALITY -> Quadruple("VLESS", NeonCyan.copy(alpha = 0.15f), NeonCyan.copy(alpha = 0.4f), NeonCyanBright)
        ProxyProtocol.HYSTERIA2 -> Quadruple("HY2", NeonEmerald.copy(alpha = 0.15f), NeonEmerald.copy(alpha = 0.4f), NeonEmeraldBright)
        ProxyProtocol.NAIVE_PROXY -> Quadruple("NAIVE", NeonViolet.copy(alpha = 0.15f), NeonViolet.copy(alpha = 0.4f), NeonViolet)
        ProxyProtocol.TUIC -> Quadruple("TUIC", NeonAmber.copy(alpha = 0.15f), NeonAmber.copy(alpha = 0.4f), NeonAmber)
        ProxyProtocol.AMNEZIA_WG -> Quadruple("AWG", NeonRose.copy(alpha = 0.15f), NeonRose.copy(alpha = 0.4f), NeonRose)
        ProxyProtocol.SHADOWSOCKS -> Quadruple("SS-2022", NeonPurple.copy(alpha = 0.15f), NeonPurple.copy(alpha = 0.4f), NeonPurple)
        ProxyProtocol.WARP -> Quadruple("WARP+", NeonCyan.copy(alpha = 0.25f), NeonCyan, NeonCyanBright)
        ProxyProtocol.SSH_TUNNEL -> Quadruple("SSH-WS", NeonAmber.copy(alpha = 0.2f), NeonAmber.copy(alpha = 0.5f), NeonAmber)
        ProxyProtocol.SS_CLOAK -> Quadruple("SS+CLOAK", NeonPurple.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.5f), NeonPurple)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SecurityDefensePill(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun getPingColor(latencyMs: Long): Color {
    return when {
        latencyMs < 0 -> PingDead
        latencyMs < 150 -> PingFast
        latencyMs < 350 -> PingMedium
        else -> PingSlow
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
