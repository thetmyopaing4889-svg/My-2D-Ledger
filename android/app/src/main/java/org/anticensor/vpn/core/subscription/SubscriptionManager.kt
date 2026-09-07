package org.anticensor.vpn.core.subscription

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.anticensor.vpn.core.parser.ProxyConfig
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class SubscriptionStatus {
    IDLE,
    UPDATING,
    SUCCESS,
    ERROR
}

/**
 * Parsed subscription bandwidth quota and expiration metadata
 * from the 'Subscription-Userinfo' or 'subscription-userinfo' HTTP response header.
 */
data class SubscriptionUserInfo(
    val upload: Long = 0L,
    val download: Long = 0L,
    val total: Long = 0L,
    val expireEpochSeconds: Long = 0L
) {
    val usedBytes: Long
        get() = upload + download

    val remainingBytes: Long
        get() = if (total > 0) (total - usedBytes).coerceAtLeast(0L) else 0L

    val usagePercentage: Float
        get() = if (total > 0) ((usedBytes.toDouble() / total.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f) else 0f

    val isExpired: Boolean
        get() = expireEpochSeconds > 0 && expireEpochSeconds < (System.currentTimeMillis() / 1000L)

    val formattedExpiry: String
        get() = if (expireEpochSeconds > 0) {
            val date = Date(expireEpochSeconds * 1000L)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } else {
            "Permanent (No Expiry)"
        }
}

/**
 * Model representing a tracked remote subscription provider.
 */
data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val lastUpdated: Long = 0L,
    val userInfo: SubscriptionUserInfo? = null,
    val nodeCount: Int = 0,
    val userAgent: String = SubscriptionManager.UA_KARING,
    val autoUpdateIntervalHours: Int = 24,
    val status: SubscriptionStatus = SubscriptionStatus.IDLE,
    val lastErrorMessage: String? = null
) {
    val formattedLastUpdated: String
        get() = if (lastUpdated > 0) {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastUpdated))
        } else {
            "Never Updated"
        }
}

data class SubscriptionFetchResult(
    val subscription: Subscription,
    val nodes: List<ProxyConfig>,
    val parseErrors: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val error: String? = null
)

/**
 * High-performance Remote Subscription Manager.
 * Handles HTTPS fetching with User-Agent camouflage, redirect chains,
 * quota header parsing, and node synchronization.
 */
class SubscriptionManager(
    private val client: OkHttpClient = defaultClient()
) {
    companion object {
        private const val TAG = "SubscriptionManager"

        // Anti-Censorship User-Agent Presets for CDN / Airport compatibility
        const val UA_KARING = "Karing/1.0.32 (Android; Mobile; AntiCensorship)"
        const val UA_V2RAYNG = "v2rayNG/1.8.19 (Android 14; Mobile; arm64-v8a)"
        const val UA_CLASH_META = "ClashMeta/1.18.0 (Android 14)"
        const val UA_SHADOWROCKET = "Shadowrocket/2.2.34 (iOS 17.5.1)"
        const val UA_AEGIS = "AegisVPN/1.0.0 (Android 14; AegisAntiDPI)"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun parseUserInfoHeader(headerValue: String?): SubscriptionUserInfo? {
            if (headerValue.isNullOrBlank()) return null
            var upload = 0L
            var download = 0L
            var total = 0L
            var expire = 0L

            // Standard format: upload=1073741824; download=10737418240; total=107374182400; expire=1735689600
            headerValue.split(";").forEach { segment ->
                val parts = segment.trim().split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim().toLongOrNull() ?: 0L
                    when (key) {
                        "upload" -> upload = value
                        "download" -> download = value
                        "total" -> total = value
                        "expire" -> expire = value
                    }
                }
            }

            return SubscriptionUserInfo(
                upload = upload,
                download = download,
                total = total,
                expireEpochSeconds = expire
            )
        }

        fun formatBytes(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            val tb = gb / 1024.0
            return when {
                tb >= 1.0 -> String.format(Locale.US, "%.2f TB", tb)
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }

    /**
     * Downloads subscription content from remote URL, extracts headers, and parses proxy nodes.
     */
    suspend fun fetchSubscription(
        url: String,
        customName: String? = null,
        userAgent: String = UA_KARING,
        existingId: String? = null,
        autoUpdateIntervalHours: Int = 24
    ): SubscriptionFetchResult = withContext(Dispatchers.IO) {
        val targetId = existingId ?: UUID.randomUUID().toString()
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", userAgent)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Connection", "close")
            .build()

        try {
            client.newCall(request).execute().use { response: Response ->
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    Log.e(TAG, "Failed to fetch subscription from $url: $errMsg")
                    return@withContext SubscriptionFetchResult(
                        subscription = Subscription(
                            id = targetId,
                            name = customName ?: "Subscription ($targetId)",
                            url = url,
                            lastUpdated = System.currentTimeMillis(),
                            status = SubscriptionStatus.ERROR,
                            lastErrorMessage = errMsg
                        ),
                        nodes = emptyList(),
                        isSuccess = false,
                        error = errMsg
                    )
                }

                // 1. Extract Quota Metadata from response headers
                val userInfoHeader = response.header("Subscription-Userinfo")
                    ?: response.header("subscription-userinfo")
                    ?: response.header("Subscription-UserInfo")
                val parsedUserInfo = parseUserInfoHeader(userInfoHeader)

                // 2. Extract suggested title from Content-Disposition or Profile-Title if customName not given
                val fallbackTitle = response.header("Profile-Title")
                    ?: response.header("Content-Disposition")?.let { extractFilename(it) }
                    ?: run {
                        val host = try { okhttp3.HttpUrl.get(url).host } catch (_: Exception) { "Proxy Sub" }
                        "Sub: $host"
                    }
                val finalName = customName?.takeIf { it.isNotBlank() } ?: fallbackTitle

                // 3. Extract Auto-Update interval if specified by server
                val intervalHeader = response.header("Profile-Update-Interval")?.toIntOrNull()
                val finalInterval = intervalHeader ?: autoUpdateIntervalHours

                // 4. Read response body and parse nodes
                val bodyText = response.body?.string() ?: ""
                val parseResult = SubscriptionParser.parseSubscriptionContent(
                    rawContent = bodyText,
                    subscriptionId = targetId
                )

                val updatedSub = Subscription(
                    id = targetId,
                    name = finalName,
                    url = url,
                    lastUpdated = System.currentTimeMillis(),
                    userInfo = parsedUserInfo,
                    nodeCount = parseResult.nodes.size,
                    userAgent = userAgent,
                    autoUpdateIntervalHours = finalInterval,
                    status = SubscriptionStatus.SUCCESS,
                    lastErrorMessage = null
                )

                Log.i(TAG, "Successfully fetched '${updatedSub.name}': ${parseResult.nodes.size} nodes imported")
                SubscriptionFetchResult(
                    subscription = updatedSub,
                    nodes = parseResult.nodes,
                    parseErrors = parseResult.parseErrors,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during subscription fetch: ${e.message}", e)
            SubscriptionFetchResult(
                subscription = Subscription(
                    id = targetId,
                    name = customName ?: "Subscription ($targetId)",
                    url = url,
                    lastUpdated = System.currentTimeMillis(),
                    status = SubscriptionStatus.ERROR,
                    lastErrorMessage = e.message ?: "Network error"
                ),
                nodes = emptyList(),
                isSuccess = false,
                error = e.message ?: "Failed to connect to subscription provider"
            )
        }
    }

    private fun extractFilename(contentDisposition: String): String? {
        val pattern = Regex("filename[*]?=['\"]?(?:UTF-8'')?([^'\";]+)['\"]?", RegexOption.IGNORE_CASE)
        val match = pattern.find(contentDisposition)
        return match?.groupValues?.get(1)?.trim()
    }
}
