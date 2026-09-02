package com.archimede.w2full.sync

import com.archimede.w2full.data.mimit.MimitRefreshResult
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.NearbyStationsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MimitSyncWorkerPolicyTest {
    @Test
    fun workerCallsTheSharedRepositoryRefreshAndRetriesOnlyRetryableFailures() = runBlocking {
        val retryable = FakeRepository(MimitRefreshResult.Failure(retryable = true))
        assertEquals(MimitSyncWorkerDecision.RETRY, runMimitSync(retryable))
        assertEquals(1, retryable.refreshCalls)

        val formatFailure = FakeRepository(MimitRefreshResult.Failure(retryable = false))
        assertEquals(MimitSyncWorkerDecision.SUCCESS, runMimitSync(formatFailure))
        assertEquals(1, formatFailure.refreshCalls)

        val success = FakeRepository(MimitRefreshResult.Success(123L))
        assertEquals(MimitSyncWorkerDecision.SUCCESS, runMimitSync(success))
        assertEquals(1, success.refreshCalls)
    }

    private class FakeRepository(
        private val result: MimitRefreshResult,
    ) : NearbyStationsRepository {
        var refreshCalls: Int = 0

        override fun observeStations(): Flow<NearbyStationsSnapshot?> = flowOf(null)

        override suspend fun loadCachedSnapshot(): NearbyStationsSnapshot? = null

        override suspend fun refresh(): MimitRefreshResult {
            refreshCalls += 1
            return result
        }
    }
}
