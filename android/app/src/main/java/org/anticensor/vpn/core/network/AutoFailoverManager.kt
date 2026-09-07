package org.anticensor.vpn.core.network

import kotlinx.coroutines.*
import org.anticensor.vpn.core.parser.ProxyConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Intelligent Seamless Auto-Failover Monitor.
 * Continuously tests tunnel heartbeat in background. If an ISP drops packets or the current node fails,
 * this engine automatically initiates an instant switch to the fastest active backup lifeline
 * without manual user intervention.
 */
class AutoFailoverManager(
    private val onFailoverTriggered: (ProxyConfig) -> Unit,
    private val onLog: (String) -> Unit
) {
    private var monitorJob: Job? = null
    private val isMonitoring = AtomicBoolean(false)
    private val consecutiveFailures = AtomicInteger(0)

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 3
        private const val PROBE_INTERVAL_MS = 8000L
    }

    fun start(currentNode: ProxyConfig, candidatePool: List<ProxyConfig>) {
        if (isMonitoring.get()) return
        isMonitoring.set(true)
        consecutiveFailures.set(0)

        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            onLog("Auto-Failover heartbeat watcher activated for ${currentNode.name}")
            while (isActive && isMonitoring.get()) {
                delay(PROBE_INTERVAL_MS)

                // Test current tunnel connectivity via HTTP-204 loopback
                val probeResult = PingLatencyTester.pingServer(
                    config = currentNode,
                    useHttp204 = true,
                    socks5Port = 10808
                )

                if (probeResult.isSuccess && probeResult.latencyMs < 3500) {
                    consecutiveFailures.set(0)
                } else {
                    val failures = consecutiveFailures.incrementAndGet()
                    onLog("Node packet drop detected ($failures/$MAX_CONSECUTIVE_FAILURES): ${probeResult.errorMessage ?: "Timeout"}")

                    if (failures >= MAX_CONSECUTIVE_FAILURES) {
                        onLog("Failover threshold reached! Searching for fastest healthy lifeline...")
                        consecutiveFailures.set(0)

                        val backupCandidates = candidatePool.filter { it.id != currentNode.id }
                        if (backupCandidates.isNotEmpty()) {
                            // Find candidate with lowest latency
                            val nextBest = backupCandidates.minByOrNull {
                                PingLatencyTester.measureTcpHandshake(it.server, it.port)
                            } ?: backupCandidates.first()

                            onLog("Auto-Failover triggered: Switching seamlessly to ${nextBest.name}")
                            withContext(Dispatchers.Main) {
                                onFailoverTriggered(nextBest)
                            }
                            break
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        isMonitoring.set(false)
        consecutiveFailures.set(0)
        monitorJob?.cancel()
        monitorJob = null
    }
}
