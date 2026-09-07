package org.anticensor.vpn.core.process

import kotlinx.coroutines.flow.StateFlow

/**
 * High-level state representing the lifecycle of an anti-censorship protocol daemon.
 */
sealed class DaemonState {
    object Idle : DaemonState()
    data class Extracting(val binaryName: String, val progressPercent: Int) : DaemonState()
    data class Starting(val binaryName: String, val configPath: String) : DaemonState()
    data class Running(
        val binaryName: String,
        val pid: Long,
        val socks5Port: Int,
        val startTimeMs: Long,
        val uptimeSeconds: Long = 0
    ) : DaemonState()
    data class Stopping(val binaryName: String, val pid: Long, val isEscalatedToKill: Boolean = false) : DaemonState()
    data class Crashed(val binaryName: String, val exitCode: Int, val errorMessage: String, val restartCount: Int) : DaemonState()
    data class Terminated(val binaryName: String, val exitCode: Int, val durationMs: Long) : DaemonState()
}

/**
 * Metadata descriptor for supported core binaries.
 */
enum class CoreBinary(
    val executableName: String,
    val displayName: String,
    val description: String,
    val defaultSocks5Port: Int = 10808,
    val supportedProtocols: List<String>
) {
    XRAY(
        executableName = "xray",
        displayName = "Xray Core",
        description = "High-performance modular proxy core with XTLS-Reality, VMess, VLESS, Trojan, and Shadowsocks.",
        supportedProtocols = listOf("vless", "vmess", "trojan", "shadowsocks")
    ),
    HYSTERIA2(
        executableName = "hysteria",
        displayName = "Hysteria 2 Core",
        description = "QUIC-based bandwidth-accelerated proxy with Salamander protocol obfuscation against severe throttling.",
        supportedProtocols = listOf("hysteria2", "hy2")
    ),
    NAIVE(
        executableName = "naive",
        displayName = "NaïveProxy (Cronet)",
        description = "Chromium network stack with HTTP/2 and HTTP/3 camouflaged traffic resisting active probing.",
        supportedProtocols = listOf("naive+https", "naive+quic")
    ),
    AMNEZIA_WG(
        executableName = "amneziawg-go",
        displayName = "AmneziaWG Go Core",
        description = "Modified WireGuard engine with junk packet injection and header mutation resisting DPI.",
        supportedProtocols = listOf("amneziawg", "awg")
    ),
    TUIC(
        executableName = "tuic-client",
        displayName = "TUIC v5 Client",
        description = "0-RTT QUIC multiplexed proxy engine with BBR congestion control.",
        supportedProtocols = listOf("tuic")
    );

    companion object {
        fun fromProtocol(protocolScheme: String): CoreBinary {
            val normalized = protocolScheme.lowercase().trim()
            return entries.firstOrNull { binary ->
                binary.supportedProtocols.any { proto -> normalized.startsWith(proto) }
            } ?: XRAY
        }
    }
}

/**
 * Log entry capturing stdout / stderr from spawned subprocesses.
 */
data class ProcessLogEntry(
    val timestampMs: Long = System.currentTimeMillis(),
    val isStderr: Boolean,
    val message: String
)
