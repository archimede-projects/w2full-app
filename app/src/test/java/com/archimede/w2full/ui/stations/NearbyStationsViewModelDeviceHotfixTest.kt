package com.archimede.w2full.ui.stations

import com.archimede.w2full.data.mimit.MimitRefreshResult
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.NearbyStationsSnapshot
import com.archimede.w2full.location.UserLocationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NearbyStationsViewModelDeviceHotfixTest {
    @Test
    fun failedMimitRefreshWithoutCacheStillResolvesLocationState() {
        val repository = NoCacheFailureRepository(UserLocationResult.PermissionDenied)
        val viewModel = NearbyStationsViewModel(repository)

        viewModel.refresh()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertEquals(NearbyLocationUiStatus.PERMISSION_DENIED, state.locationStatus)
        assertEquals(MIMIT_REFRESH_ERROR_MESSAGE, state.errorMessage)
        assertFalse(state.isLoading)
        assertEquals(1, repository.refreshCalls)
        assertEquals(1, repository.resolveLocationCalls)
    }

    private class NoCacheFailureRepository(
        private val locationResult: UserLocationResult,
    ) : NearbyStationsRepository {
        var refreshCalls = 0
        var resolveLocationCalls = 0

        override fun observeStations(): Flow<NearbyStationsSnapshot?> = flowOf(null)

        override suspend fun loadCachedSnapshot(): NearbyStationsSnapshot? = null

        override suspend fun resolveLocation(): UserLocationResult {
            resolveLocationCalls += 1
            return locationResult
        }

        override suspend fun refresh(): MimitRefreshResult {
            refreshCalls += 1
            return MimitRefreshResult.Failure(retryable = false)
        }
    }
}
