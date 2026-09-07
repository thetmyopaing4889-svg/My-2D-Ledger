package org.anticensor.vpn.core.generator

import org.anticensor.vpn.core.parser.VlessConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Generates spec-compliant Xray JSON configuration files with local SOCKS5 loopback
 * inbound at 127.0.0.1:10808 and production XTLS-Reality / XHTTP / gRPC outbound.
 */
object XrayConfigGenerator {

    fun generateJson(
        config: VlessConfig,
        socks5Port: Int = 10808,
        routingMode: org.anticensor.vpn.core.routing.RoutingMode = org.anticensor.vpn.core.routing.RoutingMode.BYPASS_LAN_AND_LOCAL,
        isAdBlockEnabled: Boolean = true
    ): String {
        val root = JSONObject()

        // 1. Log Settings
        root.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // 2. Inbounds: SOCKS5 Loopback for hev-socks5-tunnel
        val inbounds = JSONArray()
        val socksInbound = JSONObject().apply {
            put("tag", "socks-in")
            put("listen", "127.0.0.1")
            put("port", socks5Port)
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
            put("sniffing", JSONObject().apply {
                put("enabled": true, "destOverride": JSONArray(listOf("http", "tls", "quic")))
            })
        }
        inbounds.put(socksInbound)
        root.put("inbounds", inbounds)

        // 3. Outbounds: Target VLESS + Direct Fallback + Blackhole
        val outbounds = JSONArray()

        // Primary Proxy Outbound
        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")

            // VLESS User Settings
            put("settings", JSONObject().apply {
                val vnext = JSONArray()
                val serverObj = JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                    val users = JSONArray()
                    val userObj = JSONObject().apply {
                        put("id", config.uuid)
                        put("encryption", "none")
                        if (config.flow.isNotBlank()) {
                            put("flow", config.flow)
                        }
                    }
                    users.put(userObj)
                    put("users", users)
                }
                vnext.put(serverObj)
                put("vnext", vnext)
            })

            // StreamSettings (Reality / XHTTP / gRPC / WS)
            put("streamSettings", JSONObject().apply {
                put("network", config.transportType)
                put("security", config.security)

                if (config.security.equals("reality", ignoreCase = true)) {
                    put("realitySettings", JSONObject().apply {
                        put("show", false)
                        put("fingerprint", config.fingerprint.ifBlank { "chrome" })
                        put("serverName", config.sni.ifBlank { config.server })
                        put("publicKey", config.publicKey)
                        put("shortId", config.shortId)
                        put("spiderX", config.spiderX.ifBlank { "/" })
                    })
                }

                // Transport Specific Settings
                val net = config.transportType.lowercase()
                when {
                    net == "xhttp" || net == "splithttp" -> {
                        val xhttpObj = JSONObject().apply {
                            put("path", config.path.ifBlank { "/" })
                            put("mode", config.xhttpMode.ifBlank { "auto" })
                            if (config.hostHeader.isNotBlank()) {
                                put("host", config.hostHeader)
                            }
                            // Anti-DPI Traffic Morphing: xPaddingBytes & xmux stream multiplexing
                            val extra = JSONObject().apply {
                                put("xPaddingBytes", config.xPaddingBytes.ifBlank { "100-500" })
                            }
                            put("extra", extra)
                            val xmux = JSONObject().apply {
                                put("maxConcurrency", config.xmuxConcurrency.coerceAtLeast(1))
                                put("cMaxReuseTimes", config.xmuxMaxReuseTimes.coerceAtLeast(1))
                                put("cMaxRequestTimes", config.xmuxMaxRequestTimes.coerceAtLeast(1))
                                put("hMaxRequestTimes", 1024)
                            }
                            put("xmux", xmux)
                        }
                        put("xhttpSettings", xhttpObj)
                        put("splithttpSettings", xhttpObj) // Standardized alias for multi-version compatibility
                    }
                    net == "grpc" -> {
                        put("grpcSettings", JSONObject().apply {
                            put("serviceName", config.serviceName)
                            put("multiMode", true)
                        })
                    }
                    net == "ws" -> {
                        put("wsSettings", JSONObject().apply {
                            put("path", config.path.ifBlank { "/" })
                            if (config.hostHeader.isNotBlank()) {
                                put("headers", JSONObject().apply {
                                    put("Host", config.hostHeader)
                                })
                            }
                        })
                    }
                }

                // Anti-DPI TCP Segment & TLS ClientHello Fragmentation
                val sockopt = JSONObject().apply {
                    put("tcpFastOpen", true)
                    put("tcpKeepAliveInterval", 15)
                }
                put("sockopt", sockopt)
            })
        }
        outbounds.put(proxyOutbound)

        // Fragment Outbound for advanced Anti-DPI packet splitting (100-200 byte segments)
        val fragmentOutbound = JSONObject().apply {
            put("tag", "fragment")
            put("protocol", "freedom")
            put("settings", JSONObject().apply {
                put("fragment", JSONObject().apply {
                    put("packets", "tlshello")
                    put("length", "100-200")
                    put("interval", "10-20")
                })
            })
        }
        outbounds.put(fragmentOutbound)

        // Freedom (Direct) Outbound
        outbounds.put(JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
        })

        // Blackhole (Block Ad/Private IP) Outbound
        outbounds.put(JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
        })

        root.put("outbounds", outbounds)

        // 4. DNS Settings
        root.put("dns", JSONObject().apply {
            put("servers", JSONArray(listOf("1.1.1.1", "8.8.8.8", "localhost")))
        })

        // 5. Routing Rules (Smart Rule Engine)
        root.put("routing", org.anticensor.vpn.core.routing.SmartRuleEngine.generateRoutingRulesJson(routingMode, isAdBlockEnabled))

        return root.toString(2)
    }

    fun writeConfigFile(config: VlessConfig, targetFile: File, socks5Port: Int = 10808): File {
        val json = generateJson(config, socks5Port)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(json)
        return targetFile
    }

    /**
     * Generates Shadowsocks-2022 configuration using Xray-core outbound.
     */
    fun generateShadowsocksJson(config: org.anticensor.vpn.core.parser.ShadowsocksConfig, socks5Port: Int = 10808): String {
        val root = JSONObject()
        root.put("log", JSONObject().apply { put("loglevel", "warning") })

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
                put("tag", "proxy")
                put("protocol", "shadowsocks")
                put("settings", JSONObject().apply {
                    val servers = JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", config.server)
                            put("port", config.port)
                            put("method", config.method)
                            put("password", config.password)
                            put("uot", config.uot)
                        })
                    }
                    put("servers", servers)
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
     * Generates Shadowsocks + Cloak / V2Ray-Plugin configuration with TLS camouflage.
     */
    fun generateSsCloakJson(config: org.anticensor.vpn.core.parser.SsCloakConfig, socks5Port: Int = 10808): String {
        val root = JSONObject()
        root.put("log", JSONObject().apply { put("loglevel", "warning") })

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
                put("tag", "proxy")
                put("protocol", "shadowsocks")
                put("settings", JSONObject().apply {
                    val servers = JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", config.server)
                            put("port", config.port)
                            put("method", config.method)
                            put("password", config.password)
                        })
                    }
                    put("servers", servers)
                })
                put("streamSettings", JSONObject().apply {
                    put("network", "ws")
                    put("security", "tls")
                    put("tlsSettings", JSONObject().apply {
                        put("serverName", config.sni.ifBlank { config.server })
                        put("allowInsecure", false)
                    })
                    put("wsSettings", JSONObject().apply {
                        put("path", "/cloak-ws")
                        put("headers", JSONObject().apply {
                            put("Host", config.sni.ifBlank { config.server })
                        })
                    })
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
