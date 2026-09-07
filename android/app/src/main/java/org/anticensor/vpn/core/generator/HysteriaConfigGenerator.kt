package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.Hysteria2Config
import java.io.File

/**
 * Generates spec-compliant YAML configuration files for the Hysteria 2 native client.
 * Features Salamander protocol obfuscation, BBR/brutal congestion control, and local SOCKS5 binding.
 */
object HysteriaConfigGenerator {

    fun generateYaml(config: Hysteria2Config, socks5Port: Int = 10808): String {
        val sb = StringBuilder()

        // Server Address & Port Hopping to defeat UDP rate-limiting
        val serverAddr = if (config.portHopRange.isNotBlank()) {
            "${config.server}:${config.port},${config.portHopRange}"
        } else {
            "${config.server}:${config.port}"
        }
        sb.appendLine("server: $serverAddr")
        if (config.hopIntervalSeconds > 0 && config.portHopRange.isNotBlank()) {
            sb.appendLine("hopInterval: ${config.hopIntervalSeconds}s")
        }
        sb.appendLine("auth: \"${escapeYaml(config.auth)}\"")
        sb.appendLine()

        // Local SOCKS5 Inbound for hev-socks5-tunnel
        sb.appendLine("socks5:")
        sb.appendLine("  listen: 127.0.0.1:$socks5Port")
        sb.appendLine("  timeout: 300")
        sb.appendLine()

        // Obfuscation (Salamander)
        if (config.obfsPassword.isNotBlank()) {
            sb.appendLine("obfs:")
            sb.appendLine("  type: ${config.obfsType}")
            sb.appendLine("  ${config.obfsType}:")
            sb.appendLine("    password: \"${escapeYaml(config.obfsPassword)}\"")
            sb.appendLine()
        }

        // TLS & Camouflage
        sb.appendLine("tls:")
        val sni = if (config.sni.isNotBlank()) config.sni else config.server
        sb.appendLine("  sni: $sni")
        sb.appendLine("  insecure: ${config.insecure}")
        sb.appendLine()

        // Bandwidth Limits
        sb.appendLine("bandwidth:")
        sb.appendLine("  up: ${config.upMbps} mbps")
        sb.appendLine("  down: ${config.downMbps} mbps")
        sb.appendLine()

        // QUIC Tunables for Anti-Censorship
        sb.appendLine("quic:")
        sb.appendLine("  initStreamReceiveWindow: 8388608")
        sb.appendLine("  maxStreamReceiveWindow: 8388608")
        sb.appendLine("  initConnReceiveWindow: 20971520")
        sb.appendLine("  maxConnReceiveWindow: 20971520")
        sb.appendLine("  maxIdleTimeout: 30s")
        sb.appendLine("  keepAlivePeriod: 10s")
        sb.appendLine("  disablePathMTUDiscovery: false")
        sb.appendLine()

        // Fast Open
        sb.appendLine("fastOpen: true")

        return sb.toString()
    }

    private fun escapeYaml(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    fun writeConfigFile(config: Hysteria2Config, targetFile: File, socks5Port: Int = 10808): File {
        val yaml = generateYaml(config, socks5Port)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(yaml)
        return targetFile
    }
}
