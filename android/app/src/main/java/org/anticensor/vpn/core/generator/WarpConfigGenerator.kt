package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.WarpConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Generates Cloudflare WARP / WireGuard configuration with Clean Anycast IP rotation,
 * reserved bytes client identity (0,0,0), and low-overhead MTU 1280 tuning.
 */
object WarpConfigGenerator {

    /**
     * Known resilient Cloudflare Anycast Clean IP pool for escaping ISP blocks
     */
    val CLEAN_IP_POOL = listOf(
        "162.159.192.1",
        "162.159.193.10",
        "162.159.195.2",
        "188.114.96.1",
        "188.114.97.2",
        "188.114.98.3"
    )

    /**
     * Standard WireGuard / Sing-box / Xray WireGuard format
     */
    fun generateJson(config: WarpConfig, socks5Port: Int = 10808): String {
        val root = JSONObject()

        root.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        val inbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks-in")
                put("listen", "127.0.0.1")
                put("port", socks5Port)
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
            })
        }
        root.put("inbounds", inbounds)

        val outbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "warp-out")
                put("protocol", "wireguard")
                put("settings", JSONObject().apply {
                    put("secretKey", config.privateKey)
                    put("address", JSONArray(listOf(config.clientIpv4, config.clientIpv6)))
                    val peers = JSONArray().apply {
                        put(JSONObject().apply {
                            put("publicKey", config.publicKey)
                            put("endpoint", "${config.server}:${config.port}")
                            put("allowedIPs", JSONArray(listOf("0.0.0.0/0", "::/0")))
                            put("keepAlive", 25)
                        })
                    }
                    put("peers", peers)
                    put("mtu", config.mtu)
                    // Cloudflare Reserved Bytes
                    val reservedArr = JSONArray()
                    config.reservedBytes.forEach { reservedArr.put(it) }
                    put("reserved", reservedArr)
                })
            })
            put(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
            })
        }
        root.put("outbounds", outbounds)

        return root.toString(2)
    }

    /**
     * WireGuard INI config representation for kernel or Go WireGuard backend
     */
    fun generateConf(config: WarpConfig): String {
        val reservedStr = config.reservedBytes.joinToString(", ")
        return """
            [Interface]
            PrivateKey = ${config.privateKey}
            Address = ${config.clientIpv4}, ${config.clientIpv6}
            DNS = 1.1.1.1, 1.0.0.1
            MTU = ${config.mtu}
            # Cloudflare Client Reserved Bytes: [$reservedStr]

            [Peer]
            PublicKey = ${config.publicKey}
            Endpoint = ${config.server}:${config.port}
            AllowedIPs = 0.0.0.0/0, ::/0
            PersistentKeepalive = 25
        """.trimIndent()
    }
}
