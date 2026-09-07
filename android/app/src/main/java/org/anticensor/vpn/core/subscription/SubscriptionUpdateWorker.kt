package org.anticensor.vpn.core.subscription

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager CoroutineWorker for unattended subscription synchronization.
 * Runs on a recurring schedule (every 12h or 24h) with network constraints.
 */
class SubscriptionUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SubscriptionWorker"
        const val UNIQUE_WORK_NAME = "aegis_subscription_sync_work"

        /**
         * Schedules periodic background synchronization of all configured subscription providers.
         */
        fun schedulePeriodicSync(
            context: Context,
            intervalHours: Long = 24,
            requiresUnmeteredNetwork: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (requiresUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<SubscriptionUpdateWorker>(
                intervalHours.coerceAtLeast(6),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .addTag("subscription_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWork
            )
            Log.i(TAG, "Scheduled subscription periodic sync every $intervalHours hours (unmetered=$requiresUnmeteredNetwork)")
        }

        /**
         * Triggers an immediate one-off background synchronization.
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWork = OneTimeWorkRequestBuilder<SubscriptionUpdateWorker>()
                .setConstraints(constraints)
                .addTag("subscription_sync_now")
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeWork)
            Log.i(TAG, "Triggered immediate one-off subscription synchronization")
        }

        /**
         * Cancels periodic background synchronization.
         */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.i(TAG, "Cancelled subscription periodic sync")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting background subscription synchronization task...")
        val repository = SubscriptionRepository.getInstance(appContext)
        val manager = SubscriptionManager()

        val subscriptions = repository.getSubscriptions()
        if (subscriptions.isEmpty()) {
            Log.i(TAG, "No subscriptions configured, task finished.")
            return Result.success()
        }

        var updateFailures = 0
        var totalNodesRefreshed = 0

        for (sub in subscriptions) {
            try {
                Log.d(TAG, "Syncing subscription '${sub.name}' from ${sub.url}...")
                val result = manager.fetchSubscription(
                    url = sub.url,
                    customName = sub.name,
                    userAgent = sub.userAgent,
                    existingId = sub.id,
                    autoUpdateIntervalHours = sub.autoUpdateIntervalHours
                )

                if (result.isSuccess) {
                    repository.saveSubscription(result.subscription)
                    repository.replaceSubscriptionNodes(sub.id, result.nodes)
                    totalNodesRefreshed += result.nodes.size
                    Log.i(TAG, "Successfully refreshed '${sub.name}': ${result.nodes.size} nodes")
                } else {
                    updateFailures++
                    repository.saveSubscription(result.subscription)
                    Log.w(TAG, "Failed refreshing '${sub.name}': ${result.error}")
                }
            } catch (e: Exception) {
                updateFailures++
                Log.e(TAG, "Error processing subscription '${sub.name}': ${e.message}", e)
            }
        }

        Log.i(TAG, "Background sync complete: refreshed $totalNodesRefreshed nodes across ${subscriptions.size} subscriptions (failures: $updateFailures)")
        return if (updateFailures == subscriptions.size) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
