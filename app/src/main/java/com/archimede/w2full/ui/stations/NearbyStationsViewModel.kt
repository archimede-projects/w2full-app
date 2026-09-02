package com.archimede.w2full.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.mimit.MimitRefreshResult
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.NearbyStationsSnapshot
import com.archimede.w2full.location.UserLocationResult
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal const val MIMIT_REFRESH_ERROR_MESSAGE = "Impossibile aggiornare i prezzi al momento"

enum class NearbyLocationUiStatus {
    AVAILABLE,
    PERMISSION_DENIED,
    UNAVAILABLE,
}

data class NearbyStationsUiState(
    val isLoading: Boolean = false,
    val stations: List<MimitStationDistance> = emptyList(),
    val locationStatus: NearbyLocationUiStatus? = null,
    val extractionDate: LocalDate? = null,
    val pricesExtractionDate: LocalDate? = null,
    val lastSuccessfulUpdateEpochMillis: Long? = null,
    val errorMessage: String? = null,
)

internal fun NearbyStationsUiState.withRefreshFailure(): NearbyStationsUiState = copy(
    isLoading = false,
    errorMessage = MIMIT_REFRESH_ERROR_MESSAGE,
)

class NearbyStationsViewModel(
    private val repository: NearbyStationsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NearbyStationsUiState())
    val uiState: StateFlow<NearbyStationsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var locationJob: Job? = null
    private var hasLoadedOnce = false
    private var initialPermissionPromptConsumed = false

    init {
        viewModelScope.launch {
            repository.observeStations().collectLatest { snapshot ->
                snapshot?.let(::applySnapshot)
            }
        }
    }

    fun consumeInitialPermissionPrompt(): Boolean {
        if (initialPermissionPromptConsumed) return false
        initialPermissionPromptConsumed = true
        return true
    }

    fun loadIfNeeded() {
        if (!hasLoadedOnce) {
            refresh()
        }
    }

    fun refresh() {
        hasLoadedOnce = true
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
            )

            try {
                when (repository.refresh()) {
                    is MimitRefreshResult.Success -> {
                        repository.loadCachedSnapshot()?.let(::applySnapshot)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null,
                        )
                    }

                    is MimitRefreshResult.Failure -> {
                        repository.loadCachedSnapshot()?.let(::applySnapshot)
                        _uiState.value = _uiState.value.withRefreshFailure()
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                repository.loadCachedSnapshot()?.let(::applySnapshot)
                _uiState.value = _uiState.value.withRefreshFailure()
            }
        }
    }

    fun refreshLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                repository.loadCachedSnapshot()?.let(::applySnapshot)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    locationStatus = NearbyLocationUiStatus.UNAVAILABLE,
                )
            }
        }
    }

    private fun applySnapshot(snapshot: NearbyStationsSnapshot) {
        _uiState.value = _uiState.value.copy(
            stations = snapshot.rankedStations.stations,
            locationStatus = snapshot.rankedStations.locationResult.toUiStatus(),
            extractionDate = snapshot.extractionDate,
            pricesExtractionDate = snapshot.pricesExtractionDate,
            lastSuccessfulUpdateEpochMillis = snapshot.lastSuccessfulUpdateEpochMillis,
        )
    }

    private fun UserLocationResult.toUiStatus(): NearbyLocationUiStatus = when (this) {
        is UserLocationResult.Available -> NearbyLocationUiStatus.AVAILABLE
        UserLocationResult.PermissionDenied -> NearbyLocationUiStatus.PERMISSION_DENIED
        UserLocationResult.Unavailable -> NearbyLocationUiStatus.UNAVAILABLE
    }

    class Factory(
        private val repository: NearbyStationsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NearbyStationsViewModel::class.java))
            return NearbyStationsViewModel(repository) as T
        }
    }
}
