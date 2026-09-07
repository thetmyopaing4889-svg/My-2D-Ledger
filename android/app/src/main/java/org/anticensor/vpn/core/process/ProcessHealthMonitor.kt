package org.anticensor.vpn.core.process

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket

data class HealthStatus(
    val isHealthy: Boolean = false,
    val socks5Port: Int = 10808,
    val latencyMs: Long = -1,
    val consecutiveFailures: Int = 0,
    val lastCheckTimestamp: Long = 0
)

/**
 * Monitors the local SOCKS5 loopback listener (127.0.0.1:10808) created by the active protocol daemon.
 * Sends periodic SOCKS5 handshake probe (VER=0x05, NMETHODS=1, METHOD=0x00 NO_AUTH) to verify
 * that the daemon is accepting connections and not locked up or zombie.
 */
class ProcessHealthMonitor(
    private val socks5Host: String = "127.0.0.1",
    private val socks5Port: Int = 10808,
    private val checkIntervalMs: Long = 3000L,
    private val timeoutMs: Int = 1200
) {

    companion object {
        private const val TAG = "ProcessHealthMonitor"
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    private val _healthState = MutableStateFlow(HealthStatus(socks5Port = socks5Port))
    val healthState: StateFlow<HealthStatus> = _healthState.asStateFlow()

    private var onHealthFailedCallback: (() -> Unit)? = null

    fun setOnHealthFailedListener(listener: () -> Unit) {
        onHealthFailedCallback = listener
    }

    /**
     * Starts background health probing loop.
     */
    fun startMonitoring() {
        stopMonitoring()
        monitorJob = scope.launch {
            Log.i(TAG, "Starting SOCKS5 loopback probe on $socks5Host:$socks5Port every ${checkIntervalMs}ms")

            var failures = 0
            while (isActive) {
                val start = System.currentTimeMillis()
                val isUp = probeSocks5Listener()
                val latency = if (isUp) System.currentTimeMillis() - start else -1L

                if (isUp) {
                    failures = 0
                    _healthState.value = HealthStatus(
                        isHealthy = true,
                        socks5Port = socks5Port,
                        latencyMs = latency,
                        consecutiveFailures = 0,
                        lastCheckTimestamp = System.currentTimeMillis()
                    )
                } else {
                    failures++
                    _healthState.value = HealthStatus(
                        isHealthy = false,
                        socks5Port = socks5Port,
                        latencyMs = -1,
                        consecutiveFailures = failures,
                        lastCheckTimestamp = System.currentTimeMillis()
                    )
                    Log.w(TAG, "SOCKS5 health probe failed ($failures/$MAX_CONSECUTIVE_FAILURES)")

                    if (failures >= MAX_CONSECUTIVE_FAILURES) {
                        Log.e(TAG, "Process health check critically failed $failures consecutive times!")
                        onHealthFailedCallback?.invoke()
                    }
                }

                delay(checkIntervalMs)
            }
        }
    }

    /**
     * Performs a single synchronous probe with SOCKS5 handshake verification.
     */
    fun probeSocks5Listener(): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(socks5Host, socks5Port), timeoutMs)

            // Send SOCKS5 greeting: [VER=0x05, NMETHODS=1, METHOD=0x00 (No Auth)]
            val out = socket.getOutputStream()
            out.write(byteArrayOf(0x05, 0x01, 0x00))
            out.flush()

            // Read response: [VER=0x05, METHOD=0x00]
            val inStream = socket.getInputStream()
            val response = ByteArray(2)
            val bytesRead = inStream.read(response)

            bytesRead == 2 && response[0] == 0x05.toByte() && response[1] == 0x00.toByte()
        } catch (_: Exception) {
            false
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Waits until the SOCKS5 port is ready (e.g. upon daemon startup).
     * Returns true if ready within maxWaitMs, false if timed out.
     */
    suspend fun waitForPortReady(maxWaitMs: Long = 5000L): Boolean = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (System.currentTimeMillis() < deadline) {
            if (probeSocks5Listener()) {
                Log.i(TAG, "SOCKS5 listener on $socks5Host:$socks5Port is UP and responding")
                return@withContext true
            }
            delay(150)
        }
        Log.e(TAG, "Timed out waiting for SOCKS5 listener on $socks5Host:$socks5Port")
        false
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _healthState.value = HealthStatus(socks5Port = socks5Port, isHealthy = false)
    }

    fun release() {
        stopMonitoring()
        scope.cancel()
    }
}
