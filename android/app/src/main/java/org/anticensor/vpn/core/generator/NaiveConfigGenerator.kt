package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.NaiveConfig
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Generates JSON configuration files for NaïveProxy (Chromium Cronet network stack).
 */
object NaiveConfigGenerator {

    fun generateJson(config: NaiveConfig, socks5Port: Int = 10808): String {
        val root = JSONObject()

        // SOCKS5 listener for hev-socks5-tunnel
        root.put("listen", "socks://127.0.0.1:$socks5Port")

        // Encoded Proxy URI: scheme://user:pass@host:port
        val scheme = if (config.networkType.equals("quic", ignoreCase = true)) "quic" else "https"
        val encUser = URLEncoder.encode(config.username, StandardCharsets.UTF_8.name())
        val encPass = URLEncoder.encode(config.password, StandardCharsets.UTF_8.name())
        val proxyUri = "$scheme://$encUser:$encPass@${config.server}:${config.port}"
        root.put("proxy", proxyUri)

        // Anti-DPI Padding & Concurrency
        root.put("padding", config.padding)
        root.put("insecure_concurrency", config.concurrency.coerceAtLeast(1))
        root.put("log", "")

        return root.toString(2)
    }

    fun writeConfigFile(config: NaiveConfig, targetFile: File, socks5Port: Int = 10808): File {
        val json = generateJson(config, socks5Port)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(json)
        return targetFile
    }
}
