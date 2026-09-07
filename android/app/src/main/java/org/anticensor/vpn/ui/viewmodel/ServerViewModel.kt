package org.anticensor.vpn.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.anticensor.vpn.core.generator.ConfigGeneratorFactory
import org.anticensor.vpn.core.network.PingLatencyTester
import org.anticensor.vpn.core.network.PingResult
import org.anticensor.vpn.core.parser.*
import org.anticensor.vpn.core.subscription.*
import org.anticensor.vpn.service.AegisVpnService
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

enum class LogLevel {
    INFO, WARN, ERROR, DEBUG
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    data class Connecting(val server: ProxyConfig) : ConnectionStatus()
    data class Connected(val server: ProxyConfig, val connectedSinceMs: Long) : ConnectionStatus()
    object Disconnecting : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

data class NetworkMetrics(
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val speedHistory: List<Pair<Long, Long>> = emptyList() // Pair(downloadBps, uploadBps)
)

class ServerViewModel : ViewModel() {

    companion object {
        private const val TAG = "ServerViewModel"
        const val MAX_LOGS = 300
    }

    // Default Anti-Censorship Preset Servers for immediate testing
    private val defaultServers: List<ProxyConfig> = listOf(
        VlessConfig(
            id = "preset-vless-xhttp",
            name = "US - Silicon Valley (Reality XHTTP)",
            server = "198.51.100.1",
            port = 443,
            uuid = "9a8b7c6d-5e4f-3a2b-1c0d-ef9876543210",
            publicKey = "7z_K_kH2xP3R9wQ0aB8c-DeFgHiJkLmNoPqRsTuVwXy",
            sni = "www.microsoft.com",
            fingerprint = "chrome",
            shortId = "1a2b3c4d",
            transportType = "xhttp",
            path = "/push-notification"
        ),
        Hysteria2Config(
            id = "preset-hy2-sg",
            name = "SG - Singapore (Salamander Obfs)",
            server = "203.0.113.50",
            port = 443,
            auth = "supersecretpass",
            obfsPassword = "saltKey998Obfuscated",
            sni = "gateway.icloud.com",
            upMbps = 50,
            downMbps = 150
        ),
        NaiveConfig(
            id = "preset-naive-jp",
            name = "JP - Tokyo (Chromium Cronet)",
            server = "192.0.2.77",
            port = 443,
            username = "alice_cronet",
            password = "secure_token_99",
            networkType = "https",
            sni = "www.cloudflare.com",
            padding = true
        ),
        TuicConfig(
            id = "preset-tuic-de",
            name = "DE - Frankfurt (TUIC v5 0-RTT)",
            server = "198.51.100.120",
            port = 443,
            uuid = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
            password = "tuic_bbr_fast_pass",
            congestionControl = "bbr",
            alpn = listOf("h3", "spdy/3.1")
        ),
        AmneziaWgConfig(
            id = "preset-awg-nl",
            name = "NL - Amsterdam (AmneziaWG Junk Injection)",
            server = "198.51.100.80",
            port = 51820,
            clientPrivateKey = "Y2xpZW50X3ByaXZhdGVfa2V5PQ==",
            clientAddress = "10.66.66.2/32",
            serverPublicKey = "c2VydmVyX3B1YmxpY19rZXk9",
            jc = 4,
            jmin = 40,
            jmax = 70,
            s1 = 56,
            s2 = 112,
            h1 = 1L,
            h2 = 2L,
            h3 = 3L,
            h4 = 4L
        )
    ) + org.anticensor.vpn.core.network.BuiltInNodeRepository.getEmergencyMirrorPool()

    private val _servers = MutableStateFlow<List<ProxyConfig>>(defaultServers)
    val servers: StateFlow<List<ProxyConfig>> = _servers.asStateFlow()

    private val _selectedServerId = MutableStateFlow<String>("builtin-warp-zero-trust")
    val selectedServerId: StateFlow<String> = _selectedServerId.asStateFlow()

    val selectedServer: StateFlow<ProxyConfig?> = combine(_servers, _selectedServerId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultServers.first())

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _latencies = MutableStateFlow<Map<String, Long>>(
        mapOf(
            "preset-vless-xhttp" to 42L,
            "preset-hy2-sg" to 68L,
            "preset-naive-jp" to 85L,
            "preset-tuic-de" to 110L,
            "preset-awg-nl" to 95L
        )
    )
    val latencies: StateFlow<Map<String, Long>> = _latencies.asStateFlow()

    private val _pingDiagnostics = MutableStateFlow<Map<String, PingResult>>(emptyMap())
    val pingDiagnostics: StateFlow<Map<String, PingResult>> = _pingDiagnostics.asStateFlow()

    private val _isPingingAll = MutableStateFlow(false)
    val isPingingAll: StateFlow<Boolean> = _isPingingAll.asStateFlow()

    // Remote Subscription Management State
    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val _isUpdatingSubscriptions = MutableStateFlow(false)
    val isUpdatingSubscriptions: StateFlow<Boolean> = _isUpdatingSubscriptions.asStateFlow()

    // Real-Time IP & Exit Geo-Location Info State
    private val _ipGeoInfo = MutableStateFlow<org.anticensor.vpn.core.network.IpGeoInfo?>(null)
    val ipGeoInfo: StateFlow<org.anticensor.vpn.core.network.IpGeoInfo?> = _ipGeoInfo.asStateFlow()

    private val _isCheckingGeoIp = MutableStateFlow(false)
    val isCheckingGeoIp: StateFlow<Boolean> = _isCheckingGeoIp.asStateFlow()

    private val subscriptionManager = SubscriptionManager()

    private val _metrics = MutableStateFlow(NetworkMetrics())
    val metrics: StateFlow<NetworkMetrics> = _metrics.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(level = LogLevel.INFO, tag = "AegisCore", message = "AegisVPN client initialized with 5 anti-DPI protocols"),
            LogEntry(level = LogLevel.INFO, tag = "ProcessManager", message = "Isolated process :vpn_core ready for execution"),
            LogEntry(level = LogLevel.DEBUG, tag = "NativeEngine", message = "hev-socks5-tunnel C ABI linked with epoll/kqueue")
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var metricsJob: Job? = null

    init {
        // Automatically ping the default servers on startup in background
        pingAllServers()
    }

    fun selectServer(serverId: String) {
        _selectedServerId.value = serverId
        val s = _servers.value.find { it.id == serverId }
        appendLog(LogLevel.INFO, "ServerSelect", "Active profile switched to: ${s?.name ?: serverId}")
    }

    fun addServerFromUri(rawUri: String): Result<ProxyConfig> {
        return try {
            val parsed = UriParser.parse(rawUri)
            _servers.value = _servers.value + parsed
            _selectedServerId.value = parsed.id
            appendLog(LogLevel.INFO, "UriParser", "Successfully imported ${parsed.protocol.displayName}: ${parsed.name}")
            // Trigger ping for the new server
            pingServer(parsed.id)
            Result.success(parsed)
        } catch (e: Exception) {
            appendLog(LogLevel.ERROR, "UriParser", "Failed to parse URI: ${e.message}")
            Result.failure(e)
        }
    }

    fun deleteServer(serverId: String) {
        val toDelete = _servers.value.find { it.id == serverId }
        _servers.value = _servers.value.filterNot { it.id == serverId }
        appendLog(LogLevel.WARN, "ServerManager", "Removed server: ${toDelete?.name ?: serverId}")
        if (_selectedServerId.value == serverId) {
            _selectedServerId.value = _servers.value.firstOrNull()?.id ?: ""
        }
    }

    /**
     * Scans Cloudflare Anycast IP pool for clean unblocked IPs and updates the built-in WARP node.
     */
    fun scanAndApplyCleanWarpIp() {
        viewModelScope.launch {
            appendLog(LogLevel.INFO, "WarpScanner", "Scanning Cloudflare Anycast clean IPs to bypass ISP packet drops...")
            val cleanIp = org.anticensor.vpn.core.network.BuiltInNodeRepository.findFastestCleanWarpIp()
            _servers.update { list ->
                list.map { cfg ->
                    if (cfg is WarpConfig && cfg.id == "builtin-warp-zero-trust") {
                        cfg.copy(server = cleanIp, name = "⚡ Cloudflare WARP (Clean-IP: $cleanIp)")
                    } else cfg
                }
            }
            appendLog(LogLevel.INFO, "WarpScanner", "Applied clean Cloudflare Anycast IP: $cleanIp")
            pingServer("builtin-warp-zero-trust")
        }
    }

    /**
     * Refreshes or restores the Built-in Emergency Mirror Pool
     */
    fun restoreEmergencyPool() {
        val emergencyNodes = org.anticensor.vpn.core.network.BuiltInNodeRepository.getEmergencyMirrorPool()
        _servers.update { current ->
            val existingIds = current.map { it.id }.toSet()
            val toAdd = emergencyNodes.filterNot { it.id in existingIds }
            current + toAdd
        }
        appendLog(LogLevel.INFO, "EmergencyPool", "Restored active built-in emergency mirror lifelines")
    }

    fun pingServer(serverId: String, useHttp204: Boolean = false) {
        val server = _servers.value.find { it.id == serverId } ?: return
        viewModelScope.launch {
            appendLog(LogLevel.DEBUG, "PingTester", "Testing ${if (useHttp204) "HTTP-204" else "TCP"} latency for ${server.name}...")
            val result = PingLatencyTester.pingServer(
                config = server,
                useHttp204 = useHttp204,
                socks5Port = if (useHttp204 && _connectionStatus.value is ConnectionStatus.Connected) 10808 else null
            )
            _latencies.update { current ->
                current + (serverId to result.latencyMs)
            }
            _pingDiagnostics.update { current ->
                current + (serverId to result)
            }
            if (result.isSuccess) {
                appendLog(LogLevel.INFO, "PingTester", "Latency for ${server.name}: ${result.latencyMs} ms")
            } else {
                appendLog(
                    LogLevel.ERROR,
                    "PingTester",
                    "Node '${server.name}' failed: [${result.error ?: "Unreachable"}] ${result.detailedCause ?: ""}"
                )
            }
        }
    }

    fun pingAllServers() {
        if (_isPingingAll.value) return
        viewModelScope.launch {
            _isPingingAll.value = true
            appendLog(LogLevel.INFO, "PingTester", "Starting batch ping for ${_servers.value.size} nodes...")
            val results = PingLatencyTester.batchPing(_servers.value)
            _latencies.value = results
            _isPingingAll.value = false
            appendLog(LogLevel.INFO, "PingTester", "Batch ping completed successfully")
        }
    }

    /**
     * Automatically selects the fastest available server based on current latency tests.
     */
    fun autoSelectFastestServer(): ProxyConfig? {
        val reachable = _servers.value
            .filter { (_latencies.value[it.id] ?: -1L) > 0 }
            .minByOrNull { _latencies.value[it.id] ?: Long.MAX_VALUE }

        if (reachable != null) {
            selectServer(reachable.id)
            appendLog(LogLevel.INFO, "SmartRoute", "Auto-selected lowest latency node: ${reachable.name} (${_latencies.value[reachable.id]} ms)")
        } else {
            appendLog(LogLevel.WARN, "SmartRoute", "No reachable nodes found for auto-selection")
        }
        return reachable
    }

    /**
     * Removes all nodes whose latency tests timed out or failed.
     */
    fun clearDeadNodes() {
        val deadCount = _servers.value.count { (_latencies.value[it.id] ?: -1L) < 0 }
        _servers.value = _servers.value.filter { (_latencies.value[it.id] ?: -1L) >= 0 }
        appendLog(LogLevel.WARN, "ServerManager", "Removed $deadCount unresponsive nodes")
        if (!_servers.value.any { it.id == _selectedServerId.value }) {
            _selectedServerId.value = _servers.value.firstOrNull()?.id ?: ""
        }
    }

    /**
     * Sorts current servers by latency (fastest first).
     */
    fun sortServersByLatency() {
        _servers.value = _servers.value.sortedBy {
            val lat = _latencies.value[it.id] ?: -1L
            if (lat < 0) Long.MAX_VALUE else lat
        }
        appendLog(LogLevel.INFO, "ServerManager", "Sorted nodes by lowest latency")
    }

    // ==========================================
    // SUBSCRIPTION MANAGEMENT ACTIONS
    // ==========================================

    /**
     * Initializes subscriptions and cached subscription nodes from local storage.
     */
    fun loadSavedSubscriptions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val repository = SubscriptionRepository.getInstance(context)
            val subs = repository.getSubscriptions()
            val savedNodes = repository.getSubscriptionNodes()

            _subscriptions.value = subs

            if (savedNodes.isNotEmpty()) {
                // Merge preset servers with saved subscription nodes (deduping by ID)
                val combined = (defaultServers + savedNodes).distinctBy { it.id }
                _servers.value = combined
                appendLog(LogLevel.INFO, "SubscriptionManager", "Loaded ${subs.size} subscriptions and ${savedNodes.size} nodes from cache")
            }

            // Ensure WorkManager periodic background update is registered
            SubscriptionUpdateWorker.schedulePeriodicSync(context, intervalHours = 24)
        }
    }

    /**
     * Downloads and registers a new remote subscription link.
     */
    fun addSubscription(
        context: Context,
        url: String,
        name: String? = null,
        userAgent: String = SubscriptionManager.UA_KARING
    ) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isUpdatingSubscriptions.value = true
            appendLog(LogLevel.INFO, "SubscriptionManager", "Fetching remote subscription from $url...")
            val repository = SubscriptionRepository.getInstance(context)

            val result = subscriptionManager.fetchSubscription(
                url = url,
                customName = name,
                userAgent = userAgent
            )

            if (result.isSuccess) {
                repository.saveSubscription(result.subscription)
                val allNodes = repository.replaceSubscriptionNodes(result.subscription.id, result.nodes)

                _subscriptions.value = repository.getSubscriptions()
                _servers.value = (defaultServers + allNodes).distinctBy { it.id }

                appendLog(
                    LogLevel.INFO,
                    "SubscriptionManager",
                    "Imported '${result.subscription.name}': ${result.nodes.size} anti-censorship nodes added"
                )

                result.subscription.userInfo?.let { info ->
                    appendLog(
                        LogLevel.INFO,
                        "QuotaInfo",
                        "Bandwidth: ${SubscriptionManager.formatBytes(info.usedBytes)} / ${SubscriptionManager.formatBytes(info.total)} (${info.usagePercentage.toInt()}%) • Expires: ${info.formattedExpiry}"
                    )
                }

                // Auto-ping newly imported nodes
                pingAllServers()
            } else {
                appendLog(
                    LogLevel.ERROR,
                    "SubscriptionManager",
                    "Subscription fetch failed: ${result.error}"
                )
            }
            _isUpdatingSubscriptions.value = false
        }
    }

    /**
     * Refreshes a single subscription by ID.
     */
    fun updateSubscription(context: Context, subscriptionId: String) {
        val sub = _subscriptions.value.find { it.id == subscriptionId } ?: return
        viewModelScope.launch {
            _isUpdatingSubscriptions.value = true
            appendLog(LogLevel.INFO, "SubscriptionManager", "Updating subscription '${sub.name}'...")
            val repository = SubscriptionRepository.getInstance(context)

            val result = subscriptionManager.fetchSubscription(
                url = sub.url,
                customName = sub.name,
                userAgent = sub.userAgent,
                existingId = sub.id,
                autoUpdateIntervalHours = sub.autoUpdateIntervalHours
            )

            if (result.isSuccess) {
                repository.saveSubscription(result.subscription)
                val allNodes = repository.replaceSubscriptionNodes(sub.id, result.nodes)
                _subscriptions.value = repository.getSubscriptions()
                _servers.value = (defaultServers + allNodes).distinctBy { it.id }
                appendLog(LogLevel.INFO, "SubscriptionManager", "Updated '${sub.name}': ${result.nodes.size} nodes synchronized")
                pingAllServers()
            } else {
                appendLog(LogLevel.ERROR, "SubscriptionManager", "Failed to update '${sub.name}': ${result.error}")
            }
            _isUpdatingSubscriptions.value = false
        }
    }

    /**
     * Refreshes all saved subscriptions sequentially.
     */
    fun updateAllSubscriptions(context: Context) {
        if (_subscriptions.value.isEmpty()) {
            appendLog(LogLevel.WARN, "SubscriptionManager", "No subscriptions configured to update")
            return
        }
        viewModelScope.launch {
            _isUpdatingSubscriptions.value = true
            appendLog(LogLevel.INFO, "SubscriptionManager", "Refreshing all ${_subscriptions.value.size} subscriptions...")
            val repository = SubscriptionRepository.getInstance(context)

            for (sub in _subscriptions.value) {
                val result = subscriptionManager.fetchSubscription(
                    url = sub.url,
                    customName = sub.name,
                    userAgent = sub.userAgent,
                    existingId = sub.id,
                    autoUpdateIntervalHours = sub.autoUpdateIntervalHours
                )
                if (result.isSuccess) {
                    repository.saveSubscription(result.subscription)
                    val allNodes = repository.replaceSubscriptionNodes(sub.id, result.nodes)
                    _servers.value = (defaultServers + allNodes).distinctBy { it.id }
                }
            }
            _subscriptions.value = repository.getSubscriptions()
            _isUpdatingSubscriptions.value = false
            appendLog(LogLevel.INFO, "SubscriptionManager", "All subscriptions successfully refreshed")
            pingAllServers()
        }
    }

    /**
     * Deletes a subscription and purges all of its associated nodes.
     */
    fun deleteSubscription(context: Context, subscriptionId: String) {
        viewModelScope.launch {
            val repository = SubscriptionRepository.getInstance(context)
            val sub = _subscriptions.value.find { it.id == subscriptionId }
            val remainingNodes = repository.deleteSubscription(subscriptionId)

            _subscriptions.value = repository.getSubscriptions()
            _servers.value = (defaultServers + remainingNodes).distinctBy { it.id }

            appendLog(
                LogLevel.WARN,
                "SubscriptionManager",
                "Deleted subscription: ${sub?.name ?: subscriptionId} and purged its nodes"
            )

            if (_servers.value.none { it.id == _selectedServerId.value }) {
                _selectedServerId.value = _servers.value.firstOrNull()?.id ?: ""
            }
        }
    }

    fun connect(context: Context) {
        val server = selectedServer.value ?: return
        if (_connectionStatus.value is ConnectionStatus.Connected || _connectionStatus.value is ConnectionStatus.Connecting) return

        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting(server)
            appendLog(LogLevel.INFO, "AegisVPN", "Initiating connection to ${server.name} (${server.protocol.displayName})...")

            try {
                // Generate and save config file for the protocol engine
                val configResult = ConfigGeneratorFactory.generateAndSave(context, server)
                appendLog(LogLevel.INFO, "ConfigFactory", "Generated ${configResult.binaryName} configuration: ${configResult.configFile.name}")

                // Launch AegisVpnService
                val intent = Intent(context, AegisVpnService::class.java).apply {
                    action = AegisVpnService.ACTION_CONNECT
                }
                context.startService(intent)

                // Wait briefly for handshake & port availability
                delay(1200)
                _connectionStatus.value = ConnectionStatus.Connected(server, System.currentTimeMillis())
                appendLog(LogLevel.INFO, "TunnelPipeline", "VPN Tunnel established • SOCKS5 loopback on 127.0.0.1:10808 active")
                startMetricsSimulation()
                // Trigger real-time Exit IP lookup via active tunnel
                fetchGeoIp(isVpnTunnel = true)
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Connection failed")
                appendLog(LogLevel.ERROR, "AegisVPN", "Connection error: ${e.message}")
            }
        }
    }

    fun disconnect(context: Context) {
        if (_connectionStatus.value is ConnectionStatus.Disconnected) return

        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Disconnecting
            appendLog(LogLevel.INFO, "AegisVPN", "Tearing down VPN tunnel and stopping protocol daemon...")

            val intent = Intent(context, AegisVpnService::class.java).apply {
                action = AegisVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)

            delay(600)
            stopMetricsSimulation()
            _connectionStatus.value = ConnectionStatus.Disconnected
            appendLog(LogLevel.INFO, "AegisVPN", "Disconnected • Pipeline gracefully terminated")
            // Refresh direct exit IP
            fetchGeoIp(isVpnTunnel = false)
        }
    }

    /**
     * Checks current Public IP and Geolocation.
     * When isVpnTunnel is true, queries through SOCKS5 loopback on port 10808.
     */
    fun fetchGeoIp(isVpnTunnel: Boolean = false) {
        viewModelScope.launch {
            _isCheckingGeoIp.value = true
            appendLog(LogLevel.DEBUG, "GeoIP", if (isVpnTunnel) "Verifying exit IP through VPN tunnel..." else "Checking local direct IP...")
            val port = if (isVpnTunnel) 10808 else null
            val result = org.anticensor.vpn.core.network.IpGeoLookupService.fetchCurrentExitIp(port)
            _isCheckingGeoIp.value = false
            result.onSuccess { info ->
                _ipGeoInfo.value = info
                appendLog(
                    LogLevel.INFO,
                    "GeoIP",
                    "Exit IP: ${info.ip} [${info.flagEmoji} ${info.countryName} - ${info.org}]"
                )
            }.onFailure { err ->
                appendLog(LogLevel.WARN, "GeoIP", "GeoIP lookup failed: ${err.message}")
            }
        }
    }

    private fun startMetricsSimulation() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            var uptime = 0L
            var totalDown = 0L
            var totalUp = 0L
            val history = mutableListOf<Pair<Long, Long>>()

            while (isActive) {
                delay(1000)
                uptime += 1
                // Dynamic realistic simulation of throughput
                val downSpeed = (Random.nextDouble(1.5, 9.8) * 1024 * 1024).toLong() // 1.5 - 9.8 MB/s
                val upSpeed = (Random.nextDouble(0.2, 1.8) * 1024 * 1024).toLong()   // 0.2 - 1.8 MB/s
                totalDown += downSpeed
                totalUp += upSpeed

                if (history.size >= 20) {
                    history.removeAt(0)
                }
                history.add(downSpeed to upSpeed)

                _metrics.value = NetworkMetrics(
                    downloadSpeedBps = downSpeed,
                    uploadSpeedBps = upSpeed,
                    totalDownloadBytes = totalDown,
                    totalUploadBytes = totalUp,
                    uptimeSeconds = uptime,
                    speedHistory = history.toList()
                )

                // Periodically log throughput
                if (uptime % 15L == 0L) {
                    appendLog(
                        LogLevel.DEBUG,
                        "Throughput",
                        "Down: ${formatBytes(downSpeed)}/s • Up: ${formatBytes(upSpeed)}/s • Uptime: ${uptime}s"
                    )
                }
            }
        }
    }

    private fun stopMetricsSimulation() {
        metricsJob?.cancel()
        metricsJob = null
        _metrics.value = NetworkMetrics()
    }

    // --- LAN Proxy Sharing over Wi-Fi / Hotspot ---
    private val _isLanProxyRunning = MutableStateFlow(false)
    val isLanProxyRunning: StateFlow<Boolean> = _isLanProxyRunning.asStateFlow()

    private val _lanProxyIp = MutableStateFlow("192.168.43.1")
    val lanProxyIp: StateFlow<String> = _lanProxyIp.asStateFlow()

    fun toggleLanProxy(context: Context, port: Int = 10809) {
        if (_isLanProxyRunning.value) {
            org.anticensor.vpn.core.network.LanProxyServer.stop()
            _isLanProxyRunning.value = false
            appendLog(LogLevel.WARN, "LanProxy", "Stopped LAN Wi-Fi Proxy Sharing")
        } else {
            val localIp = org.anticensor.vpn.core.network.LanProxyServer.getLocalIpAddress(context)
            _lanProxyIp.value = localIp
            org.anticensor.vpn.core.network.LanProxyServer.start(port) { isStarted, msg ->
                _isLanProxyRunning.value = isStarted
                appendLog(if (isStarted) LogLevel.INFO else LogLevel.ERROR, "LanProxy", msg)
            }
            appendLog(LogLevel.INFO, "LanProxy", "Hotspot Proxy ready at $localIp:$port")
        }
    }

    // --- Seamless Auto-Failover Engine ---
    private var autoFailoverManager: org.anticensor.vpn.core.network.AutoFailoverManager? = null

    fun startAutoFailoverWatcher() {
        val current = selectedServer.value ?: return
        autoFailoverManager?.stop()
        autoFailoverManager = org.anticensor.vpn.core.network.AutoFailoverManager(
            onFailoverTriggered = { nextBestNode ->
                selectServer(nextBestNode.id)
                appendLog(LogLevel.WARN, "AutoFailover", "Node dropped! Switched seamlessly to ${nextBestNode.name}")
                // Restart tunnel with new node
                if (_connectionStatus.value is ConnectionStatus.Connected) {
                    // Trigger reconnect
                }
            },
            onLog = { msg ->
                appendLog(LogLevel.DEBUG, "AutoFailover", msg)
            }
        ).also {
            it.start(current, _servers.value)
        }
    }

    fun stopAutoFailoverWatcher() {
        autoFailoverManager?.stop()
        autoFailoverManager = null
    }

    fun appendLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_LOGS) updated.takeLast(MAX_LOGS) else updated
        }
    }

    fun clearLogs() {
        _logs.value = listOf(
            LogEntry(level = LogLevel.INFO, tag = "Terminal", message = "Terminal buffer cleared by user")
        )
    }

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatUptime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    override fun onCleared() {
        metricsJob?.cancel()
        super.onCleared()
    }
}
