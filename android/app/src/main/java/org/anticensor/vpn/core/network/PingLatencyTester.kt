package org.anticensor.vpn.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.anticensor.vpn.core.parser.ProxyConfig
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class PingResult(
    val serverId: String,
    val latencyMs: Long,
    val isSuccess: Boolean,
    val error: String? = null,
    val detailedCause: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class DetailedTcpTestResult(
    val latencyMs: Long,
    val isSuccess: Boolean,
    val errorCategory: String? = null,
    val detailedMessage: String? = null
)

/**
 * High-performance latency testing and deep network diagnostics engine.
 */
object PingLatencyTester {

    private const val TAG = "PingLatencyTester"
    private const val DEFAULT_TIMEOUT_MS = 3500

    val CONNECTIVITY_CHECK_URLS = listOf(
        "http://cp.cloudflare.com/generate_204",
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://www.google.com/generate_204"
    )

    /**
     * Performs a granular TCP handshake diagnostic test with root-cause categorization.
     */
    suspend fun diagnoseTcpLatency(
        host: String,
        port: Int,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): DetailedTcpTestResult = withContext(Dispatchers.IO) {
        val socket = Socket()
        val startTime = System.nanoTime()
        try {
            val socketAddress = InetSocketAddress(host, port)
            socket.connect(socketAddress, timeoutMs)
            val elapsedNanos = System.nanoTime() - startTime
            val latencyMs = elapsedNanos / 1_000_000
            DetailedTcpTestResult(
                latencyMs = latencyMs,
                isSuccess = true
            )
        } catch (e: UnknownHostException) {
            DetailedTcpTestResult(
                latencyMs = -1L,
                isSuccess = false,
                errorCategory = "DNS Resolution Failed",
                detailedMessage = "Domain '${host}' could not be resolved. Hostname does not exist or local ISP DNS is poisoned/blocked."
            )
        } catch (e: SocketTimeoutException) {
            DetailedTcpTestResult(
                latencyMs = -1L,
                isSuccess = false,
                errorCategory = "Connection Timed Out",
                detailedMessage = "Server '${host}:${port}' did not respond within ${timeoutMs}ms. IP/port dropped by Great Firewall / DPI or server is offline."
            )
        } catch (e: ConnectException) {
            DetailedTcpTestResult(
                latencyMs = -1L,
                isSuccess = false,
                errorCategory = "Connection Refused (RST)",
                detailedMessage = "Server actively refused TCP connection on port $port. Target port is closed, or active probing sent TCP RST packet."
            )
        } catch (e: Exception) {
            DetailedTcpTestResult(
                latencyMs = -1L,
                isSuccess = false,
                errorCategory = "Network Unreachable",
                detailedMessage = e.message ?: "General I/O socket failure connecting to $host:$port"
            )
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {}
        }
    }

    /**
     * Measures raw TCP handshake latency to the remote server host and port.
     * Returns the latency in milliseconds, or -1 if unreachable/timeout.
     */
    suspend fun testTcpLatency(
        host: String,
        port: Int,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): Long {
        val diag = diagnoseTcpLatency(host, port, timeoutMs)
        return diag.latencyMs
    }

    /**
     * Measures true end-to-end HTTP generate_204 latency (either direct or through SOCKS5 proxy).
     */
    suspend fun testHttp204Latency(
        urlStr: String = CONNECTIVITY_CHECK_URLS.first(),
        socks5Port: Int? = null,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            val proxy = if (socks5Port != null && socks5Port > 0) {
                java.net.Proxy(java.net.Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socks5Port))
            } else {
                java.net.Proxy.NO_PROXY
            }
            connection = (url.openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
                useCaches = false
                requestMethod = "HEAD"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AegisVPN/2.0)")
            }
            val responseCode = connection.responseCode
            val elapsed = System.currentTimeMillis() - startTime
            if (responseCode == 204 || responseCode in 200..299) {
                elapsed
            } else {
                -1L
            }
        } catch (e: Exception) {
            Log.d(TAG, "HTTP 204 test failed ($urlStr, proxy=$socks5Port): ${e.message}")
            -1L
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Pings a single ProxyConfig profile and produces a structured PingResult.
     * Can use TCP Handshake or True HTTP-204 if tunnel is running.
     */
    suspend fun pingServer(
        config: ProxyConfig,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        useHttp204: Boolean = false,
        socks5Port: Int? = null
    ): PingResult {
        return if (useHttp204 && socks5Port != null) {
            val latency = testHttp204Latency(socks5Port = socks5Port, timeoutMs = timeoutMs)
            PingResult(
                serverId = config.id,
                latencyMs = latency,
                isSuccess = latency >= 0,
                error = if (latency < 0) "HTTP-204 Unreachable via SOCKS5" else null,
                detailedCause = if (latency < 0) "Captive portal check (cp.cloudflare.com/generate_204) failed through proxy tunnel. Remote server may not have outbound internet access or TLS decrypt failed." else null
            )
        } else {
            val diag = diagnoseTcpLatency(config.server, config.port, timeoutMs)
            PingResult(
                serverId = config.id,
                latencyMs = diag.latencyMs,
                isSuccess = diag.isSuccess,
                error = diag.errorCategory,
                detailedCause = diag.detailedMessage
            )
        }
    }

    /**
     * Batch pings a list of proxy servers concurrently with a bounded dispatcher.
     */
    suspend fun batchPing(
        servers: List<ProxyConfig>,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): Map<String, Long> = coroutineScope {
        val deferredResults = servers.map { server ->
            async(Dispatchers.IO) {
                val result = pingServer(server, timeoutMs)
                server.id to result.latencyMs
            }
        }
        deferredResults.awaitAll().toMap()
    }
}
