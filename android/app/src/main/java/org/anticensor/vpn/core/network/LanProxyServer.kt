package org.anticensor.vpn.core.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance Hotspot / Wi-Fi LAN Proxy Sharing Server.
 * Allows laptops, PCs, Smart TVs, and other devices connected to the phone's Wi-Fi / Hotspot
 * to route their internet through the phone's anti-censorship VPN tunnel.
 *
 * Listens on 0.0.0.0:10809 and transparently relays traffic to local SOCKS5 tunnel (127.0.0.1:10808).
 */
object LanProxyServer {

    private const val DEFAULT_LAN_PORT = 10809
    private const val SOCKS5_TUNNEL_PORT = 10808
    private const val SOCKS5_TUNNEL_HOST = "127.0.0.1"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val isRunningFlag = AtomicBoolean(false)
    val activeClientCount = AtomicInteger(0)

    val isRunning: Boolean
        get() = isRunningFlag.get()

    /**
     * Starts LAN proxy sharing on all network interfaces (0.0.0.0:port)
     */
    fun start(port: Int = DEFAULT_LAN_PORT, onStatusChange: ((Boolean, String) -> Unit)? = null) {
        if (isRunningFlag.get()) return

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val ss = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress("0.0.0.0", port))
                }
                serverSocket = ss
                isRunningFlag.set(true)
                onStatusChange?.invoke(true, "LAN Proxy listening on port $port")

                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        activeClientCount.incrementAndGet()
                        launch {
                            handleClientRelay(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) {
                            // Log error
                        }
                    }
                }
            } catch (e: Exception) {
                isRunningFlag.set(false)
                onStatusChange?.invoke(false, "Failed to start LAN Proxy: ${e.message}")
            } finally {
                stop()
                onStatusChange?.invoke(false, "LAN Proxy stopped")
            }
        }
    }

    fun stop() {
        isRunningFlag.set(false)
        activeClientCount.set(0)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * Relays raw bytes bidirectionally between external LAN device and local SOCKS5 proxy
     */
    private suspend fun handleClientRelay(clientSocket: Socket) = withContext(Dispatchers.IO) {
        var tunnelSocket: Socket? = null
        try {
            tunnelSocket = Socket()
            tunnelSocket.connect(InetSocketAddress(SOCKS5_TUNNEL_HOST, SOCKS5_TUNNEL_PORT), 3000)

            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val tunnelIn = tunnelSocket.getInputStream()
            val tunnelOut = tunnelSocket.getOutputStream()

            // Bidirectional streaming pipes
            val job1 = launch { pipeStreams(clientIn, tunnelOut) }
            val job2 = launch { pipeStreams(tunnelIn, clientOut) }

            joinAll(job1, job2)
        } catch (_: Exception) {
            // Client disconnected or tunnel timeout
        } finally {
            activeClientCount.decrementAndGet()
            try { clientSocket.close() } catch (_: Exception) {}
            try { tunnelSocket?.close() } catch (_: Exception) {}
        }
    }

    private fun pipeStreams(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        try {
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: Exception) {}
    }

    /**
     * Resolves the phone's current Wi-Fi or Hotspot IPv4 address (e.g. 192.168.43.1 or 192.168.1.50)
     */
    fun getLocalIpAddress(context: Context? = null): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                // Check common hotspot and wifi interface names (wlan, ap, rndis)
                val name = iface.name.lowercase()
                val isHotspotOrWifi = name.contains("wlan") || name.contains("ap") || name.contains("swlan")

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddr = addr.hostAddress ?: ""
                        if (hostAddr.isNotBlank() && !hostAddr.startsWith("127.")) {
                            if (isHotspotOrWifi || hostAddr.startsWith("192.168.") || hostAddr.startsWith("10.")) {
                                return hostAddr
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return "192.168.43.1" // Standard Android Wi-Fi AP fallback
    }

    /**
     * Full sharing instructions for the user to configure laptops/TVs
     */
    fun getSharingInstructions(port: Int = DEFAULT_LAN_PORT, localIp: String): String {
        return "HTTP/SOCKS5 Proxy IP: $localIp, Port: $port"
    }
}
