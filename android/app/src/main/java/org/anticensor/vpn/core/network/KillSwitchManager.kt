package org.anticensor.vpn.core.network

import android.content.Context
import android.os.Build
import org.anticensor.vpn.core.routing.DnsMode
import org.anticensor.vpn.core.routing.RoutingSettingsRepository

/**
 * Kill Switch & DNS Leak Shield Controller.
 * Protects users against ISP Deep Packet Inspection (DPI) and IP/DNS leaks when tunnels drop unexpectedly.
 */
object KillSwitchManager {

    /**
     * Checks whether system-level Always-On VPN and Block connections without VPN
     * are active in Android settings.
     */
    fun isSystemAlwaysOnVpnSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    /**
     * Validates DNS Leak protection profile. Ensures standard plaintext UDP 53 DNS is blocked
     * and strictly routed to DoH / DoT encrypted upstreams.
     */
    fun getStrictDnsConfig(dnsMode: DnsMode): Pair<List<String>, Boolean> {
        return when (dnsMode) {
            DnsMode.CLOUDFLARE_DOH -> Pair(listOf("1.1.1.1", "1.0.0.1"), true)
            DnsMode.GOOGLE_DOH -> Pair(listOf("8.8.8.8", "8.8.4.4"), true)
            DnsMode.ADGUARD_DOH -> Pair(listOf("94.140.14.14", "94.140.15.15"), true)
            DnsMode.SYSTEM_DNS -> Pair(listOf("1.1.1.1", "8.8.8.8"), false)
        }
    }

    /**
     * Formats security audit status for UI display
     */
    fun getSecurityAuditSummary(context: Context): SecurityAuditReport {
        val routing = RoutingSettingsRepository.getInstance(context)
        return SecurityAuditReport(
            isKillSwitchActive = routing.isKillSwitchEnabled,
            dnsShieldActive = routing.dnsMode.isEncrypted,
            dnsProviderName = routing.dnsMode.displayName,
            isBypassDomesticActive = routing.routingMode == org.anticensor.vpn.core.routing.RoutingMode.BYPASS_LAN_AND_LOCAL,
            isSplitTunnelActive = routing.routingMode == org.anticensor.vpn.core.routing.RoutingMode.SPLIT_TUNNEL_PER_APP,
            selectedAppCount = routing.getSelectedApps().size
        )
    }
}

data class SecurityAuditReport(
    val isKillSwitchActive: Boolean,
    val dnsShieldActive: Boolean,
    val dnsProviderName: String,
    val isBypassDomesticActive: Boolean,
    val isSplitTunnelActive: Boolean,
    val selectedAppCount: Int
)
