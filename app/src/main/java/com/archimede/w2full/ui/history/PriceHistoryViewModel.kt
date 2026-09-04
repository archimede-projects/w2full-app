package com.archimede.w2full.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.repository.PriceHistoryPoint
import com.archimede.w2full.data.repository.PriceHistoryRepository
import com.archimede.w2full.data.repository.PriceHistoryStation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PriceHistoryUiState(
    val stations: List<PriceHistoryStation> = emptyList(),
    val selectedStationId: Long? = null,
    val favoriteStationIds: Set<Long> = emptySet(),
    val fuelTypes: List<String> = emptyList(),
    val selectedFuelType: String? = null,
    val serviceModes: List<Boolean> = emptyList(),
    val selectedIsSelf: Boolean? = null,
    val points: List<PriceHistoryPoint> = emptyList(),
    val defaultFuelType: String? = null,
)

class PriceHistoryViewModel(
    private val repository: PriceHistoryRepository,
    private val favoriteStationsStore: HistoryFavoriteStationsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PriceHistoryUiState(
            favoriteStationIds = favoriteStationsStore.load(),
        ),
    )
    val uiState: StateFlow<PriceHistoryUiState> = _uiState.asStateFlow()

    private var fuelJob: Job? = null
    private var serviceJob: Job? = null
    private var seriesJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeStationsWithHistory(),
                repository.observeDefaultFuelType(),
            ) { stations, defaultFuel -> stations to defaultFuel }
                .collectLatest { (stations, defaultFuel) ->
                    val currentState = _uiState.value
                    val resolvedStation = resolveHistoryStationSelection(
                        currentStationId = currentState.selectedStationId,
                        stations = stations,
                        favoriteStationIds = currentState.favoriteStationIds,
                    )

                    _uiState.value = currentState.copy(
                        stations = stations,
                        selectedStationId = resolvedStation,
                        defaultFuelType = defaultFuel,
                    )
                    observeFuelTypes(resolvedStation, defaultFuel)
                }
        }
    }

    fun selectStation(stationId: Long) {
        if (_uiState.value.stations.none { it.stationId == stationId }) return
        _uiState.value = _uiState.value.copy(
            selectedStationId = stationId,
            fuelTypes = emptyList(),
            selectedFuelType = null,
            serviceModes = emptyList(),
            selectedIsSelf = null,
            points = emptyList(),
        )
        observeFuelTypes(stationId, _uiState.value.defaultFuelType)
    }

    fun toggleFavorite(stationId: Long) {
        val state = _uiState.value
        if (state.stations.none { it.stationId == stationId }) return
        val updatedFavorites = toggledHistoryFavoriteStationIds(
            current = state.favoriteStationIds,
            stationId = stationId,
        )
        favoriteStationsStore.save(updatedFavorites)
        _uiState.value = state.copy(favoriteStationIds = updatedFavorites)
    }

    fun selectFuelType(fuelType: String) {
        if (fuelType !in _uiState.value.fuelTypes) return
        val stationId = _uiState.value.selectedStationId ?: return
        _uiState.value = _uiState.value.copy(
            selectedFuelType = fuelType,
            serviceModes = emptyList(),
            selectedIsSelf = null,
            points = emptyList(),
        )
        observeServiceModes(stationId, fuelType)
    }

    fun selectServiceMode(isSelf: Boolean) {
        val state = _uiState.value
        if (isSelf !in state.serviceModes) return
        val stationId = state.selectedStationId ?: return
        val fuelType = state.selectedFuelType ?: return
        _uiState.value = state.copy(
            selectedIsSelf = isSelf,
            points = emptyList(),
        )
        observeSeries(stationId, fuelType, isSelf)
    }

    private fun observeFuelTypes(stationId: Long?, defaultFuelType: String?) {
        fuelJob?.cancel()
        serviceJob?.cancel()
        seriesJob?.cancel()
        if (stationId == null) {
            _uiState.value = _uiState.value.copy(
                fuelTypes = emptyList(),
                selectedFuelType = null,
                serviceModes = emptyList(),
                selectedIsSelf = null,
                points = emptyList(),
            )
            return
        }

        fuelJob = viewModelScope.launch {
            repository.observeFuelTypes(stationId).collectLatest { fuelTypes ->
                val currentFuel = _uiState.value.selectedFuelType
                val resolvedFuel = currentFuel
                    ?.takeIf { it in fuelTypes }
                    ?: fuelTypes.firstOrNull {
                        defaultFuelType != null && it.equals(defaultFuelType.trim(), ignoreCase = true)
                    }
                    ?: fuelTypes.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    fuelTypes = fuelTypes,
                    selectedFuelType = resolvedFuel,
                    serviceModes = emptyList(),
                    selectedIsSelf = null,
                    points = emptyList(),
                )
                if (resolvedFuel != null) {
                    observeServiceModes(stationId, resolvedFuel)
                }
            }
        }
    }

    private fun observeServiceModes(stationId: Long, fuelType: String) {
        serviceJob?.cancel()
        seriesJob?.cancel()
        serviceJob = viewModelScope.launch {
            repository.observeServiceModes(stationId, fuelType).collectLatest { modes ->
                val currentMode = _uiState.value.selectedIsSelf
                val resolvedMode = currentMode
                    ?.takeIf { it in modes }
                    ?: true.takeIf { it in modes }
                    ?: modes.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    serviceModes = modes,
                    selectedIsSelf = resolvedMode,
                    points = emptyList(),
                )
                if (resolvedMode != null) {
                    observeSeries(stationId, fuelType, resolvedMode)
                }
            }
        }
    }

    private fun observeSeries(stationId: Long, fuelType: String, isSelf: Boolean) {
        seriesJob?.cancel()
        seriesJob = viewModelScope.launch {
            repository.observeSeries(stationId, fuelType, isSelf).collectLatest { points ->
                _uiState.value = _uiState.value.copy(points = points)
            }
        }
    }

    class Factory(
        private val repository: PriceHistoryRepository,
        private val favoriteStationsStore: HistoryFavoriteStationsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PriceHistoryViewModel::class.java))
            return PriceHistoryViewModel(repository, favoriteStationsStore) as T
        }
    }
}
