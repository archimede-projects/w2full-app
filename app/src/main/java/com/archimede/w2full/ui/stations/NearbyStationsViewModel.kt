package com.archimede.w2full.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.location.UserLocationResult
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val lastSuccessfulUpdateEpochMillis: Long? = null,
    val errorMessage: String? = null,
)

class NearbyStationsViewModel(
    private val repository: NearbyStationsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NearbyStationsUiState())
    val uiState: StateFlow<NearbyStationsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var hasLoadedOnce = false
    private var initialPermissionPromptConsumed = false

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
                val snapshot = repository.loadStations()
                _uiState.value = NearbyStationsUiState(
                    isLoading = false,
                    stations = snapshot.rankedStations.stations,
                    locationStatus = snapshot.rankedStations.locationResult.toUiStatus(),
                    extractionDate = snapshot.extractionDate,
                    lastSuccessfulUpdateEpochMillis = null,
                    errorMessage = null,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Impossibile caricare le stazioni al momento",
                )
            }
        }
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
