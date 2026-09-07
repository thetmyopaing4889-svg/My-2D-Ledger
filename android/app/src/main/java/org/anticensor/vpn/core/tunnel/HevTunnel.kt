package org.anticensor.vpn.core.tunnel

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * State of the native hev-socks5-tunnel engine.
 */
enum class TunnelState {
    STOPPED,
    INITIALIZING,
    RUNNING,
    ERROR
}

/**
 * Live network metrics snapshot from hev-socks5-tunnel.
 */
data class TunnelStats(
    val txBytes: Long = 0L,
    val rxBytes: Long = 0L,
    val txSpeedBps: Long = 0L,
    val rxSpeedBps: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Production JNI wrapper and lifecycle manager for hev-socks5-tunnel.
 * Bridges the Linux TUN file descriptor provided by [android.net.VpnService]
 * directly to the local SOCKS5 proxy port (127.0.0.1:10808) managed by the active protocol daemon.
 */
object HevTunnel {
    private const val TAG = "HevTunnel"

    private val _state = MutableStateFlow(TunnelState.STOPPED)
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(TunnelStats())
    val stats: StateFlow<TunnelStats> = _stats.asStateFlow()

    private var statsJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var lastTxBytes: Long = 0L
    private var lastRxBytes: Long = 0L
    private var lastTimestamp: Long = 0L

    init {
        try {
            System.loadLibrary("hev-socks5-tunnel")
            val ver = try { nativeGetVersion() } catch (e: Throwable) { "unknown" }
            Log.i(TAG, "libhev-socks5-tunnel.so loaded successfully (core version: $ver)")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "FATAL: Failed to load libhev-socks5-tunnel.so", e)
            _state.value = TunnelState.ERROR
        }
    }

    /**
     * Initializes hev-socks5-tunnel with the generated YAML configuration path and the TUN device FD.
     */
    @Synchronized
    fun init(configPath: String, tunFd: Int): Int {
        if (_state.value == TunnelState.RUNNING) {
            Log.w(TAG, "Tunnel already running. Must stop prior to re-init.")
            return -1
        }

        _state.value = TunnelState.INITIALIZING
        val result = nativeInit(configPath, tunFd)
        if (result != 0) {
            Log.e(TAG, "nativeInit failed with code: $result")
            _state.value = TunnelState.ERROR
        }
        return result
    }

    /**
     * Starts the native event loop on a dedicated POSIX pthread and begins stats polling.
     */
    @Synchronized
    fun start(): Int {
        val result = nativeStart()
        if (result == 0) {
            _state.value = TunnelState.RUNNING
            startStatsPolling()
            Log.i(TAG, "hev-socks5-tunnel running.")
        } else {
            Log.e(TAG, "nativeStart failed with code: $result")
            _state.value = TunnelState.ERROR
        }
        return result
    }

    /**
     * Signals the tunnel to terminate its event loop, waits for pthread exit, and releases resources.
     */
    @Synchronized
    fun stop(): Int {
        stopStatsPolling()
        val result = nativeStop()
        _state.value = TunnelState.STOPPED
        _stats.value = TunnelStats()
        Log.i(TAG, "hev-socks5-tunnel stopped.")
        return result
    }

    /**
     * Retrieves cumulative network throughput statistics: [txBytes, rxBytes].
     */
    fun getRawStats(): LongArray {
        val raw = LongArray(2)
        try {
            nativeGetStats(raw)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to fetch native tunnel stats", e)
        }
        return raw
    }

    /**
     * Queries the compiled native version string.
     */
    fun getVersion(): String {
        return try {
            nativeGetVersion()
        } catch (e: Throwable) {
            "2.7.3-aegis"
        }
    }

    private fun startStatsPolling() {
        stopStatsPolling()
        lastTxBytes = 0L
        lastRxBytes = 0L
        lastTimestamp = System.currentTimeMillis()

        statsJob = scope.launch {
            while (isActive && _state.value == TunnelState.RUNNING) {
                val raw = getRawStats()
                val currentTx = raw[0]
                val currentRx = raw[1]
                val now = System.currentTimeMillis()

                val elapsedSec = (now - lastTimestamp).coerceAtLeast(1) / 1000.0
                val txSpeed = if (lastTxBytes > 0 && currentTx >= lastTxBytes) {
                    ((currentTx - lastTxBytes) / elapsedSec).toLong()
                } else 0L

                val rxSpeed = if (lastRxBytes > 0 && currentRx >= lastRxBytes) {
                    ((currentRx - lastRxBytes) / elapsedSec).toLong()
                } else 0L

                lastTxBytes = currentTx
                lastRxBytes = currentRx
                lastTimestamp = now

                _stats.value = TunnelStats(
                    txBytes = currentTx,
                    rxBytes = currentRx,
                    txSpeedBps = txSpeed,
                    rxSpeedBps = rxSpeed,
                    timestampMs = now
                )

                delay(1000) // Poll every 1 second
            }
        }
    }

    private fun stopStatsPolling() {
        statsJob?.cancel()
        statsJob = null
    }

    // JNI External Declarations
    private external fun nativeInit(configPath: String, tunFd: Int): Int
    private external fun nativeStart(): Int
    private external fun nativeStop(): Int
    private external fun nativeGetStats(stats: LongArray)
    private external fun nativeGetVersion(): String
}
