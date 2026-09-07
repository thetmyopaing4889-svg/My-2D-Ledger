package org.anticensor.vpn.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.Locale

data class IpGeoInfo(
    val ip: String,
    val countryCode: String, // e.g. "SG", "JP", "US", "DE", "MM"
    val countryName: String, // e.g. "Singapore", "Japan"
    val city: String = "",
    val org: String = "", // ISP or Cloud provider, e.g. "DigitalOcean", "Cloudflare"
    val flagEmoji: String = getCountryFlagEmoji(countryCode),
    val isVpnActive: Boolean = false,
    val queryTimestamp: Long = System.currentTimeMillis()
)

fun getCountryFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val upper = countryCode.uppercase(Locale.ROOT)
    val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
    return try {
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (_: Exception) {
        "🌐"
    }
}

/**
 * Real-time public IP and Geo-location lookup engine.
 *
 * Checks current outbound exit IP and country either directly (before VPN connects)
 * or through the SOCKS5 loopback tunnel (when VPN is active).
 */
object IpGeoLookupService {

    private const val TAG = "IpGeoLookupService"

    /**
     * Resolves flag emoji and country name heuristically from server address or server name.
     */
    fun detectCountry(serverHost: String, serverName: String): Pair<String, String> {
        val combined = "$serverName $serverHost".lowercase(Locale.ROOT)
        return when {
            combined.contains("sg") || combined.contains("singapore") -> "SG" to "Singapore"
            combined.contains("jp") || combined.contains("japan") || combined.contains("tokyo") -> "JP" to "Japan"
            combined.contains("us") || combined.contains("united states") || combined.contains("america") || combined.contains("la") -> "US" to "United States"
            combined.contains("de") || combined.contains("germany") || combined.contains("frankfurt") -> "DE" to "Germany"
            combined.contains("nl") || combined.contains("netherlands") || combined.contains("amsterdam") -> "NL" to "Netherlands"
            combined.contains("hk") || combined.contains("hong kong") -> "HK" to "Hong Kong"
            combined.contains("kr") || combined.contains("korea") || combined.contains("seoul") -> "KR" to "South Korea"
            combined.contains("uk") || combined.contains("london") || combined.contains("gb") -> "GB" to "United Kingdom"
            combined.contains("ca") || combined.contains("canada") -> "CA" to "Canada"
            combined.contains("mm") || combined.contains("myanmar") || combined.contains("burma") -> "MM" to "Myanmar"
            else -> "UN" to "Global Node"
        }
    }

    /**
     * Performs a live IP & GeoIP query.
     * Uses ipapi.co / ip-api.com / icanhazip fallback endpoints.
     */
    suspend fun fetchCurrentExitIp(socks5Port: Int? = null): Result<IpGeoInfo> = withContext(Dispatchers.IO) {
        val proxy = if (socks5Port != null && socks5Port > 0) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socks5Port))
        } else {
            Proxy.NO_PROXY
        }

        // Try IPAPI json
        try {
            val url = URL("https://ipapi.co/json/")
            val conn = (url.openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AegisVPN/2.0)")
            }
            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(responseText)
                val ip = json.optString("ip", "Unknown IP")
                val countryCode = json.optString("country_code", "UN")
                val countryName = json.optString("country_name", "Global")
                val city = json.optString("city", "")
                val org = json.optString("org", "")
                return@withContext Result.success(
                    IpGeoInfo(
                        ip = ip,
                        countryCode = countryCode,
                        countryName = countryName,
                        city = city,
                        org = org,
                        isVpnActive = socks5Port != null
                    )
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "ipapi.co check failed: ${e.message}")
        }

        // Fallback: icanhazip.com plain IP
        try {
            val url = URL("https://icanhazip.com")
            val conn = (url.openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "curl/8.0")
            }
            if (conn.responseCode in 200..299) {
                val ip = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                return@withContext Result.success(
                    IpGeoInfo(
                        ip = ip,
                        countryCode = "UN",
                        countryName = if (socks5Port != null) "Secured Tunnel" else "Direct Network",
                        city = "",
                        org = "",
                        isVpnActive = socks5Port != null
                    )
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "icanhazip check failed: ${e.message}")
        }

        Result.failure(Exception("Unable to retrieve public IP from remote GeoIP endpoints"))
    }
}
