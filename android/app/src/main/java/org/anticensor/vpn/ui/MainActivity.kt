package org.anticensor.vpn.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.anticensor.vpn.ui.screens.LogViewerScreen
import org.anticensor.vpn.ui.screens.MainScreen
import org.anticensor.vpn.ui.screens.ServerListScreen
import org.anticensor.vpn.ui.screens.SettingsScreen
import org.anticensor.vpn.ui.theme.*
import org.anticensor.vpn.ui.viewmodel.LogLevel
import org.anticensor.vpn.ui.viewmodel.ServerViewModel

enum class NavigationTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Shield", Icons.Default.Shield),
    SERVERS("Nodes", Icons.Default.Dns),
    ROUTING("Rules", Icons.Default.Tune),
    TERMINAL("Logs", Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ServerViewModel by viewModels()

    // Launcher for system VpnService.prepare() consent dialog
    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.appendLog(LogLevel.INFO, "VpnService", "System VPN permission granted by user")
            viewModel.connect(this)
        } else {
            viewModel.appendLog(LogLevel.WARN, "VpnService", "VPN permission request declined")
            Toast.makeText(this, "VPN permission required to create secure tunnel", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for Android 13+ Notification permission
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            viewModel.appendLog(LogLevel.WARN, "Notification", "Notification permission denied; status updates may be hidden")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13 (Tiramisu) or above
        checkNotificationPermission()

        // Load saved subscriptions and cached nodes from persistent storage
        viewModel.loadSavedSubscriptions(this)

        // Handle possible deep link URI import (vless://, hysteria2://, tuic://, etc.)
        handleIntentUri(intent)

        setContent {
            AegisVPNTheme {
                var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundDark,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) NeonCyan else TextMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) NeonCyan else TextMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = SurfaceElevatedDark
                                    )
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = BackgroundDark
                    ) {
                        when (currentTab) {
                            NavigationTab.DASHBOARD -> {
                                MainScreen(
                                    viewModel = viewModel,
                                    onNavigateToServerList = { currentTab = NavigationTab.SERVERS },
                                    onConnectRequested = { requestVpnConnection() }
                                )
                            }
                            NavigationTab.SERVERS -> {
                                ServerListScreen(
                                    viewModel = viewModel,
                                    onServerSelected = { currentTab = NavigationTab.DASHBOARD }
                                )
                            }
                            NavigationTab.ROUTING -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                            NavigationTab.TERMINAL -> {
                                LogViewerScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentUri(intent)
    }

    /**
     * Checks if VpnService preparation dialog is required.
     * If already prepared, initiates VPN connection immediately.
     */
    private fun requestVpnConnection() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            viewModel.appendLog(LogLevel.INFO, "VpnService", "Requesting system VPN consent dialog...")
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            // Already prepared by the system
            viewModel.connect(this)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun handleIntentUri(intent: Intent?) {
        val uri = intent?.dataString ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!uri.isNullOrBlank()) {
            val result = viewModel.addServerFromUri(uri)
            if (result.isSuccess) {
                Toast.makeText(this, "Imported: ${result.getOrNull()?.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to import configuration link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
