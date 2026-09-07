package org.anticensor.vpn.core.routing

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class AppRoutingInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isProxyEnabled: Boolean = true
)

enum class RoutingMode(val displayName: String, val description: String) {
    GLOBAL_PROXY("Global Proxy", "Route all device traffic through secure tunnel"),
    BYPASS_LAN_AND_LOCAL("Bypass LAN & Direct", "Bypass private IPs and direct sites, proxy blocked traffic"),
    SPLIT_TUNNEL_PER_APP("Per-App Split Tunnel", "Only route selected applications through VPN")
}

enum class DnsMode(val displayName: String, val upstreamDns: String, val isEncrypted: Boolean) {
    CLOUDFLARE_DOH("Cloudflare DoH", "https://1.1.1.1/dns-query", true),
    GOOGLE_DOH("Google DoH", "https://dns.google/dns-query", true),
    ADGUARD_DOH("AdGuard DoH (AdBlock)", "https://dns.adguard.com/dns-query", true),
    SYSTEM_DNS("Standard DNS", "1.1.1.1, 8.8.8.8", false)
}

/**
 * Manages routing rules, split-tunneling app selection, and DNS privacy configurations.
 */
class RoutingSettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "aegis_routing_settings"
        private const val KEY_ROUTING_MODE = "key_routing_mode"
        private const val KEY_DNS_MODE = "key_dns_mode"
        private const val KEY_SELECTED_APPS = "key_selected_apps"
        private const val KEY_KILL_SWITCH = "key_kill_switch"
        private const val KEY_SMART_FAILOVER = "key_smart_failover"
        private const val KEY_PACKET_FRAGMENTATION = "key_packet_fragmentation"
        private const val KEY_CUSTOM_MTU = "key_custom_mtu"
        private const val KEY_AUTO_UPDATE_SUBS = "key_auto_update_subs"
        private const val KEY_SUBS_INTERVAL_HOURS = "key_subs_interval_hours"

        @Volatile
        private var instance: RoutingSettingsRepository? = null

        fun getInstance(context: Context): RoutingSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: RoutingSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    var routingMode: RoutingMode
        get() {
            val name = prefs.getString(KEY_ROUTING_MODE, RoutingMode.GLOBAL_PROXY.name)
            return try {
                RoutingMode.valueOf(name ?: RoutingMode.GLOBAL_PROXY.name)
            } catch (_: Exception) {
                RoutingMode.GLOBAL_PROXY
            }
        }
        set(value) {
            prefs.edit().putString(KEY_ROUTING_MODE, value.name).apply()
        }

    var dnsMode: DnsMode
        get() {
            val name = prefs.getString(KEY_DNS_MODE, DnsMode.CLOUDFLARE_DOH.name)
            return try {
                DnsMode.valueOf(name ?: DnsMode.CLOUDFLARE_DOH.name)
            } catch (_: Exception) {
                DnsMode.CLOUDFLARE_DOH
            }
        }
        set(value) {
            prefs.edit().putString(KEY_DNS_MODE, value.name).apply()
        }

    var isKillSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, false)
        set(value) = prefs.edit().putBoolean(KEY_KILL_SWITCH, value).apply()

    var isSmartFailoverEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_FAILOVER, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_FAILOVER, value).apply()

    var isPacketFragmentationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PACKET_FRAGMENTATION, true)
        set(value) = prefs.edit().putBoolean(KEY_PACKET_FRAGMENTATION, value).apply()

    var customMtu: Int
        get() = prefs.getInt(KEY_CUSTOM_MTU, 1400)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_MTU, value.coerceIn(1280, 1500)).apply()

    var isAutoUpdateSubscriptionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_SUBS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE_SUBS, value).apply()

    var subscriptionIntervalHours: Int
        get() = prefs.getInt(KEY_SUBS_INTERVAL_HOURS, 12)
        set(value) = prefs.edit().putInt(KEY_SUBS_INTERVAL_HOURS, value.coerceIn(1, 48)).apply()

    fun getSelectedApps(): Set<String> {
        val raw = prefs.getString(KEY_SELECTED_APPS, null) ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            val result = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
            result
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun setSelectedApps(packageNames: Set<String>) {
        val array = JSONArray()
        packageNames.forEach { array.put(it) }
        prefs.edit().putString(KEY_SELECTED_APPS, array.toString()).apply()
    }

    fun toggleAppSelection(packageName: String): Boolean {
        val current = getSelectedApps().toMutableSet()
        val newState = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        setSelectedApps(current)
        return newState
    }

    /**
     * Scans installed applications on device for split tunneling management.
     */
    suspend fun getInstalledApps(context: Context): List<AppRoutingInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val selectedApps = getSelectedApps()

        installedApps
            .filter { it.packageName != context.packageName } // Exclude self
            .map { appInfo ->
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = pm.getApplicationLabel(appInfo).toString()
                AppRoutingInfo(
                    packageName = appInfo.packageName,
                    appName = label,
                    isSystemApp = isSystem,
                    isProxyEnabled = selectedApps.contains(appInfo.packageName)
                )
            }
            .sortedWith(
                compareByDescending<AppRoutingInfo> { it.isProxyEnabled }
                    .thenBy { it.isSystemApp }
                    .thenBy { it.appName.lowercase() }
            )
    }
}
