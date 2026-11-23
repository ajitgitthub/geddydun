package com.personal.remindme

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Every 15 minutes or so, even with the app closed: anything waiting for my OK
 * (an errand, a shared alarm, suggested changes, a list invite) gets a notification,
 * and comes back every 4 hours if swiped away unanswered.
 */
class ApprovalCheck(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Store.init(applicationContext)
        runCatching { Cloud.checkApprovals(applicationContext) }
        return Result.success()
    }

    companion object {
        fun start(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ApprovalCheck>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("approvals", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
