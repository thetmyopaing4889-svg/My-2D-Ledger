package org.anticensor.vpn.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.anticensor.vpn.AegisApplication
import org.anticensor.vpn.R
import org.anticensor.vpn.core.process.BinaryManager
import org.anticensor.vpn.core.process.CoreBinary
import org.anticensor.vpn.core.process.ProcessHealthMonitor
import org.anticensor.vpn.core.process.ProcessManager
import org.anticensor.vpn.core.routing.DnsMode
import org.anticensor.vpn.core.routing.RoutingMode
import org.anticensor.vpn.core.routing.RoutingSettingsRepository
import org.anticensor.vpn.core.tunnel.HevTunnel
import org.anticensor.vpn.core.tunnel.HevTunnelConfig
import java.io.File

class AegisVpnService : VpnService() {

    companion object {
        private const val TAG = "AegisVpnService"
        const val ACTION_CONNECT = "org.anticensor.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "org.anticensor.vpn.ACTION_DISCONNECT"
        const val NOTIFICATION_ID = 1001

        const val TUN_IPV4_ADDR = "172.19.0.1"
        const val TUN_IPV4_PREFIX = 30
        const val TUN_IPV6_ADDR = "fdfe:dcba:9876::1"
        const val TUN_IPV6_PREFIX = 126
        const val TUN_MTU = 1500
        const val SOCKS5_PORT = 10808
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var binaryManager: BinaryManager
    private lateinit var processManager: ProcessManager
    private lateinit var healthMonitor: ProcessHealthMonitor

    override fun onCreate() {
        super.onCreate()
        binaryManager = BinaryManager(applicationContext)
        processManager = ProcessManager(applicationContext)
        healthMonitor = ProcessHealthMonitor(socks5Host = "127.0.0.1", socks5Port = SOCKS5_PORT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return
        Log.i(TAG, "Starting Aegis VPN Service in isolated :vpn_core process...")

        startForeground(NOTIFICATION_ID, buildForegroundNotification("Establishing VPN Tunnel..."))

        serviceScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Ensure active protocol core binary is ready (e.g. Xray)
                val core = CoreBinary.XRAY
                val executable = binaryManager.extractBinary(core)
                val daemonConfig = getOrCreateDefaultDaemonConfig(core)

                // Step 2: Spawn protocol daemon process
                val daemonStarted = processManager.startDaemon(core, executable, daemonConfig)
                if (!daemonStarted) {
                    Log.e(TAG, "Failed to start protocol daemon ${core.displayName}")
                    stopVpn()
                    return@launch
                }

                // Step 3: Wait for local SOCKS5 loopback listener on 127.0.0.1:10808
                val isPortReady = healthMonitor.waitForPortReady(maxWaitMs = 5000L)
                if (!isPortReady) {
                    Log.e(TAG, "Daemon did not open port $SOCKS5_PORT in time; halting tunnel setup")
                    stopVpn()
                    return@launch
                }
                healthMonitor.startMonitoring()

                // Step 4: Establish Android VpnService TUN interface with user routing policies
                val routingRepo = RoutingSettingsRepository.getInstance(applicationContext)
                val activeMtu = routingRepo.customMtu.coerceIn(1280, 1500)

                val builder = Builder()
                    .setSession("AegisVPN")
                    .setMtu(activeMtu)
                    .addAddress(TUN_IPV4_ADDR, TUN_IPV4_PREFIX)
                    .addRoute("0.0.0.0", 0) // Route all IPv4 traffic into the TUN interface

                // DNS Configuration based on DnsMode
                when (routingRepo.dnsMode) {
                    DnsMode.CLOUDFLARE_DOH -> {
                        builder.addDnsServer("1.1.1.1")
                        builder.addDnsServer("1.0.0.1")
                    }
                    DnsMode.GOOGLE_DOH -> {
                        builder.addDnsServer("8.8.8.8")
                        builder.addDnsServer("8.8.4.4")
                    }
                    DnsMode.ADGUARD_DOH -> {
                        builder.addDnsServer("94.140.14.14")
                        builder.addDnsServer("94.140.15.15")
                    }
                    DnsMode.SYSTEM_DNS -> {
                        builder.addDnsServer("1.1.1.1")
                        builder.addDnsServer("8.8.8.8")
                    }
                }

                // Add IPv6 support
                try {
                    builder.addAddress(TUN_IPV6_ADDR, TUN_IPV6_PREFIX)
                    builder.addRoute("::", 0)
                } catch (e: Exception) {
                    Log.w(TAG, "IPv6 route addition failed; falling back to IPv4-only", e)
                }

                // Kill Switch (System blocking mode)
                if (routingRepo.isKillSwitchEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setBlocking(true)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                // Per-App Split Tunneling vs Global
                if (routingRepo.routingMode == RoutingMode.SPLIT_TUNNEL_PER_APP) {
                    val allowedApps = routingRepo.getSelectedApps()
                    if (allowedApps.isNotEmpty()) {
                        for (pkg in allowedApps) {
                            try {
                                builder.addAllowedApplication(pkg)
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not add allowed app $pkg: ${e.message}")
                            }
                        }
                    } else {
                        // Fallback: exclude self
                        try {
                            builder.addDisallowedApplication(packageName)
                        } catch (_: Exception) {}
                    }
                } else {
                    // Global / Direct bypass: exclude self to prevent loop
                    try {
                        builder.addDisallowedApplication(packageName)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to disallow self-package", e)
                    }
                }

                tunInterface = builder.establish()

                val fd = tunInterface?.fd
                if (fd != null && fd > 0) {
                    Log.i(TAG, "TUN interface established with FD: $fd")

                    // Step 5: Write YAML config for hev-socks5-tunnel C engine
                    val configFile = File(filesDir, "hev_tunnel_config.yaml")
                    val tunnelConfig = HevTunnelConfig.default(port = SOCKS5_PORT, mtu = TUN_MTU)
                    tunnelConfig.writeToFile(configFile)

                    // Step 6: Initialize and start native C tun2socks coroutine engine
                    val initResult = HevTunnel.init(configFile.absolutePath, fd)
                    if (initResult == 0) {
                        HevTunnel.start()
                        isRunning = true
                        Log.i(TAG, "AegisVPN pipeline fully running: TUN -> hev-socks5-tunnel -> Xray -> Remote Proxy")
                        withContext(Dispatchers.Main) {
                            startForeground(NOTIFICATION_ID, buildForegroundNotification("Connected • ${core.displayName} Active"))
                        }
                    } else {
                        Log.e(TAG, "HevTunnel nativeInit failed with code: $initResult")
                        stopVpn()
                    }
                } else {
                    Log.e(TAG, "Failed to acquire valid TUN file descriptor")
                    stopVpn()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during VPN pipeline startup", e)
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping Aegis VPN Service and tearing down pipeline...")
        isRunning = false

        serviceScope.launch(Dispatchers.IO) {
            try {
                // 1. Stop hev-socks5-tunnel C loop
                HevTunnel.stop()

                // 2. Close Android TUN interface
                tunInterface?.close()
                tunInterface = null

                // 3. Stop loopback health monitor
                healthMonitor.stopMonitoring()

                // 4. Gracefully terminate protocol daemon (SIGTERM -> SIGKILL)
                processManager.stopDaemon()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing tunnel pipeline", e)
            } finally {
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun getOrCreateDefaultDaemonConfig(core: CoreBinary): File {
        val file = File(filesDir, "config_${core.executableName}.json")
        if (!file.exists()) {
            file.writeText(
                """
                {
                  "log": { "loglevel": "warning" },
                  "inbounds": [{
                    "port": $SOCKS5_PORT,
                    "listen": "127.0.0.1",
                    "protocol": "socks",
                    "settings": { "auth": "noauth", "udp": true }
                  }],
                  "outbounds": [{ "protocol": "freedom" }]
                }
                """.trimIndent()
            )
        }
        return file
    }

    private fun buildForegroundNotification(statusText: String): android.app.Notification {
        return NotificationCompat.Builder(this, AegisApplication.VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        healthMonitor.release()
        processManager.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}
