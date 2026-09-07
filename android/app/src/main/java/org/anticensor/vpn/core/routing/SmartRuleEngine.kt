package org.anticensor.vpn.core.routing

import org.json.JSONArray
import org.json.JSONObject

/**
 * Smart Rule-Based Traffic Routing Engine.
 * Intelligently separates traffic into:
 * 1. DIRECT: Myanmar domestic domains, banking apps, local telcos, private LAN IPs.
 * 2. PROXY: Censored social platforms, news outlets, streaming, global web.
 * 3. REJECT: Advertising trackers, telemetry, malware domains.
 */
object SmartRuleEngine {

    /**
     * Common Myanmar domestic services & banks that should bypass VPN for fast local speeds
     * and avoid bank fraud security flags.
     */
    val DOMESTIC_MYANMAR_DOMAINS = listOf(
        "regexp:.*\\.mm$",
        "domain:kbzpay.com",
        "domain:kbzbank.com",
        "domain:wavemoney.com.mm",
        "domain:ayabank.com",
        "domain:cbbank.com.mm",
        "domain:yomabank.com",
        "domain:uab.com.mm",
        "domain:mab.com.mm",
        "domain:mpt.com.mm",
        "domain:atom.com.mm",
        "domain:ooredoo.com.mm",
        "domain:mytel.com.mm",
        "domain:myanmarnet.com",
        "domain:frontiir.net",
        "domain:shwepay.com"
    )

    /**
     * Censored news, social media, messaging, and human rights gateways
     */
    val CENSORED_TARGET_DOMAINS = listOf(
        "domain:facebook.com",
        "domain:fbcdn.net",
        "domain:messenger.com",
        "domain:instagram.com",
        "domain:telegram.org",
        "domain:t.me",
        "domain:twitter.com",
        "domain:x.com",
        "domain:youtube.com",
        "domain:googlevideo.com",
        "domain:signal.org",
        "domain:bbc.com",
        "domain:rfa.org",
        "domain:voanews.com",
        "domain:myanmar-now.org",
        "domain:irrawaddy.com",
        "domain:khitthitnews.com",
        "domain:medium.com",
        "domain:reddit.com",
        "domain:wikipedia.org"
    )

    /**
     * Ad trackers and telemetry domains blocked for speed and privacy
     */
    val AD_TRACKER_DOMAINS = listOf(
        "geosite:category-ads-all",
        "domain:doubleclick.net",
        "domain:googlesyndication.com",
        "domain:adservice.google.com",
        "domain:ads.facebook.com"
    )

    val PRIVATE_LAN_IPS = listOf(
        "geoip:private",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8"
    )

    /**
     * Generates Xray/Sing-box compliant JSON routing rule table
     */
    fun generateRoutingRulesJson(routingMode: RoutingMode, isAdBlockEnabled: Boolean = true): JSONObject {
        val routing = JSONObject()
        routing.put("domainStrategy", "IPIfNonMatch")

        val rules = JSONArray()

        // 1. REJECT Rule (AdBlock)
        if (isAdBlockEnabled) {
            rules.put(JSONObject().apply {
                put("type", "field")
                put("outboundTag", "block")
                val domArr = JSONArray()
                AD_TRACKER_DOMAINS.forEach { domArr.put(it) }
                put("domain", domArr)
            })
        }

        when (routingMode) {
            RoutingMode.GLOBAL_PROXY -> {
                // Route all traffic through proxy, except private LAN loopback
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("ip", JSONArray(listOf("127.0.0.0/8")))
                })
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "proxy")
                    put("network", "tcp,udp")
                })
            }
            RoutingMode.BYPASS_LAN_AND_LOCAL -> {
                // 2. Direct Rule: LAN IPs and Myanmar Domestic Domains
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    val ipArr = JSONArray()
                    PRIVATE_LAN_IPS.forEach { ipArr.put(it) }
                    ipArr.put("geoip:mm")
                    put("ip", ipArr)

                    val domArr = JSONArray()
                    DOMESTIC_MYANMAR_DOMAINS.forEach { domArr.put(it) }
                    put("domain", domArr)
                })

                // 3. Proxy Rule: Censored domains & fallback foreign traffic
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "proxy")
                    val domArr = JSONArray()
                    CENSORED_TARGET_DOMAINS.forEach { domArr.put(it) }
                    put("domain", domArr)
                })

                // Fallback default to proxy for uncategorized international sites
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "proxy")
                    put("network", "tcp,udp")
                })
            }
            RoutingMode.SPLIT_TUNNEL_PER_APP -> {
                // App-level routing is handled natively via Android VpnService.Builder package filters
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "proxy")
                    put("network", "tcp,udp")
                })
            }
        }

        routing.put("rules", rules)
        return routing
    }
}
