package org.anticensor.vpn.core.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.anticensor.vpn.core.parser.ProxyConfig
import org.anticensor.vpn.core.parser.UriParser
import org.json.JSONArray
import org.json.JSONObject

/**
 * Storage and Synchronization Repository for Subscriptions and their associated Proxy Nodes.
 * Provides safe JSON serialization via Android SharedPreferences.
 */
class SubscriptionRepository(context: Context) {

    companion object {
        private const val TAG = "SubscriptionRepo"
        private const val PREFS_NAME = "aegis_subscriptions_prefs"
        private const val KEY_SUBSCRIPTIONS = "saved_subscriptions_json"
        private const val KEY_SUBSCRIPTION_NODES = "subscription_nodes_json"

        @Volatile
        private var INSTANCE: SubscriptionRepository? = null

        fun getInstance(context: Context): SubscriptionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Loads all saved subscriptions from storage.
     */
    fun getSubscriptions(): List<Subscription> {
        val rawJson = prefs.getString(KEY_SUBSCRIPTIONS, null) ?: return emptyList()
        val list = mutableListOf<Subscription>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val userInfoObj = obj.optJSONObject("userInfo")
                val userInfo = if (userInfoObj != null) {
                    SubscriptionUserInfo(
                        upload = userInfoObj.optLong("upload", 0L),
                        download = userInfoObj.optLong("download", 0L),
                        total = userInfoObj.optLong("total", 0L),
                        expireEpochSeconds = userInfoObj.optLong("expire", 0L)
                    )
                } else null

                val sub = Subscription(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    url = obj.getString("url"),
                    lastUpdated = obj.optLong("lastUpdated", 0L),
                    userInfo = userInfo,
                    nodeCount = obj.optInt("nodeCount", 0),
                    userAgent = obj.optString("userAgent", SubscriptionManager.UA_KARING),
                    autoUpdateIntervalHours = obj.optInt("autoUpdateIntervalHours", 24),
                    status = SubscriptionStatus.valueOf(obj.optString("status", SubscriptionStatus.IDLE.name)),
                    lastErrorMessage = obj.optString("lastErrorMessage").takeIf { it.isNotBlank() }
                )
                list.add(sub)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing subscriptions: ${e.message}", e)
        }
        return list
    }

    /**
     * Saves the current list of subscriptions.
     */
    fun saveSubscriptions(subscriptions: List<Subscription>) {
        try {
            val array = JSONArray()
            for (sub in subscriptions) {
                val obj = JSONObject().apply {
                    put("id", sub.id)
                    put("name", sub.name)
                    put("url", sub.url)
                    put("lastUpdated", sub.lastUpdated)
                    put("nodeCount", sub.nodeCount)
                    put("userAgent", sub.userAgent)
                    put("autoUpdateIntervalHours", sub.autoUpdateIntervalHours)
                    put("status", sub.status.name)
                    put("lastErrorMessage", sub.lastErrorMessage ?: "")
                    if (sub.userInfo != null) {
                        val uObj = JSONObject().apply {
                            put("upload", sub.userInfo.upload)
                            put("download", sub.userInfo.download)
                            put("total", sub.userInfo.total)
                            put("expire", sub.userInfo.expireEpochSeconds)
                        }
                        put("userInfo", uObj)
                    }
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_SUBSCRIPTIONS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing subscriptions: ${e.message}", e)
        }
    }

    /**
     * Upserts a subscription in persistent storage.
     */
    fun saveSubscription(subscription: Subscription) {
        val current = getSubscriptions().toMutableList()
        val index = current.indexOfFirst { it.id == subscription.id }
        if (index >= 0) {
            current[index] = subscription
        } else {
            current.add(subscription)
        }
        saveSubscriptions(current)
    }

    /**
     * Deletes a subscription by ID and removes its corresponding nodes.
     */
    fun deleteSubscription(subscriptionId: String): List<ProxyConfig> {
        val subs = getSubscriptions().filter { it.id != subscriptionId }
        saveSubscriptions(subs)
        val remainingNodes = getSubscriptionNodes().filter { it.subscriptionId != subscriptionId }
        saveSubscriptionNodes(remainingNodes)
        return remainingNodes
    }

    /**
     * Saves all proxy nodes parsed from subscriptions.
     * Note: We serialize nodes using their canonical URI representations or config data.
     */
    fun saveSubscriptionNodes(nodes: List<ProxyConfig>) {
        try {
            val array = JSONArray()
            for (node in nodes) {
                val obj = JSONObject().apply {
                    put("id", node.id)
                    put("subscriptionId", node.subscriptionId ?: "")
                    put("rawUri", node.toShareLink())
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_SUBSCRIPTION_NODES, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing subscription nodes: ${e.message}", e)
        }
    }

    /**
     * Retrieves all saved proxy nodes belonging to subscriptions.
     */
    fun getSubscriptionNodes(): List<ProxyConfig> {
        val rawJson = prefs.getString(KEY_SUBSCRIPTION_NODES, null) ?: return emptyList()
        val list = mutableListOf<ProxyConfig>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawUri = obj.getString("rawUri")
                val subId = obj.optString("subscriptionId").takeIf { it.isNotBlank() }
                try {
                    val config = UriParser.parse(rawUri, subscriptionId = subId)
                    list.add(config)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore node: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing subscription nodes: ${e.message}", e)
        }
        return list
    }

    /**
     * Atomically replaces all nodes belonging to [subscriptionId] with [newNodes].
     */
    fun replaceSubscriptionNodes(subscriptionId: String, newNodes: List<ProxyConfig>): List<ProxyConfig> {
        val currentNodes = getSubscriptionNodes().filter { it.subscriptionId != subscriptionId }
        val combined = currentNodes + newNodes
        saveSubscriptionNodes(combined)
        return combined
    }
}
