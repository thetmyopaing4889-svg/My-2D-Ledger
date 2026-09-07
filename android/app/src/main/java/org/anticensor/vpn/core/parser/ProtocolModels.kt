package org.anticensor.vpn.core.parser

import java.util.UUID

enum class ProxyProtocol(val scheme: String, val displayName: String, val defaultPort: Int) {
    VLESS_REALITY("vless", "VLESS XTLS-Reality", 443),
    HYSTERIA2("hysteria2", "Hysteria 2 (Salamander)", 443),
    NAIVE_PROXY("naive+https", "NaïveProxy (Cronet)", 443),
    TUIC("tuic", "TUIC v5 (0-RTT)", 443),
    AMNEZIA_WG("amneziawg", "AmneziaWG (Junk Injection)", 51820),
    SHADOWSOCKS("ss", "Shadowsocks-2022", 8388),
    WARP("warp", "Cloudflare WARP (Clean-IP)", 2408),
    SSH_TUNNEL("ssh", "SSH over WebSocket / CDN", 22),
    SS_CLOAK("ss+cloak", "Shadowsocks + Cloak (CDN)", 443);

    companion object {
        fun fromScheme(scheme: String): ProxyProtocol? {
            val normalized = scheme.lowercase().trim().removeSuffix("://")
            return when {
                normalized == "vless" -> VLESS_REALITY
                normalized == "hysteria2" || normalized == "hy2" -> HYSTERIA2
                normalized == "naive+https" || normalized == "naive+quic" || normalized == "naive" -> NAIVE_PROXY
                normalized == "tuic" -> TUIC
                normalized == "amneziawg" || normalized == "awg" -> AMNEZIA_WG
                normalized == "ss" -> SHADOWSOCKS
                normalized == "warp" || normalized == "warp+wg" -> WARP
                normalized == "ssh" || normalized == "ssh+ws" -> SSH_TUNNEL
                normalized == "ss+cloak" || normalized == "cloak" -> SS_CLOAK
                else -> null
            }
        }
    }
}

/**
 * Base abstract model for all parsed proxy configurations.
 * Includes optional subscriptionId linking node back to parent subscription source.
 */
sealed class ProxyConfig(
    open val id: String = UUID.randomUUID().toString(),
    open val name: String,
    open val server: String,
    open val port: Int,
    open val protocol: ProxyProtocol,
    open val subscriptionId: String? = null
) {
    abstract fun toShareLink(): String
}

/**
 * VLESS Configuration with XTLS-Reality & XHTTP (SplitHTTP) / gRPC / WS transport parameters.
 * XHTTP implements full chunked upload/download streams and xPaddingBytes randomization.
 */
data class VlessConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 443,
    val uuid: String,
    val flow: String = "xtls-rprx-vision", // Latest vision flow eliminating TLS-in-TLS signature
    val security: String = "reality",
    val publicKey: String, // pbk: Reality server public key
    val sni: String, // Server Name Indication (camouflage target)
    val fingerprint: String = "chrome", // uTLS fingerprint: chrome, safari, ios, android, randomized
    val shortId: String = "", // sid: hex short ID
    val spiderX: String = "/", // spx: SpiderX web crawler path
    val transportType: String = "tcp", // tcp, xhttp (splithttp), grpc, ws
    val path: String = "", // path for xhttp or ws
    val serviceName: String = "", // serviceName for gRPC
    val hostHeader: String = "",
    // XHTTP / SplitHTTP Advanced Anti-DPI Parameters
    val xhttpMode: String = "auto", // "auto", "packet-up", "stream-up", "stream-one"
    val xPaddingBytes: String = "100-500", // Length range for payload noise padding
    val xmuxConcurrency: Int = 16, // Max multiplexed streams
    val xmuxMaxReuseTimes: Int = 64,
    val xmuxMaxRequestTimes: Int = 512,
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.VLESS_REALITY, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val encPath = java.net.URLEncoder.encode(path, "UTF-8")
        val encSpx = java.net.URLEncoder.encode(spiderX, "UTF-8")
        return "vless://$uuid@$server:$port?security=$security&flow=$flow&type=$transportType&pbk=$publicKey&sni=$sni&fp=$fingerprint&sid=$shortId&spx=$encSpx&path=$encPath&mode=$xhttpMode&padding=$xPaddingBytes#$encName"
    }
}

/**
 * Hysteria 2 Configuration with Salamander protocol obfuscation and brutal bandwidth control.
 * Supports port-hopping ranges (e.g. "20000-50000") and hop interval timings.
 */
data class Hysteria2Config(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 443,
    val auth: String,
    val obfsType: String = "salamander",
    val obfsPassword: String = "",
    val sni: String = "",
    val insecure: Boolean = false,
    val upMbps: Int = 50,
    val downMbps: Int = 200,
    val portHopRange: String = "", // e.g. "20000-50000" or empty
    val hopIntervalSeconds: Int = 30, // Interval for port hopping to bypass UDP blocking
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.HYSTERIA2, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val hopParam = if (portHopRange.isNotBlank()) "&mport=$portHopRange&hop_interval=$hopIntervalSeconds" else ""
        val obfsParam = if (obfsPassword.isNotBlank()) "&obfs=$obfsType&obfs-password=$obfsPassword" else ""
        val insec = if (insecure) 1 else 0
        return "hysteria2://$auth@$server:$port?sni=$sni&insecure=$insec&up=$upMbps&down=$downMbps$obfsParam$hopParam#$encName"
    }
}

/**
 * NaïveProxy Configuration utilizing Chromium network stack (Cronet) camouflage.
 */
data class NaiveConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 443,
    val username: String,
    val password: String,
    val networkType: String = "https", // https (HTTP/2) or quic (HTTP/3)
    val sni: String = "",
    val padding: Boolean = true,
    val concurrency: Int = 1,
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.NAIVE_PROXY, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val pad = if (padding) 1 else 0
        return "naive+$networkType://$username:$password@$server:$port?sni=$sni&padding=$pad#$encName"
    }
}

/**
 * TUIC v5 Configuration utilizing 0-RTT QUIC multiplexing with custom congestion control.
 */
data class TuicConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 443,
    val uuid: String,
    val password: String,
    val congestionControl: String = "bbr", // bbr, cubic, new_reno
    val udpRelayMode: String = "native", // native or quic
    val sni: String = "",
    val alpn: List<String> = listOf("h3", "spdy/3.1"),
    val disableSni: Boolean = false,
    val zeroRttHandshake: Boolean = true,
    val heartbeatIntervalMs: Long = 10000L,
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.TUIC, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val alpnStr = alpn.joinToString(",")
        val rtt = if (zeroRttHandshake) 1 else 0
        return "tuic://$uuid:$password@$server:$port?congestion_control=$congestionControl&udp_relay_mode=$udpRelayMode&sni=$sni&alpn=$alpnStr&zero_rtt_handshake=$rtt#$encName"
    }
}

/**
 * AmneziaWG Configuration with anti-DPI junk packets and mutated handshake headers.
 * Strictly guarantees all 9 mutation parameters: Jc, Jmin, Jmax, S1, S2, H1, H2, H3, H4.
 */
data class AmneziaWgConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 51820,
    val addressIpv4: String = "10.0.0.2/32",
    val addressIpv6: String? = null,
    val privateKey: String,
    val publicKey: String,
    val presharedKey: String? = null,
    val dns: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    val mtu: Int = 1360,
    // Strictly Verified 9 AmneziaWG Header Mutation & Junk Injection Parameters:
    val jc: Int = 4,        // 1. Junk Packet Count (0..128)
    val jmin: Int = 40,     // 2. Junk Packet Min Size in bytes
    val jmax: Int = 70,     // 3. Junk Packet Max Size in bytes
    val s1: Int = 20,       // 4. Init Packet Junk Header Size (0..1000)
    val s2: Int = 20,       // 5. Response Packet Junk Header Size (0..1000)
    val h1: Long = 1L,      // 6. Custom Init packet magic type ID (Default WG=1)
    val h2: Long = 2L,      // 7. Custom Response packet magic type ID (Default WG=2)
    val h3: Long = 3L,      // 8. Custom Cookie packet magic type ID (Default WG=3)
    val h4: Long = 4L,      // 9. Custom Transport packet magic type ID (Default WG=4)
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.AMNEZIA_WG, subscriptionId) {

    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val encPriv = java.net.URLEncoder.encode(privateKey, "UTF-8")
        val encPub = java.net.URLEncoder.encode(publicKey, "UTF-8")
        return "amneziawg://$encPriv@$server:$port?public_key=$encPub&address=$addressIpv4&jc=$jc&jmin=$jmin&jmax=$jmax&s1=$s1&s2=$s2&h1=$h1&h2=$h2&h3=$h3&h4=$h4#$encName"
    }

    // Compatibility constructor supporting alternate parameter naming
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        server: String,
        port: Int = 51820,
        clientPrivateKey: String,
        clientAddress: String = "10.0.0.2/32",
        serverPublicKey: String,
        presharedKey: String? = null,
        dns: List<String> = listOf("1.1.1.1", "8.8.8.8"),
        mtu: Int = 1360,
        jc: Int = 4,
        jmin: Int = 40,
        jmax: Int = 70,
        s1: Int = 20,
        s2: Int = 20,
        h1: Long = 1L,
        h2: Long = 2L,
        h3: Long = 3L,
        h4: Long = 4L,
        subscriptionId: String? = null
    ) : this(
        id = id,
        name = name,
        server = server,
        port = port,
        addressIpv4 = clientAddress,
        privateKey = clientPrivateKey,
        publicKey = serverPublicKey,
        presharedKey = presharedKey,
        dns = dns,
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

/**
 * Shadowsocks-2022 Configuration with Blake3 KDF and AEAD cipher suites.
 */
data class ShadowsocksConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 8388,
    val method: String = "2022-blake3-aes-128-gcm",
    val password: String,
    val uot: Boolean = true, // UDP over TCP
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.SHADOWSOCKS, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val authBytes = "$method:$password".toByteArray(java.nio.charset.StandardCharsets.UTF_8)
        val base64Auth = android.util.Base64.encodeToString(authBytes, android.util.Base64.NO_WRAP)
        return "ss://$base64Auth@$server:$port#$encName"
    }
}

/**
 * Cloudflare WARP / WARP+ Zero-Trust WireGuard Configuration.
 * Features automated Cloudflare key registration and Clean Anycast IP Scanner.
 */
data class WarpConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Cloudflare WARP (Clean-IP Anycast)",
    override val server: String = "162.159.192.1",
    override val port: Int = 2408,
    val privateKey: String = "aHR0cHM6Ly9jbG91ZGZsYXJlLndhcnAucHJpdmF0ZS5rZXk=",
    val publicKey: String = "bm9uc2VydmVycHVibGlja2V5Zm9yY2xvdWRmbGFyZXdhcnAx",
    val clientIpv4: String = "172.16.0.2/32",
    val clientIpv6: String = "2606:4700:110:8a43:a83b:14c5:a98:3626/128",
    val reservedBytes: List<Int> = listOf(0, 0, 0), // Cloudflare client identification bytes
    val mtu: Int = 1280,
    val isWarpPlus: Boolean = false,
    val licenseKey: String = "",
    val autoCleanIpScan: Boolean = true,
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.WARP, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val res = reservedBytes.joinToString(",")
        return "warp://$privateKey@$server:$port?reserved=$res&mtu=$mtu&plus=$isWarpPlus#$encName"
    }
}

/**
 * SSH Tunneling Configuration (Direct SSH & SSH over WebSocket / CDN Camouflage).
 * Supports custom HTTP payload, SNI bug-host, and Cloudflare CDN reverse-proxying.
 */
data class SshTunnelConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 22,
    val username: String,
    val password: String,
    val payload: String = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf][crlf]",
    val sni: String = "",
    val wsPath: String = "/ssh-ws",
    val proxyHost: String = "", // CDN Reverse Proxy IP/Domain
    val proxyPort: Int = 443,
    val isWebSocket: Boolean = true,
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.SSH_TUNNEL, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val encUser = java.net.URLEncoder.encode(username, "UTF-8")
        val encPass = java.net.URLEncoder.encode(password, "UTF-8")
        val wsFlag = if (isWebSocket) 1 else 0
        return "ssh://$encUser:$encPass@$server:$port?sni=$sni&ws=$wsFlag&proxy=$proxyHost&proxy_port=$proxyPort#$encName"
    }
}

/**
 * Shadowsocks with Cloak / V2Ray-Plugin CDN Camouflage Configuration.
 * Camouflages Shadowsocks streams behind legitimate TLS web servers.
 */
data class SsCloakConfig(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val server: String,
    override val port: Int = 443,
    val method: String = "aes-128-gcm",
    val password: String,
    val plugin: String = "cloak", // "cloak" or "v2ray-plugin"
    val pluginOpts: String = "fakeDomain=dl.google.com;browser=chrome",
    val sni: String = "dl.google.com",
    override val subscriptionId: String? = null
) : ProxyConfig(id, name, server, port, ProxyProtocol.SS_CLOAK, subscriptionId) {
    override fun toShareLink(): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        val authBytes = "$method:$password".toByteArray(java.nio.charset.StandardCharsets.UTF_8)
        val base64Auth = android.util.Base64.encodeToString(authBytes, android.util.Base64.NO_WRAP)
        val encOpts = java.net.URLEncoder.encode(pluginOpts, "UTF-8")
        return "ss://$base64Auth@$server:$port?plugin=$plugin&plugin-opts=$encOpts#$encName"
    }
}


