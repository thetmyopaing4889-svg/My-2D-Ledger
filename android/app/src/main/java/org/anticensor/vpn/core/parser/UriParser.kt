package org.anticensor.vpn.core.parser

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object UriParser {

    /**
     * Top-level entry point to parse any supported link or config snippet.
     * Optionally associates the parsed node with a parent subscription ID.
     * Throws IllegalArgumentException if the format is invalid or unsupported.
     */
    fun parse(rawInput: String, subscriptionId: String? = null): ProxyConfig {
        val trimmed = rawInput.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed, subscriptionId)
            trimmed.startsWith("hysteria2://", ignoreCase = true) ||
            trimmed.startsWith("hy2://", ignoreCase = true) -> parseHysteria2(trimmed, subscriptionId)
            trimmed.startsWith("naive+https://", ignoreCase = true) ||
            trimmed.startsWith("naive+quic://", ignoreCase = true) -> parseNaive(trimmed, subscriptionId)
            trimmed.startsWith("tuic://", ignoreCase = true) -> parseTuic(trimmed, subscriptionId)
            trimmed.startsWith("amneziawg://", ignoreCase = true) ||
            trimmed.startsWith("awg://", ignoreCase = true) -> parseAmneziaUri(trimmed, subscriptionId)
            trimmed.startsWith("warp://", ignoreCase = true) -> parseWarp(trimmed, subscriptionId)
            trimmed.startsWith("ssh://", ignoreCase = true) ||
            trimmed.startsWith("ssh+ws://", ignoreCase = true) -> parseSsh(trimmed, subscriptionId)
            trimmed.startsWith("ss://", ignoreCase = true) ||
            trimmed.startsWith("ss+cloak://", ignoreCase = true) -> parseShadowsocksOrCloak(trimmed, subscriptionId)
            trimmed.contains("[Interface]", ignoreCase = true) &&
            trimmed.contains("[Peer]", ignoreCase = true) -> parseWireGuardConf(trimmed, subscriptionId = subscriptionId)
            else -> throw IllegalArgumentException("Unsupported protocol scheme or configuration format")
        }
    }

    /**
     * Parses standard VLESS links with Reality / XHTTP (SplitHTTP) / gRPC / WS parameters.
     * Example: vless://uuid@domain.com:443?security=reality&pbk=xxx&type=xhttp&flow=xtls-rprx-vision&sni=dl.google.com&fp=chrome&sid=1a2b&spx=%2F&mode=auto#RealityNode
     */
    fun parseVless(uriString: String, subscriptionId: String? = null): VlessConfig {
        val uri = Uri.parse(uriString)
        val userInfo = uri.userInfo ?: uri.authority?.substringBefore("@")
            ?: throw IllegalArgumentException("Missing UUID in VLESS URI")

        val uuid = userInfo.trim()
        val host = uri.host ?: throw IllegalArgumentException("Missing host in VLESS URI")
        val port = if (uri.port > 0) uri.port else 443

        val queryParams = extractQueryParams(uriString)
        val name = decodeFragment(uri.fragment, "VLESS Reality ($host)")
        val transportType = (queryParams["type"] ?: "tcp").lowercase()

        return VlessConfig(
            name = name,
            server = host,
            port = port,
            uuid = uuid,
            flow = queryParams["flow"] ?: "xtls-rprx-vision",
            security = queryParams["security"] ?: "reality",
            publicKey = queryParams["pbk"] ?: queryParams["publicKey"] ?: "",
            sni = queryParams["sni"] ?: queryParams["peer"] ?: host,
            fingerprint = queryParams["fp"] ?: "chrome",
            shortId = queryParams["sid"] ?: "",
            spiderX = queryParams["spx"] ?: queryParams["spiderx"] ?: "/",
            transportType = if (transportType == "splithttp") "xhttp" else transportType,
            path = queryParams["path"] ?: "",
            serviceName = queryParams["serviceName"] ?: queryParams["service_name"] ?: "",
            hostHeader = queryParams["host"] ?: "",
            xhttpMode = queryParams["mode"] ?: queryParams["xhttp_mode"] ?: "auto",
            xPaddingBytes = queryParams["padding"] ?: queryParams["xpadding"] ?: "100-500",
            subscriptionId = subscriptionId
        )
    }

    /**
     * Parses Hysteria 2 / Hy2 URIs with Salamander obfuscation & port-hopping parameters.
     * Example: hysteria2://secret_auth@server.net:443?insecure=0&sni=bing.com&obfs=salamander&obfs-password=MyObfsPass&mport=20000-50000#Hy2Node
     */
    fun parseHysteria2(uriString: String, subscriptionId: String? = null): Hysteria2Config {
        // Standardize hy2:// to hysteria2:// for android.net.Uri compatibility
        val standardized = if (uriString.startsWith("hy2://", ignoreCase = true)) {
            "hysteria2://" + uriString.substring(6)
        } else {
            uriString
        }

        val uri = Uri.parse(standardized)
        val auth = uri.userInfo ?: uri.authority?.substringBefore("@")
            ?: throw IllegalArgumentException("Missing auth password in Hysteria 2 URI")

        val host = uri.host ?: throw IllegalArgumentException("Missing host in Hysteria 2 URI")
        val port = if (uri.port > 0) uri.port else 443
        val queryParams = extractQueryParams(standardized)
        val name = decodeFragment(uri.fragment, "Hysteria 2 ($host)")

        val insecure = queryParams["insecure"]?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: false
        val upMbps = queryParams["upmbps"]?.toIntOrNull() ?: queryParams["up"]?.toIntOrNull() ?: 50
        val downMbps = queryParams["downmbps"]?.toIntOrNull() ?: queryParams["down"]?.toIntOrNull() ?: 200
        val portHop = queryParams["mport"] ?: queryParams["ports"] ?: queryParams["hop"] ?: ""
        val hopInterval = queryParams["hop_interval"]?.toIntOrNull() ?: queryParams["interval"]?.toIntOrNull() ?: 30

        return Hysteria2Config(
            name = name,
            server = host,
            port = port,
            auth = auth,
            obfsType = queryParams["obfs"] ?: "salamander",
            obfsPassword = queryParams["obfs-password"] ?: queryParams["obfs_password"] ?: "",
            sni = queryParams["sni"] ?: host,
            insecure = insecure,
            upMbps = upMbps,
            downMbps = downMbps,
            portHopRange = portHop,
            hopIntervalSeconds = hopInterval,
            subscriptionId = subscriptionId
        )
    }

    /**
     * Parses NaïveProxy URIs (naive+https:// or naive+quic://).
     * Example: naive+https://user1:SecretPass@proxy.example.com:443?sni=proxy.example.com&padding=1#NaiveNode
     */
    fun parseNaive(uriString: String, subscriptionId: String? = null): NaiveConfig {
        val isQuic = uriString.startsWith("naive+quic://", ignoreCase = true)
        val normalized = uriString.replaceFirst("naive+https://", "https://", ignoreCase = true)
            .replaceFirst("naive+quic://", "https://", ignoreCase = true)

        val uri = Uri.parse(normalized)
        val userInfo = uri.userInfo ?: throw IllegalArgumentException("Missing username/password credentials in NaïveProxy URI")
        val parts = userInfo.split(":")
        val username = parts.getOrNull(0) ?: throw IllegalArgumentException("Missing Naive username")
        val password = parts.getOrNull(1) ?: ""

        val host = uri.host ?: throw IllegalArgumentException("Missing host in Naive URI")
        val port = if (uri.port > 0) uri.port else 443
        val queryParams = extractQueryParams(uriString)
        val name = decodeFragment(uri.fragment, "NaïveProxy ($host)")

        val padding = queryParams["padding"]?.let { it != "0" && !it.equals("false", ignoreCase = true) } ?: true

        return NaiveConfig(
            name = name,
            server = host,
            port = port,
            username = username,
            password = password,
            networkType = if (isQuic) "quic" else "https",
            sni = queryParams["sni"] ?: host,
            padding = padding,
            subscriptionId = subscriptionId
        )
    }

    /**
     * Parses TUIC v5 URIs.
     * Example: tuic://uuid:password@tuic.server.com:443?congestion_control=bbr&udp_relay_mode=native&sni=tuic.server.com&alpn=h3#TuicNode
     */
    fun parseTuic(uriString: String, subscriptionId: String? = null): TuicConfig {
        val uri = Uri.parse(uriString)
        val userInfo = uri.userInfo ?: throw IllegalArgumentException("Missing UUID/password in TUIC URI")
        val parts = userInfo.split(":")
        val uuid = parts.getOrNull(0) ?: throw IllegalArgumentException("Missing TUIC UUID")
        val password = parts.getOrNull(1) ?: ""

        val host = uri.host ?: throw IllegalArgumentException("Missing host in TUIC URI")
        val port = if (uri.port > 0) uri.port else 443
        val queryParams = extractQueryParams(uriString)
        val name = decodeFragment(uri.fragment, "TUIC v5 ($host)")

        val alpnRaw = queryParams["alpn"] ?: "h3,spdy/3.1"
        val alpnList = alpnRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return TuicConfig(
            name = name,
            server = host,
            port = port,
            uuid = uuid,
            password = password,
            congestionControl = queryParams["congestion_control"] ?: "bbr",
            udpRelayMode = queryParams["udp_relay_mode"] ?: "native",
            sni = queryParams["sni"] ?: host,
            alpn = if (alpnList.isEmpty()) listOf("h3") else alpnList,
            disableSni = queryParams["disable_sni"] == "1" || queryParams["disable_sni"] == "true",
            zeroRttHandshake = queryParams["zero_rtt_handshake"] != "0",
            subscriptionId = subscriptionId
        )
    }

    /**
     * Parses AmneziaWG URIs (amneziawg:// or awg://).
     * Example: amneziawg://aKey=@awg.host.com:51820?public_key=bKey=&address=10.0.0.2/32&jc=4&jmin=40&jmax=70&s1=20&s2=20&h1=1&h2=2&h3=3&h4=4#AWGNode
     */
    fun parseAmneziaUri(uriString: String, subscriptionId: String? = null): AmneziaWgConfig {
        val normalized = if (uriString.startsWith("awg://", ignoreCase = true)) {
            "amneziawg://" + uriString.substring(6)
        } else {
            uriString
        }

        val uri = Uri.parse(normalized)
        val privateKey = uri.userInfo ?: uri.authority?.substringBefore("@")
            ?: throw IllegalArgumentException("Missing privateKey in AmneziaWG URI")

        val host = uri.host ?: throw IllegalArgumentException("Missing host in AmneziaWG URI")
        val port = if (uri.port > 0) uri.port else 51820
        val queryParams = extractQueryParams(normalized)
        val name = decodeFragment(uri.fragment, "AmneziaWG ($host)")

        return AmneziaWgConfig(
            name = name,
            server = host,
            port = port,
            addressIpv4 = queryParams["address"] ?: "10.0.0.2/32",
            addressIpv6 = queryParams["address6"],
            privateKey = URLDecoder.decode(privateKey, StandardCharsets.UTF_8.name()),
            publicKey = queryParams["public_key"]?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: "",
            presharedKey = queryParams["preshared_key"]?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) },
            jc = queryParams["jc"]?.toIntOrNull() ?: 4,
            jmin = queryParams["jmin"]?.toIntOrNull() ?: 40,
            jmax = queryParams["jmax"]?.toIntOrNull() ?: 70,
            s1 = queryParams["s1"]?.toIntOrNull() ?: 20,
            s2 = queryParams["s2"]?.toIntOrNull() ?: 20,
            h1 = queryParams["h1"]?.toLongOrNull() ?: 1L,
            h2 = queryParams["h2"]?.toLongOrNull() ?: 2L,
            h3 = queryParams["h3"]?.toLongOrNull() ?: 3L,
            h4 = queryParams["h4"]?.toLongOrNull() ?: 4L,
            subscriptionId = subscriptionId
        )
    }

    /**
     * Parses Shadowsocks or Shadowsocks + Cloak / V2Ray-Plugin URIs.
     */
    fun parseShadowsocksOrCloak(uriString: String, subscriptionId: String? = null): ProxyConfig {
        val queryParams = extractQueryParams(uriString)
        val fragmentIdx = uriString.indexOf('#')
        val rawFragment = if (fragmentIdx >= 0) uriString.substring(fragmentIdx + 1) else null
        val cleanUri = if (fragmentIdx >= 0) uriString.substring(0, fragmentIdx) else uriString
        val questionIdx = cleanUri.indexOf('?')
        val bodyAndScheme = if (questionIdx >= 0) cleanUri.substring(0, questionIdx) else cleanUri
        val body = bodyAndScheme.substringAfter("://")

        val (method, password, host, port) = if (body.contains("@")) {
            val parts = body.split("@", limit = 2)
            val decodedAuth = tryDecodeBase64(parts[0])
            val (m, p) = if (decodedAuth.contains(":")) decodedAuth.split(":", limit = 2) else listOf("2022-blake3-aes-128-gcm", decodedAuth)
            val (h, pt) = parseHostPort(parts[1], 8388)
            listOf(m, p, h, pt.toString())
        } else {
            val decoded = tryDecodeBase64(body)
            if (decoded.contains("@")) {
                val parts = decoded.split("@", limit = 2)
                val (m, p) = parts[0].split(":", limit = 2)
                val (h, pt) = parseHostPort(parts[1], 8388)
                listOf(m, p, h, pt.toString())
            } else {
                throw IllegalArgumentException("Invalid Shadowsocks URI payload")
            }
        }

        val plugin = queryParams["plugin"]?.lowercase() ?: ""
        val isCloak = uriString.startsWith("ss+cloak://", ignoreCase = true) || plugin.contains("cloak") || plugin.contains("v2ray-plugin")

        return if (isCloak) {
            val name = decodeFragment(rawFragment, "SS+Cloak CDN ($host)")
            SsCloakConfig(
                name = name,
                server = host,
                port = port.toIntOrNull() ?: 443,
                method = method,
                password = password,
                plugin = if (plugin.contains("v2ray-plugin")) "v2ray-plugin" else "cloak",
                pluginOpts = queryParams["plugin-opts"] ?: "fakeDomain=dl.google.com;browser=chrome",
                sni = queryParams["sni"] ?: queryParams["peer"] ?: "dl.google.com",
                subscriptionId = subscriptionId
            )
        } else {
            val name = decodeFragment(rawFragment, "Shadowsocks ($host)")
            ShadowsocksConfig(
                name = name,
                server = host,
                port = port.toIntOrNull() ?: 8388,
                method = method,
                password = password,
                subscriptionId = subscriptionId
            )
        }
    }

    fun parseShadowsocks(uriString: String, subscriptionId: String? = null): ShadowsocksConfig {
        val cfg = parseShadowsocksOrCloak(uriString, subscriptionId)
        return if (cfg is ShadowsocksConfig) cfg else {
            ShadowsocksConfig(
                name = cfg.name,
                server = cfg.server,
                port = cfg.port,
                password = (cfg as? SsCloakConfig)?.password ?: "pass",
                subscriptionId = subscriptionId
            )
        }
    }

    /**
     * Parses Cloudflare WARP URIs (warp://...).
     * Example: warp://[privateKey]@162.159.192.1:2408?reserved=0,0,0&mtu=1280&plus=false#WARP-Node
     */
    fun parseWarp(uriString: String, subscriptionId: String? = null): WarpConfig {
        val uri = Uri.parse(uriString)
        val privateKey = uri.userInfo ?: uri.authority?.substringBefore("@") ?: "aHR0cHM6Ly9jbG91ZGZsYXJlLndhcnAucHJpdmF0ZS5rZXk="
        val host = uri.host ?: "162.159.192.1"
        val port = if (uri.port > 0) uri.port else 2408
        val queryParams = extractQueryParams(uriString)
        val name = decodeFragment(uri.fragment, "Cloudflare WARP (Clean-IP)")

        val reservedList = queryParams["reserved"]?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            ?: listOf(0, 0, 0)
        val mtu = queryParams["mtu"]?.toIntOrNull() ?: 1280
        val isPlus = queryParams["plus"]?.toBoolean() ?: false

        return WarpConfig(
            name = name,
            server = host,
            port = port,
            privateKey = privateKey,
            reservedBytes = reservedList,
            mtu = mtu,
            isWarpPlus = isPlus,
            subscriptionId = subscriptionId
        )
    }

    /**
     * Parses SSH Tunnel & SSH-over-WebSocket URIs (ssh://...).
     * Example: ssh://user:pass@vps.example.com:22?sni=sni.cloudflare.com&ws=1&proxy=104.16.1.1&proxy_port=443#SSH-CDN
     */
    fun parseSsh(uriString: String, subscriptionId: String? = null): SshTunnelConfig {
        val uri = Uri.parse(uriString)
        val userInfo = uri.userInfo ?: ""
        val (username, password) = if (userInfo.contains(":")) {
            userInfo.split(":", limit = 2)
        } else {
            listOf(userInfo.ifBlank { "root" }, "")
        }
        val host = uri.host ?: throw IllegalArgumentException("Missing SSH host in URI")
        val port = if (uri.port > 0) uri.port else 22
        val queryParams = extractQueryParams(uriString)
        val name = decodeFragment(uri.fragment, "SSH Tunnel ($host)")

        val isWs = queryParams["ws"]?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: true
        val proxyHost = queryParams["proxy"] ?: ""
        val proxyPort = queryParams["proxy_port"]?.toIntOrNull() ?: 443
        val sni = queryParams["sni"] ?: ""

        return SshTunnelConfig(
            name = name,
            server = host,
            port = port,
            username = username,
            password = password,
            sni = sni,
            proxyHost = proxyHost,
            proxyPort = proxyPort,
            isWebSocket = isWs,
            subscriptionId = subscriptionId
        )
    }

    private fun tryDecodeBase64(input: String): String {
        return try {
            val clean = input.replace('-', '+').replace('_', '/')
            val padded = when (clean.length % 4) {
                2 -> "$clean=="
                3 -> "$clean="
                else -> clean
            }
            String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            input
        }
    }

    /**
     * Parses standard WireGuard & AmneziaWG INI-style .conf configuration text.
     */
    fun parseWireGuardConf(
        confText: String,
        defaultName: String = "AmneziaWG Conf",
        subscriptionId: String? = null
    ): AmneziaWgConfig {
        var currentSection = ""
        val interfaceMap = mutableMapOf<String, String>()
        val peerMap = mutableMapOf<String, String>()

        confText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).trim().lowercase()
                return@forEach
            }

            val eqIdx = line.indexOf('=')
            if (eqIdx > 0) {
                val key = line.substring(0, eqIdx).trim().lowercase()
                val value = line.substring(eqIdx + 1).trim()
                if (currentSection == "interface") {
                    interfaceMap[key] = value
                } else if (currentSection == "peer") {
                    peerMap[key] = value
                }
            }
        }

        val endpoint = peerMap["endpoint"] ?: throw IllegalArgumentException("Missing Endpoint in [Peer] section")
        val (host, port) = parseHostPort(endpoint, 51820)
        val privateKey = interfaceMap["privatekey"] ?: throw IllegalArgumentException("Missing PrivateKey in [Interface] section")
        val publicKey = peerMap["publickey"] ?: throw IllegalArgumentException("Missing PublicKey in [Peer] section")

        val addresses = (interfaceMap["address"] ?: "10.0.0.2/32").split(",").map { it.trim() }
        val ipv4 = addresses.firstOrNull { !it.contains(":") } ?: "10.0.0.2/32"
        val ipv6 = addresses.firstOrNull { it.contains(":") }

        val dnsList = interfaceMap["dns"]?.split(",")?.map { it.trim() } ?: listOf("1.1.1.1", "8.8.8.8")
        val mtu = interfaceMap["mtu"]?.toIntOrNull() ?: 1360

        // Custom AmneziaWG parameters
        val jc = (interfaceMap["jc"] ?: peerMap["jc"])?.toIntOrNull() ?: 4
        val jmin = (interfaceMap["jmin"] ?: peerMap["jmin"])?.toIntOrNull() ?: 40
        val jmax = (interfaceMap["jmax"] ?: peerMap["jmax"])?.toIntOrNull() ?: 70
        val s1 = (interfaceMap["s1"] ?: peerMap["s1"])?.toIntOrNull() ?: 20
        val s2 = (interfaceMap["s2"] ?: peerMap["s2"])?.toIntOrNull() ?: 20
        val h1 = (interfaceMap["h1"] ?: peerMap["h1"])?.toLongOrNull() ?: 1L
        val h2 = (interfaceMap["h2"] ?: peerMap["h2"])?.toLongOrNull() ?: 2L
        val h3 = (interfaceMap["h3"] ?: peerMap["h3"])?.toLongOrNull() ?: 3L
        val h4 = (interfaceMap["h4"] ?: peerMap["h4"])?.toLongOrNull() ?: 4L

        return AmneziaWgConfig(
            name = defaultName,
            server = host,
            port = port,
            addressIpv4 = ipv4,
            addressIpv6 = ipv6,
            privateKey = privateKey,
            publicKey = publicKey,
            presharedKey = peerMap["presharedkey"],
            dns = dnsList,
            mtu = mtu,
            jc = jc,
            jmin = jmin,
            jmax = jmax,
            s1 = s1,
            s2 = s2,
            h1 = h1,
            h2 = h2,
            h3 = h3,
            h4 = h4,
            subscriptionId = subscriptionId
        )
    }

    private fun parseHostPort(endpoint: String, defaultPort: Int): Pair<String, Int> {
        val trimmed = endpoint.trim()
        if (trimmed.startsWith("[")) {
            val endBracket = trimmed.indexOf(']')
            if (endBracket > 0) {
                val host = trimmed.substring(1, endBracket)
                val portStr = trimmed.substring(endBracket + 1).removePrefix(":")
                val port = portStr.toIntOrNull() ?: defaultPort
                return Pair(host, port)
            }
        }
        val colonIdx = trimmed.lastIndexOf(':')
        return if (colonIdx > 0) {
            val host = trimmed.substring(0, colonIdx)
            val port = trimmed.substring(colonIdx + 1).toIntOrNull() ?: defaultPort
            Pair(host, port)
        } else {
            Pair(trimmed, defaultPort)
        }
    }

    private fun extractQueryParams(urlString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val queryStart = urlString.indexOf('?')
        if (queryStart < 0) return map

        val fragmentStart = urlString.indexOf('#', queryStart)
        val queryString = if (fragmentStart >= 0) {
            urlString.substring(queryStart + 1, fragmentStart)
        } else {
            urlString.substring(queryStart + 1)
        }

        queryString.split("&").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = pair.substring(0, idx).trim()
                val rawVal = pair.substring(idx + 1).trim()
                val decodedVal = try {
                    URLDecoder.decode(rawVal, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    rawVal
                }
                map[key] = decodedVal
            }
        }
        return map
    }

    private fun decodeFragment(fragment: String?, defaultVal: String): String {
        if (fragment.isNullOrBlank()) return defaultVal
        return try {
            URLDecoder.decode(fragment, StandardCharsets.UTF_8.name()).trim()
        } catch (_: Exception) {
            fragment.trim()
        }
    }
}
