package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.SshTunnelConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates configuration for SSH Tunneling with Direct SSH or SSH-over-WebSocket CDN camouflage.
 * Implements local SOCKS5 dynamic forwarder (-D 10808) with optional HTTP Upgrade ProxyCommand.
 */
object SshConfigGenerator {

    fun generateShellScript(config: SshTunnelConfig, socks5Port: Int = 10808): String {
        val proxyCmd = if (config.isWebSocket && config.proxyHost.isNotBlank()) {
            val sniTarget = config.sni.ifBlank { config.server }
            "-o \"ProxyCommand=nc -X connect -x ${config.proxyHost}:${config.proxyPort} %h %p\""
        } else ""

        return """
            #!/bin/sh
            # AegisVPN SSH Tunnel Launcher
            # Target: ${config.server}:${config.port}
            # SOCKS5 Dynamic Forward: 127.0.0.1:$socks5Port

            export SSHPASS="${config.password}"
            sshpass -e ssh -N -D 127.0.0.1:$socks5Port \
                -p ${config.port} \
                -o StrictHostKeyChecking=no \
                -o UserKnownHostsFile=/dev/null \
                -o ServerAliveInterval=15 \
                -o ServerAliveCountMax=3 \
                $proxyCmd \
                ${config.username}@${config.server}
        """.trimIndent()
    }

    /**
     * Sing-box / Xray compatible SSH client configuration block
     */
    fun generateJson(config: SshTunnelConfig, socks5Port: Int = 10808): String {
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
                put("tag", "ssh-out")
                put("protocol", "ssh")
                put("settings", JSONObject().apply {
                    put("server", config.server)
                    put("port", config.port)
                    put("user", config.username)
                    put("password", config.password)
                    if (config.isWebSocket && config.proxyHost.isNotBlank()) {
                        put("proxy", JSONObject().apply {
                            put("host", config.proxyHost)
                            put("port", config.proxyPort)
                            put("sni", config.sni.ifBlank { config.server })
                            put("path", config.wsPath)
                        })
                    }
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
}
