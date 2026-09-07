package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.TuicConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Generates JSON configuration files for TUIC v5 (0-RTT QUIC multiplexed proxy client).
 */
object TuicConfigGenerator {

    fun generateJson(config: TuicConfig, socks5Port: Int = 10808): String {
        val root = JSONObject()

        // Relay remote server configuration
        val relay = JSONObject().apply {
            put("server", "${config.server}:${config.port}")
            put("uuid", config.uuid)
            put("password", config.password)
            put("certificates", JSONArray())
            put("udp_relay_mode", config.udpRelayMode)
            put("zero_rtt_handshake", config.zeroRttHandshake)
            put("disable_sni", config.disableSni)
            put("congestion_control", config.congestionControl)
            put("heartbeat", "${config.heartbeatIntervalMs / 1000}s")

            if (config.sni.isNotBlank() && !config.disableSni) {
                put("sni", config.sni)
            }
            put("alpn", JSONArray(config.alpn))
        }
        root.put("relay", relay)

        // Local SOCKS5 listener for hev-socks5-tunnel
        val local = JSONObject().apply {
            put("server", "127.0.0.1:$socks5Port")
            put("dual_stack", false)
            put("max_packet_size", 1500)
        }
        root.put("local", local)

        root.put("log_level", "warn")

        return root.toString(2)
    }

    fun writeConfigFile(config: TuicConfig, targetFile: File, socks5Port: Int = 10808): File {
        val json = generateJson(config, socks5Port)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(json)
        return targetFile
    }
}
