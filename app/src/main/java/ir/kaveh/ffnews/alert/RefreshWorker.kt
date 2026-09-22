package ir.kaveh.ffnews.alert

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.kaveh.ffnews.data.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Refreshes the weekly calendar every few hours in background and re-arms alarms. */
class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ok = NewsRepository.fetch(applicationContext).isSuccess
        AlertScheduler.reschedule(applicationContext)
        if (ok) Result.success() else Result.retry()
    }

    companion object {
        fun enqueue(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<RefreshWorker>(4, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "ff_refresh", ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
