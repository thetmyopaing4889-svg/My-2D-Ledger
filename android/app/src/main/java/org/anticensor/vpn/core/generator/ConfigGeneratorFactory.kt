package org.anticensor.vpn.core.generator

import android.content.Context
import org.anticensor.vpn.core.parser.*
import java.io.File

data class GeneratedConfigResult(
    val configFile: File,
    val configText: String,
    val protocol: ProxyProtocol,
    val binaryName: String,
    val socks5Port: Int,
    val launchArgs: List<String>
)

/**
 * Unified factory that takes any parsed ProxyConfig, applies protocol-specific generators,
 * and writes ready-to-execute configuration files into context.filesDir.
 */
object ConfigGeneratorFactory {

    private const val DEFAULT_SOCKS5_PORT = 10808

    fun generateAndSave(
        context: Context,
        config: ProxyConfig,
        socks5Port: Int = DEFAULT_SOCKS5_PORT
    ): GeneratedConfigResult {
        val configDir = File(context.filesDir, "configs").apply { mkdirs() }

        return when (config) {
            is VlessConfig -> {
                val targetFile = File(configDir, "xray_config.json")
                val text = XrayConfigGenerator.generateJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.VLESS_REALITY,
                    binaryName = "xray",
                    socks5Port = socks5Port,
                    launchArgs = listOf("run", "-c", targetFile.absolutePath)
                )
            }
            is Hysteria2Config -> {
                val targetFile = File(configDir, "hysteria2_config.yaml")
                val text = HysteriaConfigGenerator.generateYaml(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.HYSTERIA2,
                    binaryName = "hysteria",
                    socks5Port = socks5Port,
                    launchArgs = listOf("client", "-c", targetFile.absolutePath)
                )
            }
            is NaiveConfig -> {
                val targetFile = File(configDir, "naive_config.json")
                val text = NaiveConfigGenerator.generateJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.NAIVE_PROXY,
                    binaryName = "naive",
                    socks5Port = socks5Port,
                    launchArgs = listOf(targetFile.absolutePath)
                )
            }
            is TuicConfig -> {
                val targetFile = File(configDir, "tuic_config.json")
                val text = TuicConfigGenerator.generateJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.TUIC,
                    binaryName = "tuic-client",
                    socks5Port = socks5Port,
                    launchArgs = listOf("-c", targetFile.absolutePath)
                )
            }
            is AmneziaWgConfig -> {
                val targetFile = File(configDir, "amnezia_wg0.conf")
                val text = AmneziaConfigGenerator.generateConf(config)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.AMNEZIA_WG,
                    binaryName = "amneziawg-go",
                    socks5Port = socks5Port,
                    launchArgs = listOf("-f", targetFile.absolutePath)
                )
            }
            is ShadowsocksConfig -> {
                val targetFile = File(configDir, "shadowsocks_config.json")
                val text = XrayConfigGenerator.generateShadowsocksJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.SHADOWSOCKS,
                    binaryName = "xray",
                    socks5Port = socks5Port,
                    launchArgs = listOf("run", "-c", targetFile.absolutePath)
                )
            }
            is WarpConfig -> {
                val targetFile = File(configDir, "warp_wireguard.json")
                val text = WarpConfigGenerator.generateJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.WARP,
                    binaryName = "xray",
                    socks5Port = socks5Port,
                    launchArgs = listOf("run", "-c", targetFile.absolutePath)
                )
            }
            is SshTunnelConfig -> {
                val targetFile = File(configDir, "ssh_tunnel.json")
                val text = SshConfigGenerator.generateJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.SSH_TUNNEL,
                    binaryName = "ssh",
                    socks5Port = socks5Port,
                    launchArgs = listOf("-F", targetFile.absolutePath)
                )
            }
            is SsCloakConfig -> {
                val targetFile = File(configDir, "ss_cloak_config.json")
                val text = XrayConfigGenerator.generateSsCloakJson(config, socks5Port)
                targetFile.writeText(text)
                GeneratedConfigResult(
                    configFile = targetFile,
                    configText = text,
                    protocol = ProxyProtocol.SS_CLOAK,
                    binaryName = "xray",
                    socks5Port = socks5Port,
                    launchArgs = listOf("run", "-c", targetFile.absolutePath)
                )
            }
        }
    }

    /**
     * Convenience method to parse a raw URI and immediately produce the configuration file.
     */
    fun parseAndGenerate(
        context: Context,
        rawUri: String,
        socks5Port: Int = DEFAULT_SOCKS5_PORT
    ): GeneratedConfigResult {
        val parsedConfig = UriParser.parse(rawUri)
        return generateAndSave(context, parsedConfig, socks5Port)
    }
}
