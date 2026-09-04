package com.archimede.w2full.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.mimit.NearbyStationsRepository
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
    val stationScope: HistoryStationScope = HistoryStationScope.FAVORITES,
    val distanceByStationId: Map<Long, Double?> = emptyMap(),
    val fuelTypes: List<String> = emptyList(),
    val defaultFuelType: String? = null,
    val seriesAFuelType: String? = null,
    val seriesAServiceModes: List<Boolean> = emptyList(),
    val seriesAIsSelf: Boolean? = null,
    val seriesAPoints: List<PriceHistoryPoint> = emptyList(),
    val seriesBEnabled: Boolean = false,
    val seriesBFuelType: String? = null,
    val seriesBServiceModes: List<Boolean> = emptyList(),
    val seriesBIsSelf: Boolean? = null,
    val seriesBPoints: List<PriceHistoryPoint> = emptyList(),
    val period: HistoryPeriod = HistoryPeriod.ALL,
    val showTable: Boolean = false,
    // Compatibility aliases for the original single-series M5/M7.2 tests.
    val selectedFuelType: String? = null,
    val serviceModes: List<Boolean> = emptyList(),
    val selectedIsSelf: Boolean? = null,
    val points: List<PriceHistoryPoint> = emptyList(),
)

internal fun sortHistoryStationsByDistance(
    stations: List<PriceHistoryStation>,
    distanceByStationId: Map<Long, Double?>,
): List<PriceHistoryStation> = stations.sortedWith(
    compareBy<PriceHistoryStation> { distanceByStationId[it.stationId] ?: Double.POSITIVE_INFINITY }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.ifBlank { "Eni #${it.stationId}" } }
        .thenBy { it.stationId },
)

internal fun historyStationsForScope(
    stations: List<PriceHistoryStation>,
    favoriteStationIds: Set<Long>,
    scope: HistoryStationScope,
): List<PriceHistoryStation> = when (scope) {
    HistoryStationScope.FAVORITES -> stations.filter { it.stationId in favoriteStationIds }
    HistoryStationScope.OTHERS -> stations.filterNot { it.stationId in favoriteStationIds }
}

class PriceHistoryViewModel(
    private val repository: PriceHistoryRepository,
    private val favoriteStationsStore: HistoryFavoriteStationsStore,
    private val preferencesStore: HistoryPreferencesStore = InMemoryHistoryPreferencesStore(),
    private val nearbyStationsRepository: NearbyStationsRepository? = null,
) : ViewModel() {
    private var preferences = preferencesStore.load()
    private val _uiState = MutableStateFlow(
        PriceHistoryUiState(
            favoriteStationIds = favoriteStationsStore.load(),
            stationScope = preferences.stationScope,
            seriesAFuelType = preferences.seriesAFuelType,
            seriesAIsSelf = preferences.seriesAIsSelf,
            seriesBEnabled = preferences.seriesBEnabled,
            seriesBFuelType = preferences.seriesBFuelType,
            seriesBIsSelf = preferences.seriesBIsSelf,
            period = preferences.period,
            showTable = preferences.showTable,
        ),
    )
    val uiState: StateFlow<PriceHistoryUiState> = _uiState.asStateFlow()

    private var latestStationsWithHistory: List<PriceHistoryStation> = emptyList()
    private var latestDefaultFuelType: String? = null
    private var latestDistances: Map<Long, Double?> = emptyMap()
    private var fuelJob: Job? = null
    private var seriesAServiceJob: Job? = null
    private var seriesBServiceJob: Job? = null
    private var seriesAJob: Job? = null
    private var seriesBJob: Job? = null

    init {
        if (nearbyStationsRepository != null) {
            viewModelScope.launch {
                combine(
                    repository.observeStationsWithHistory(),
                    repository.observeDefaultFuelType(),
                    nearbyStationsRepository.observeStations(),
                ) { stations, defaultFuel, nearbySnapshot ->
                    Triple(stations, defaultFuel, nearbySnapshot)
                }.collectLatest { (stations, defaultFuel, nearbySnapshot) ->
                    latestStationsWithHistory = stations
                    latestDefaultFuelType = defaultFuel
                    latestDistances = nearbySnapshot?.rankedStations?.stations
                        ?.associate { it.station.id to it.distanceKm }
                        .orEmpty()
                    applyStationScope()
                }
            }
        } else {
            viewModelScope.launch {
                combine(
                    repository.observeStationsWithHistory(),
                    repository.observeDefaultFuelType(),
                ) { stations, defaultFuel -> stations to defaultFuel }
                    .collectLatest { (stations, defaultFuel) ->
                        latestStationsWithHistory = stations
                        latestDefaultFuelType = defaultFuel
                        applyStationScope()
                    }
            }
        }
    }

    fun reloadFavorites() {
        preferences = preferencesStore.load()
        _uiState.value = _uiState.value.copy(
            favoriteStationIds = favoriteStationsStore.load(),
            stationScope = preferences.stationScope,
            seriesAFuelType = preferences.seriesAFuelType,
            seriesAIsSelf = preferences.seriesAIsSelf,
            seriesBEnabled = preferences.seriesBEnabled,
            seriesBFuelType = preferences.seriesBFuelType,
            seriesBIsSelf = preferences.seriesBIsSelf,
            period = preferences.period,
            showTable = preferences.showTable,
        )
        applyStationScope()
        val stationId = _uiState.value.selectedStationId
        if (stationId != null) {
            resetForStation(stationId)
            observeFuelTypes(stationId)
        }
    }

    fun setStationScope(scope: HistoryStationScope) {
        preferences = preferences.copy(stationScope = scope)
        persistPreferences()
        _uiState.value = _uiState.value.copy(stationScope = scope)
        applyStationScope()
    }

    fun selectStation(stationId: Long) {
        if (_uiState.value.stations.none { it.stationId == stationId }) return
        resetForStation(stationId)
        observeFuelTypes(stationId)
    }

    fun selectSeriesAFuelType(fuelType: String) {
        if (fuelType !in _uiState.value.fuelTypes) return
        preferences = preferences.copy(seriesAFuelType = fuelType)
        persistPreferences()
        _uiState.value = _uiState.value.copy(
            seriesAFuelType = fuelType,
            selectedFuelType = fuelType,
            seriesAServiceModes = emptyList(),
            serviceModes = emptyList(),
            seriesAPoints = emptyList(),
            points = emptyList(),
        )
        _uiState.value.selectedStationId?.let { observeSeriesAServiceModes(it, fuelType) }
    }

    fun selectSeriesAServiceMode(isSelf: Boolean) {
        if (isSelf !in _uiState.value.seriesAServiceModes) return
        preferences = preferences.copy(seriesAIsSelf = isSelf)
        persistPreferences()
        _uiState.value = _uiState.value.copy(
            seriesAIsSelf = isSelf,
            selectedIsSelf = isSelf,
            seriesAPoints = emptyList(),
            points = emptyList(),
        )
        val stationId = _uiState.value.selectedStationId ?: return
        val fuel = _uiState.value.seriesAFuelType ?: return
        observeSeriesA(stationId, fuel, isSelf)
    }

    fun setSeriesBEnabled(enabled: Boolean) {
        preferences = preferences.copy(seriesBEnabled = enabled)
        persistPreferences()
        _uiState.value = _uiState.value.copy(seriesBEnabled = enabled)
        if (enabled) {
            val state = _uiState.value
            val stationId = state.selectedStationId
            val fuel = state.seriesBFuelType
            if (stationId != null && fuel != null) observeSeriesBServiceModes(stationId, fuel)
        } else {
            seriesBServiceJob?.cancel()
            seriesBJob?.cancel()
            _uiState.value = _uiState.value.copy(seriesBPoints = emptyList())
        }
    }

    fun selectSeriesBFuelType(fuelType: String) {
        if (fuelType !in _uiState.value.fuelTypes) return
        preferences = preferences.copy(seriesBFuelType = fuelType)
        persistPreferences()
        _uiState.value = _uiState.value.copy(
            seriesBFuelType = fuelType,
            seriesBServiceModes = emptyList(),
            seriesBPoints = emptyList(),
        )
        _uiState.value.selectedStationId?.let { observeSeriesBServiceModes(it, fuelType) }
    }

    fun selectSeriesBServiceMode(isSelf: Boolean) {
        if (isSelf !in _uiState.value.seriesBServiceModes) return
        preferences = preferences.copy(seriesBIsSelf = isSelf)
        persistPreferences()
        _uiState.value = _uiState.value.copy(seriesBIsSelf = isSelf, seriesBPoints = emptyList())
        val stationId = _uiState.value.selectedStationId ?: return
        val fuel = _uiState.value.seriesBFuelType ?: return
        observeSeriesB(stationId, fuel, isSelf)
    }

    fun setPeriod(period: HistoryPeriod) {
        preferences = preferences.copy(period = period)
        persistPreferences()
        _uiState.value = _uiState.value.copy(period = period)
    }

    fun setShowTable(show: Boolean) {
        preferences = preferences.copy(showTable = show)
        persistPreferences()
        _uiState.value = _uiState.value.copy(showTable = show)
    }

    // Compatibility entry points for older single-series call sites/tests.
    fun selectFuelType(fuelType: String) = selectSeriesAFuelType(fuelType)
    fun selectServiceMode(isSelf: Boolean) = selectSeriesAServiceMode(isSelf)

    private fun applyStationScope() {
        val current = _uiState.value
        val candidates = historyStationsForScope(
            stations = latestStationsWithHistory,
            favoriteStationIds = current.favoriteStationIds,
            scope = current.stationScope,
        )
        val sorted = sortHistoryStationsByDistance(candidates, latestDistances)
        val selected = current.selectedStationId?.takeIf { id -> sorted.any { it.stationId == id } }
            ?: sorted.firstOrNull()?.stationId
        val stationChanged = selected != current.selectedStationId
        _uiState.value = current.copy(
            stations = sorted,
            selectedStationId = selected,
            distanceByStationId = latestDistances,
            defaultFuelType = latestDefaultFuelType,
        )
        if (stationChanged) {
            resetForStation(selected)
            selected?.let(::observeFuelTypes)
        } else if (selected != null && current.fuelTypes.isEmpty()) {
            observeFuelTypes(selected)
        }
    }

    private fun resetForStation(stationId: Long?) {
        fuelJob?.cancel()
        seriesAServiceJob?.cancel()
        seriesBServiceJob?.cancel()
        seriesAJob?.cancel()
        seriesBJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedStationId = stationId,
            fuelTypes = emptyList(),
            seriesAServiceModes = emptyList(),
            seriesBServiceModes = emptyList(),
            seriesAPoints = emptyList(),
            seriesBPoints = emptyList(),
            selectedFuelType = null,
            serviceModes = emptyList(),
            selectedIsSelf = null,
            points = emptyList(),
        )
    }

    private fun observeFuelTypes(stationId: Long) {
        fuelJob?.cancel()
        fuelJob = viewModelScope.launch {
            repository.observeFuelTypes(stationId).collectLatest { fuelTypes ->
                val state = _uiState.value
                val seriesAFuel = resolveFuel(
                    requested = preferences.seriesAFuelType ?: state.seriesAFuelType,
                    available = fuelTypes,
                    defaultFuelType = latestDefaultFuelType,
                )
                val seriesBFuel = resolveSecondFuel(
                    requested = preferences.seriesBFuelType ?: state.seriesBFuelType,
                    available = fuelTypes,
                    seriesAFuel = seriesAFuel,
                )
                _uiState.value = state.copy(
                    fuelTypes = fuelTypes,
                    seriesAFuelType = seriesAFuel,
                    selectedFuelType = seriesAFuel,
                    seriesBFuelType = seriesBFuel,
                )
                if (seriesAFuel != null) observeSeriesAServiceModes(stationId, seriesAFuel)
                if (_uiState.value.seriesBEnabled && seriesBFuel != null) {
                    observeSeriesBServiceModes(stationId, seriesBFuel)
                }
            }
        }
    }

    private fun observeSeriesAServiceModes(stationId: Long, fuelType: String) {
        seriesAServiceJob?.cancel()
        seriesAJob?.cancel()
        seriesAServiceJob = viewModelScope.launch {
            repository.observeServiceModes(stationId, fuelType).collectLatest { modes ->
                val resolved = resolveServiceMode(preferences.seriesAIsSelf, modes)
                _uiState.value = _uiState.value.copy(
                    seriesAServiceModes = modes,
                    seriesAIsSelf = resolved,
                    serviceModes = modes,
                    selectedIsSelf = resolved,
                    seriesAPoints = emptyList(),
                    points = emptyList(),
                )
                if (resolved != null) observeSeriesA(stationId, fuelType, resolved)
            }
        }
    }

    private fun observeSeriesBServiceModes(stationId: Long, fuelType: String) {
        seriesBServiceJob?.cancel()
        seriesBJob?.cancel()
        seriesBServiceJob = viewModelScope.launch {
            repository.observeServiceModes(stationId, fuelType).collectLatest { modes ->
                val resolved = resolveServiceMode(preferences.seriesBIsSelf, modes)
                _uiState.value = _uiState.value.copy(
                    seriesBServiceModes = modes,
                    seriesBIsSelf = resolved,
                    seriesBPoints = emptyList(),
                )
                if (_uiState.value.seriesBEnabled && resolved != null) {
                    observeSeriesB(stationId, fuelType, resolved)
                }
            }
        }
    }

    private fun observeSeriesA(stationId: Long, fuelType: String, isSelf: Boolean) {
        seriesAJob?.cancel()
        seriesAJob = viewModelScope.launch {
            repository.observeSeries(stationId, fuelType, isSelf).collectLatest { points ->
                _uiState.value = _uiState.value.copy(seriesAPoints = points, points = points)
            }
        }
    }

    private fun observeSeriesB(stationId: Long, fuelType: String, isSelf: Boolean) {
        seriesBJob?.cancel()
        seriesBJob = viewModelScope.launch {
            repository.observeSeries(stationId, fuelType, isSelf).collectLatest { points ->
                _uiState.value = _uiState.value.copy(seriesBPoints = points)
            }
        }
    }

    private fun persistPreferences() {
        preferencesStore.save(preferences)
    }

    private fun resolveFuel(
        requested: String?,
        available: List<String>,
        defaultFuelType: String?,
    ): String? = requested?.takeIf { requestedFuel -> available.any { it.equals(requestedFuel, true) } }
        ?.let { requestedFuel -> available.first { it.equals(requestedFuel, true) } }
        ?: defaultFuelType?.let { defaultFuel -> available.firstOrNull { it.equals(defaultFuel.trim(), true) } }
        ?: available.firstOrNull()

    private fun resolveSecondFuel(
        requested: String?,
        available: List<String>,
        seriesAFuel: String?,
    ): String? = requested?.takeIf { requestedFuel -> available.any { it.equals(requestedFuel, true) } }
        ?.let { requestedFuel -> available.first { it.equals(requestedFuel, true) } }
        ?: available.firstOrNull { !it.equals(seriesAFuel, true) }
        ?: seriesAFuel

    private fun resolveServiceMode(requestedIsSelf: Boolean, modes: List<Boolean>): Boolean? =
        requestedIsSelf.takeIf { it in modes }
            ?: true.takeIf { it in modes }
            ?: modes.firstOrNull()

    class Factory(
        private val repository: PriceHistoryRepository,
        private val favoriteStationsStore: HistoryFavoriteStationsStore,
        private val preferencesStore: HistoryPreferencesStore = InMemoryHistoryPreferencesStore(),
        private val nearbyStationsRepository: NearbyStationsRepository? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PriceHistoryViewModel::class.java))
            return PriceHistoryViewModel(
                repository = repository,
                favoriteStationsStore = favoriteStationsStore,
                preferencesStore = preferencesStore,
                nearbyStationsRepository = nearbyStationsRepository,
            ) as T
        }
    }
}