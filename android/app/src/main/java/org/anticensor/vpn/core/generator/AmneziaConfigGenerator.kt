package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.AmneziaWgConfig
import java.io.File

/**
 * Generates AmneziaWG / WireGuard configuration files (.conf).
 * Preserves all anti-DPI junk injection parameters (Jc, Jmin, Jmax, S1, S2, H1, H2, H3, H4)
 * used to defeat state-level deep packet inspection of WireGuard handshakes.
 */
object AmneziaConfigGenerator {

    fun generateConf(config: AmneziaWgConfig): String {
        val sb = StringBuilder()

        // [Interface] Section
        sb.appendLine("[Interface]")
        val addresses = mutableListOf(config.addressIpv4)
        if (!config.addressIpv6.isNullOrBlank()) {
            addresses.add(config.addressIpv6)
        }
        sb.appendLine("Address = ${addresses.joinToString(", ")}")
        sb.appendLine("PrivateKey = ${config.privateKey}")
        sb.appendLine("DNS = ${config.dns.joinToString(", ")}")
        sb.appendLine("MTU = ${config.mtu}")

        // Anti-Censorship Junk Packet Injection & Header Mutation
        sb.appendLine("Jc = ${config.jc}")
        sb.appendLine("Jmin = ${config.jmin}")
        sb.appendLine("Jmax = ${config.jmax}")
        sb.appendLine("S1 = ${config.s1}")
        sb.appendLine("S2 = ${config.s2}")
        sb.appendLine("H1 = ${config.h1}")
        sb.appendLine("H2 = ${config.h2}")
        sb.appendLine("H3 = ${config.h3}")
        sb.appendLine("H4 = ${config.h4}")
        sb.appendLine()

        // [Peer] Section
        sb.appendLine("[Peer]")
        sb.appendLine("PublicKey = ${config.publicKey}")
        if (!config.presharedKey.isNullOrBlank()) {
            sb.appendLine("PresharedKey = ${config.presharedKey}")
        }
        sb.appendLine("Endpoint = ${config.server}:${config.port}")
        sb.appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
        sb.appendLine("PersistentKeepalive = 25")

        return sb.toString()
    }

    fun writeConfigFile(config: AmneziaWgConfig, targetFile: File): File {
        val conf = generateConf(config)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(conf)
        return targetFile
    }
}
