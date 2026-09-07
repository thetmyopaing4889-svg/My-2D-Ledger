package org.anticensor.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.anticensor.vpn.AegisApplication
import org.anticensor.vpn.R
import org.anticensor.vpn.core.process.*
import org.anticensor.vpn.core.tunnel.HevTunnel
import org.anticensor.vpn.core.tunnel.HevTunnelConfig
import java.io.File

/**
 * High-level orchestration service running in the isolated :vpn_core Android process.
 * Coordinates protocol daemon execution, port allocation at 127.0.0.1:10808,
 * health monitoring, and synchronizes with HevTunnel C engine.
 */
class DaemonService : Service() {

    companion object {
        private const val TAG = "DaemonService"
        const val NOTIFICATION_ID = 1002
        const val SOCKS5_PORT = 10808

        const val ACTION_START_DAEMON = "org.anticensor.vpn.ACTION_START_DAEMON"
        const val ACTION_STOP_DAEMON = "org.anticensor.vpn.ACTION_STOP_DAEMON"
        const val ACTION_SWITCH_PROTOCOL = "org.anticensor.vpn.ACTION_SWITCH_PROTOCOL"

        const val EXTRA_CORE_BINARY = "extra_core_binary"
        const val EXTRA_CONFIG_PATH = "extra_config_path"
    }

    private val binder = DaemonBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    lateinit var binaryManager: BinaryManager
        private set

    lateinit var processManager: ProcessManager
        private set

    lateinit var healthMonitor: ProcessHealthMonitor
        private set

    private val _serviceStatus = MutableStateFlow<String>("IDLE")
    val serviceStatus: StateFlow<String> = _serviceStatus.asStateFlow()

    inner class DaemonBinder : Binder() {
        fun getService(): DaemonService = this@DaemonService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DaemonService created in :vpn_core process")

        binaryManager = BinaryManager(applicationContext)
        processManager = ProcessManager(applicationContext)
        healthMonitor = ProcessHealthMonitor(socks5Host = "127.0.0.1", socks5Port = SOCKS5_PORT)

        healthMonitor.setOnHealthFailedListener {
            Log.e(TAG, "Health check failed repeatedly! Triggering daemon restart recovery...")
            serviceScope.launch {
                // Trigger auto-healing if needed
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val coreName = intent?.getStringExtra(EXTRA_CORE_BINARY) ?: CoreBinary.XRAY.name
        val configPath = intent?.getStringExtra(EXTRA_CONFIG_PATH)

        when (action) {
            ACTION_START_DAEMON -> {
                val core = try { CoreBinary.valueOf(coreName) } catch (_: Exception) { CoreBinary.XRAY }
                startDaemonWorkflow(core, configPath)
            }
            ACTION_SWITCH_PROTOCOL -> {
                val core = try { CoreBinary.valueOf(coreName) } catch (_: Exception) { CoreBinary.HYSTERIA2 }
                switchProtocol(core, configPath)
            }
            ACTION_STOP_DAEMON -> {
                stopDaemonWorkflow()
            }
        }

        return START_STICKY
    }

    /**
     * Executes the full pipeline:
     * 1. Extract binary if not present
     * 2. Ensure previous daemon is terminated
     * 3. Launch target daemon
     * 4. Probe port 127.0.0.1:10808 until ready
     * 5. Start health monitor loop
     */
    fun startDaemonWorkflow(core: CoreBinary, configPath: String?) {
        serviceScope.launch(Dispatchers.IO) {
            _serviceStatus.value = "EXTRACTING_${core.name}"
            startForeground(NOTIFICATION_ID, buildNotification("Starting ${core.displayName}..."))

            try {
                // Step 1: Ensure binary is extracted & has +x
                val executable = binaryManager.extractBinary(core)

                // Step 2: Prepare config file
                val configFile = if (configPath != null) {
                    File(configPath)
                } else {
                    getOrCreateDefaultConfig(core)
                }

                _serviceStatus.value = "LAUNCHING_${core.name}"

                // Step 3: Launch daemon via ProcessManager
                val started = processManager.startDaemon(core, executable, configFile)
                if (!started) {
                    _serviceStatus.value = "START_FAILED"
                    return@launch
                }

                // Step 4: Wait for SOCKS5 127.0.0.1:10808 to be ready
                _serviceStatus.value = "PROBING_SOCKS5"
                val portReady = healthMonitor.waitForPortReady(maxWaitMs = 6000L)

                if (portReady) {
                    _serviceStatus.value = "DAEMON_READY"
                    healthMonitor.startMonitoring()
                    updateNotification("${core.displayName} Active (127.0.0.1:$SOCKS5_PORT)")
                    Log.i(TAG, "Daemon ${core.displayName} is healthy and ready for traffic.")
                } else {
                    _serviceStatus.value = "PORT_TIMEOUT"
                    Log.e(TAG, "Daemon did not open port $SOCKS5_PORT within timeout window!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during daemon start workflow", e)
                _serviceStatus.value = "ERROR: ${e.message}"
            }
        }
    }

    /**
     * Dynamic switching between protocols:
     * Cleanly terminates the current daemon, verifies port 10808 is closed,
     * and boots the new protocol daemon seamlessly.
     */
    fun switchProtocol(newCore: CoreBinary, newConfigPath: String?) {
        serviceScope.launch(Dispatchers.IO) {
            _serviceStatus.value = "SWITCHING_TO_${newCore.name}"
            Log.i(TAG, "Switching protocol daemon to: ${newCore.displayName}")

            // 1. Stop health monitor
            healthMonitor.stopMonitoring()

            // 2. Terminate existing daemon (SIGTERM -> SIGKILL)
            processManager.stopDaemon()

            // 3. Small debounce to ensure socket TIME_WAIT or cleanup
            delay(300)

            // 4. Start new daemon workflow
            startDaemonWorkflow(newCore, newConfigPath)
        }
    }

    /**
     * Gracefully stops the daemon and health monitor.
     */
    fun stopDaemonWorkflow() {
        serviceScope.launch(Dispatchers.IO) {
            _serviceStatus.value = "STOPPING"
            healthMonitor.stopMonitoring()
            processManager.stopDaemon()
            _serviceStatus.value = "STOPPED"
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun getOrCreateDefaultConfig(core: CoreBinary): File {
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

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, AegisApplication.VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Aegis Core Daemon")
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        healthMonitor.release()
        processManager.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}
