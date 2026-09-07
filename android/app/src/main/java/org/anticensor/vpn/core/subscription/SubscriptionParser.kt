package org.anticensor.vpn.core.subscription

import android.util.Base64
import android.util.Log
import org.anticensor.vpn.core.parser.ProxyConfig
import org.anticensor.vpn.core.parser.UriParser
import java.nio.charset.StandardCharsets

data class SubscriptionParseResult(
    val nodes: List<ProxyConfig>,
    val parseErrors: List<String> = emptyList(),
    val rawNodeCount: Int = 0
)

/**
 * Universal Subscription Decoding and Link Extraction Engine.
 * Supports:
 * 1. Standard Base64 encoded subscription bundles (RFC 4648 §4)
 * 2. URL-Safe Base64 encoded bundles with/without padding (RFC 4648 §5)
 * 3. Plain text multi-line URI bundles (UNIX \n, Windows \r\n)
 * 4. Mixed protocol schemes (VLESS Reality, XHTTP, Hysteria 2, TUIC v5, NaïveProxy, AmneziaWG, Shadowsocks-2022)
 *
 * Each extracted node is tagged with [subscriptionId] for lifecycle synchronization and bulk pruning.
 */
object SubscriptionParser {

    private const val TAG = "SubscriptionParser"

    fun parseSubscriptionContent(
        rawContent: String,
        subscriptionId: String? = null
    ): SubscriptionParseResult {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return SubscriptionParseResult(emptyList(), listOf("Subscription body is empty"))
        }

        // 1. Attempt decoding as Base64 (Standard & URL-Safe)
        val decodedText = decodeIfBase64(trimmed)

        // 2. Split into candidate lines
        val lines = decodedText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }

        val parsedNodes = mutableListOf<ProxyConfig>()
        val errors = mutableListOf<String>()

        // 3. Process candidate lines
        for ((index, line) in lines.withIndex()) {
            // Check if line matches known protocol schemes
            if (isKnownProxyScheme(line)) {
                try {
                    val config = UriParser.parse(line, subscriptionId = subscriptionId)
                    parsedNodes.add(config)
                } catch (e: Exception) {
                    val errMsg = "Line ${index + 1} (${line.take(30)}...): ${e.message}"
                    Log.w(TAG, errMsg)
                    errors.add(errMsg)
                }
            } else if (line.contains("[Interface]", ignoreCase = true) && line.contains("[Peer]", ignoreCase = true)) {
                // Inline AmneziaWG / WireGuard configuration
                try {
                    val config = UriParser.parseWireGuardConf(
                        confText = line,
                        defaultName = "AmneziaWG Sub Node ${parsedNodes.size + 1}",
                        subscriptionId = subscriptionId
                    )
                    parsedNodes.add(config)
                } catch (e: Exception) {
                    errors.add("WG block at line ${index + 1}: ${e.message}")
                }
            }
        }

        return SubscriptionParseResult(
            nodes = parsedNodes,
            parseErrors = errors,
            rawNodeCount = lines.size
        )
    }

    /**
     * Inspects input string to determine if it is base64 encoded.
     * Decodes standard Base64 or URL-Safe Base64 with padding auto-repair.
     */
    fun decodeIfBase64(input: String): String {
        val cleaned = input.trim()

        // If it clearly starts with a recognized URI scheme, it's already plain text
        if (isKnownProxyScheme(cleaned) || cleaned.startsWith("[Interface]") || cleaned.contains("\nvless://")) {
            return cleaned
        }

        // Test if string contains non-base64 characters
        val base64CharsRegex = Regex("^[A-Za-z0-9+/=_\\-\\s]+$")
        if (!base64CharsRegex.matches(cleaned)) {
            return cleaned
        }

        return try {
            // Normalize URL-safe characters: '-' -> '+', '_' -> '/'
            var normalized = cleaned.replace('-', '+').replace('_', '/').replace("\\s".toRegex(), "")

            // Repair missing padding
            val missingPadding = (4 - (normalized.length % 4)) % 4
            if (missingPadding > 0) {
                normalized += "=".repeat(missingPadding)
            }

            val decodedBytes = Base64.decode(normalized, Base64.DEFAULT)
            val decodedString = String(decodedBytes, StandardCharsets.UTF_8).trim()

            // Verify decoded content looks like URI lists or config
            if (decodedString.isNotBlank() && (
                isKnownProxyScheme(decodedString) ||
                decodedString.contains("://") ||
                decodedString.contains("[Interface]") ||
                decodedString.lines().size > 1
            )) {
                decodedString
            } else {
                cleaned
            }
        } catch (_: Exception) {
            // Fallback to original string if decoding fails
            cleaned
        }
    }

    private fun isKnownProxyScheme(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("vless://") ||
               lower.startsWith("hysteria2://") ||
               lower.startsWith("hy2://") ||
               lower.startsWith("naive+https://") ||
               lower.startsWith("naive+quic://") ||
               lower.startsWith("tuic://") ||
               lower.startsWith("amneziawg://") ||
               lower.startsWith("awg://") ||
               lower.startsWith("ss://")
    }
}
