package com.archimede.w2full.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.data.mimit.MimitRefreshResult
import com.archimede.w2full.data.mimit.NearbyStationsRepository

class MimitSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? W2FullApplication ?: return Result.failure()
        return when (runMimitSync(application.nearbyStationsRepository)) {
            MimitSyncWorkerDecision.SUCCESS -> Result.success()
            MimitSyncWorkerDecision.RETRY -> Result.retry()
        }
    }
}

internal enum class MimitSyncWorkerDecision {
    SUCCESS,
    RETRY,
}

internal suspend fun runMimitSync(repository: NearbyStationsRepository): MimitSyncWorkerDecision =
    when (val result = repository.refresh()) {
        is MimitRefreshResult.Success -> MimitSyncWorkerDecision.SUCCESS
        is MimitRefreshResult.Failure -> if (result.retryable) {
            MimitSyncWorkerDecision.RETRY
        } else {
            MimitSyncWorkerDecision.SUCCESS
        }
    }
