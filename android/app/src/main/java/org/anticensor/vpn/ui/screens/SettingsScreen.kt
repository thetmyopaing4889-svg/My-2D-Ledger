package org.anticensor.vpn.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.anticensor.vpn.core.routing.AppRoutingInfo
import org.anticensor.vpn.core.routing.DnsMode
import org.anticensor.vpn.core.routing.RoutingMode
import org.anticensor.vpn.core.routing.RoutingSettingsRepository
import org.anticensor.vpn.ui.theme.*
import org.anticensor.vpn.ui.viewmodel.ServerViewModel

@Composable
fun SettingsScreen(
    viewModel: ServerViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val routingRepo = remember { RoutingSettingsRepository.getInstance(context) }

    var routingMode by remember { mutableStateOf(routingRepo.routingMode) }
    var dnsMode by remember { mutableStateOf(routingRepo.dnsMode) }
    var isKillSwitch by remember { mutableStateOf(routingRepo.isKillSwitchEnabled) }
    var isSmartFailover by remember { mutableStateOf(routingRepo.isSmartFailoverEnabled) }
    var isPacketFragmentation by remember { mutableStateOf(routingRepo.isPacketFragmentationEnabled) }
    var customMtu by remember { mutableStateOf(routingRepo.customMtu) }
    var isAutoUpdateSubs by remember { mutableStateOf(routingRepo.isAutoUpdateSubscriptionsEnabled) }
    var subsIntervalHours by remember { mutableStateOf(routingRepo.subscriptionIntervalHours) }

    var installedApps by remember { mutableStateOf<List<AppRoutingInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var showAppSelectorDialog by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        installedApps = routingRepo.getInstalledApps(context)
        isLoadingApps = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Routing Rules",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "ROUTING & PRIVACY",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "TRAFFIC SPLITTING, DNS SECURITY & KILL SWITCH",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section 1: Routing Modes
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TUNNEL ROUTING POLICY",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    RoutingMode.values().forEach { mode ->
                        val isSelected = routingMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    routingMode = mode
                                    routingRepo.routingMode = mode
                                    Toast.makeText(context, "Routing set to: ${mode.displayName}", Toast.LENGTH_SHORT).show()
                                }
                                .background(if (isSelected) SurfaceElevatedDark else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = mode.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    routingMode = mode
                                    routingRepo.routingMode = mode
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeonCyan,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (routingMode == RoutingMode.SPLIT_TUNNEL_PER_APP) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val selectedCount = installedApps.count { it.isProxyEnabled }
                        OutlinedButton(
                            onClick = { showAppSelectorDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceElevatedDark,
                                contentColor = NeonCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = "Configure Apps",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SELECT APPS ($selectedCount SELECTED)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Encrypted DNS Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ENCRYPTED DNS (DoH)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ANTI-POISONING",
                                color = NeonEmeraldBright,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DnsMode.values().forEach { dns ->
                        val isSelected = dnsMode == dns
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    dnsMode = dns
                                    routingRepo.dnsMode = dns
                                }
                                .background(if (isSelected) SurfaceElevatedDark else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dns.displayName,
                                    color = if (isSelected) NeonEmeraldBright else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = dns.upstreamDns,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    dnsMode = dns
                                    routingRepo.dnsMode = dns
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeonEmerald,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Kill Switch & Auto Failover Toggles
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SECURITY & CONTINUITY DEFENSES",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Kill Switch Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kill Switch (Block Leakage)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Block all non-VPN internet traffic if tunnel drops",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isKillSwitch,
                            onCheckedChange = {
                                isKillSwitch = it
                                routingRepo.isKillSwitchEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonRose,
                                uncheckedTrackColor = SurfaceElevatedDark
                            )
                        )
                    }

                    Divider(
                        color = BorderSubtle,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Smart Auto-Failover Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Node Failover",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically switch to lowest latency backup node on timeout",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isSmartFailover,
                            onCheckedChange = {
                                isSmartFailover = it
                                routingRepo.isSmartFailoverEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevatedDark
                            )
                        )
                    }
                }
            }
        }

        // Section 4: Advanced Anti-DPI Packet Fragmentation & MTU Tuning
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ADVANCED ANTI-DPI PACKET SHUFFLING & MTU",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Packet Fragmentation Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TCP Segment & TLS SNI Fragmentation",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Splits TLS ClientHello across small TCP segments to bypass strict DPI filters",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isPacketFragmentation,
                            onCheckedChange = {
                                isPacketFragmentation = it
                                routingRepo.isPacketFragmentationEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonEmeraldBright,
                                uncheckedTrackColor = SurfaceElevatedDark
                            )
                        )
                    }

                    Divider(
                        color = BorderSubtle,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Custom MTU selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tunnel MTU Buffer Size",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recommended: 1400 bytes (lower avoids ISP packet drop)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1280, 1360, 1400, 1500).forEach { mtuVal ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (customMtu == mtuVal) NeonCyan else SurfaceElevatedDark,
                                    modifier = Modifier.clickable {
                                        customMtu = mtuVal
                                        routingRepo.customMtu = mtuVal
                                    }
                                ) {
                                    Text(
                                        text = "$mtuVal",
                                        color = if (customMtu == mtuVal) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Automated Subscription Scheduler
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SUBSCRIPTION AUTO-SYNC SCHEDULER",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Periodic Background Sync",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically fetches new active nodes from remote subscription URLs",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAutoUpdateSubs,
                            onCheckedChange = {
                                isAutoUpdateSubs = it
                                routingRepo.isAutoUpdateSubscriptionsEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonPurple,
                                uncheckedTrackColor = SurfaceElevatedDark
                            )
                        )
                    }

                    if (isAutoUpdateSubs) {
                        Divider(
                            color = BorderSubtle,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sync Interval",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(6, 12, 24).forEach { hours ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (subsIntervalHours == hours) NeonPurple else SurfaceElevatedDark,
                                        modifier = Modifier.clickable {
                                            subsIntervalHours = hours
                                            routingRepo.subscriptionIntervalHours = hours
                                        }
                                    ) {
                                        Text(
                                            text = "${hours}h",
                                            color = if (subsIntervalHours == hours) Color.White else TextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // App Selector Modal Dialog for Split Tunneling
    if (showAppSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showAppSelectorDialog = false },
            containerColor = SurfaceDark,
            title = {
                Column {
                    Text(
                        text = "SELECT APPS TO PROXY",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Unselected applications will connect directly",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search apps...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            text = {
                val filtered = installedApps.filter {
                    it.appName.contains(appSearchQuery, ignoreCase = true) ||
                    it.packageName.contains(appSearchQuery, ignoreCase = true)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val isNowEnabled = routingRepo.toggleAppSelection(app.packageName)
                                    installedApps = installedApps.map {
                                        if (it.packageName == app.packageName) it.copy(isProxyEnabled = isNowEnabled) else it
                                    }
                                }
                                .background(if (app.isProxyEnabled) SurfaceElevatedDark else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    color = if (app.isProxyEnabled) NeonCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = app.packageName,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Checkbox(
                                checked = app.isProxyEnabled,
                                onCheckedChange = {
                                    val isNowEnabled = routingRepo.toggleAppSelection(app.packageName)
                                    installedApps = installedApps.map {
                                        if (it.packageName == app.packageName) it.copy(isProxyEnabled = isNowEnabled) else it
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonCyan,
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppSelectorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "DONE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }
            }
        )
    }
}
