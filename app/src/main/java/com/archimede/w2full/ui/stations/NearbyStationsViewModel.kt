package com.archimede.w2full.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.mimit.EniStationDistanceRanker
import com.archimede.w2full.data.mimit.MimitRefreshResult
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.MimitStationFuelPrice
import com.archimede.w2full.data.mimit.MimitStationPriceSelector
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.NearbyStationsSnapshot
import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.LocationLabelResolver
import com.archimede.w2full.location.NoOpLocationLabelResolver
import com.archimede.w2full.location.UserLocationResult
import com.archimede.w2full.ui.history.HistoryFavoriteStationsStore
import com.archimede.w2full.ui.history.InMemoryHistoryFavoriteStationsStore
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
    val favoriteStations: List<MimitStationDistance> = emptyList(),
    val favoriteStationIds: Set<Long> = emptySet(),
    val filteredStationCount: Int = 0,
    val totalStationCount: Int = 0,
    val locationStatus: NearbyLocationUiStatus? = null,
    val locationLabel: String? = null,
    val extractionDate: LocalDate? = null,
    val pricesExtractionDate: LocalDate? = null,
    val lastSuccessfulUpdateEpochMillis: Long? = null,
    val selectedFuelType: String = MimitStationPriceSelector.FALLBACK_FUEL_TYPE,
    val pricesByStationId: Map<Long, MimitStationFuelPrice> = emptyMap(),
    val radiusEnabled: Boolean = false,
    val radiusKm: Int = StationListPreferences.DEFAULT_RADIUS_KM,
    val radiusInput: String = StationListPreferences.DEFAULT_RADIUS_KM.toString(),
    val radiusInputError: String? = null,
    val sortMode: StationSortMode = StationSortMode.DISTANCE,
    val scope: StationListScope = StationListScope.ALL,
    val errorMessage: String? = null,
)

internal data class FavoriteStationPresentation(
    val favorites: List<MimitStationDistance>,
    val regular: List<MimitStationDistance>,
)

internal fun splitStationsForFavorites(
    sourceStations: List<MimitStationDistance>,
    displayedStations: List<MimitStationDistance>,
    favoriteStationIds: Set<Long>,
): FavoriteStationPresentation = FavoriteStationPresentation(
    favorites = sourceStations.filter { it.station.id in favoriteStationIds },
    regular = displayedStations.filterNot { it.station.id in favoriteStationIds },
)

internal fun toggledFavoriteStationIds(
    current: Set<Long>,
    stationId: Long,
): Set<Long> = if (stationId in current) current - stationId else current + stationId

internal fun NearbyStationsUiState.withRefreshFailure(): NearbyStationsUiState = copy(
    isLoading = false,
    errorMessage = MIMIT_REFRESH_ERROR_MESSAGE,
)

class NearbyStationsViewModel(
    private val repository: NearbyStationsRepository,
    private val preferencesStore: StationListPreferencesStore = InMemoryStationListPreferencesStore(),
    private val favoriteStationsStore: HistoryFavoriteStationsStore = InMemoryHistoryFavoriteStationsStore(),
    private val locationLabelResolver: LocationLabelResolver = NoOpLocationLabelResolver,
) : ViewModel() {
    private var stationPreferences = preferencesStore.load()
    private var favoriteStationIds = favoriteStationsStore.load()
    private val _uiState = MutableStateFlow(
        NearbyStationsUiState(
            radiusEnabled = stationPreferences.radiusEnabled,
            radiusKm = stationPreferences.radiusKm,
            radiusInput = stationPreferences.radiusKm.toString(),
            sortMode = stationPreferences.sortMode,
            scope = stationPreferences.scope,
            favoriteStationIds = favoriteStationIds,
        ),
    )
    val uiState: StateFlow<NearbyStationsUiState> = _uiState.asStateFlow()

    private var sourceStations: List<MimitStationDistance> = emptyList()
    private var sourcePricesByStationId: Map<Long, MimitStationFuelPrice> = emptyMap()
    private var sourceLocationStatus: NearbyLocationUiStatus? = null
    private var lastAvailableLocation: GeoPoint? = null
    private var refreshJob: Job? = null
    private var locationJob: Job? = null
    private var locationLabelJob: Job? = null
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
        reloadLocalPreferences()
        if (!hasLoadedOnce) {
            refresh()
        } else {
            refreshLocation()
        }
    }

    fun reloadLocalPreferences() {
        stationPreferences = preferencesStore.load()
        favoriteStationIds = favoriteStationsStore.load()
        _uiState.value = _uiState.value.copy(
            radiusEnabled = stationPreferences.radiusEnabled,
            radiusKm = stationPreferences.radiusKm,
            radiusInput = stationPreferences.radiusKm.toString(),
            radiusInputError = null,
            sortMode = stationPreferences.sortMode,
            scope = stationPreferences.scope,
            favoriteStationIds = favoriteStationIds,
        )
        recomputeDisplayedStations()
    }

    fun refresh() {
        hasLoadedOnce = true
        refreshLocation()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                when (repository.refresh()) {
                    is MimitRefreshResult.Success -> {
                        repository.loadCachedSnapshot()?.let(::applySnapshot)
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
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
        _uiState.value = _uiState.value.copy(locationStatus = null)
        locationJob = viewModelScope.launch {
            try {
                when (val result = repository.resolveLocation()) {
                    is UserLocationResult.Available -> applyFreshLocation(result.point)
                    UserLocationResult.PermissionDenied -> applyUnavailableLocation(NearbyLocationUiStatus.PERMISSION_DENIED)
                    UserLocationResult.Unavailable -> applyUnavailableLocation(NearbyLocationUiStatus.UNAVAILABLE)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                applyUnavailableLocation(NearbyLocationUiStatus.UNAVAILABLE)
            }
        }
    }

    fun setRadiusEnabled(enabled: Boolean) {
        stationPreferences = stationPreferences.copy(radiusEnabled = enabled)
        persistPreferences()
        _uiState.value = _uiState.value.copy(radiusEnabled = enabled, radiusInputError = null)
        recomputeDisplayedStations()
    }

    fun onRadiusInputChanged(input: String) {
        if (input.length <= 3 && input.all(Char::isDigit)) {
            _uiState.value = _uiState.value.copy(radiusInput = input, radiusInputError = null)
        }
    }

    fun applyRadiusInput() {
        val input = _uiState.value.radiusInput
        if (!isValidRadiusInput(input)) {
            _uiState.value = _uiState.value.copy(radiusInputError = "Inserisci un valore da 1 a 200 km")
            return
        }
        val radiusKm = validatedRadiusOrPrevious(input, stationPreferences.radiusKm)
        stationPreferences = stationPreferences.copy(radiusKm = radiusKm)
        persistPreferences()
        _uiState.value = _uiState.value.copy(
            radiusKm = radiusKm,
            radiusInput = radiusKm.toString(),
            radiusInputError = null,
        )
        recomputeDisplayedStations()
    }

    fun setSortMode(sortMode: StationSortMode) {
        stationPreferences = stationPreferences.copy(sortMode = sortMode)
        persistPreferences()
        _uiState.value = _uiState.value.copy(sortMode = sortMode)
        recomputeDisplayedStations()
    }

    fun setScope(scope: StationListScope) {
        stationPreferences = stationPreferences.copy(scope = scope)
        persistPreferences()
        _uiState.value = _uiState.value.copy(scope = scope)
        recomputeDisplayedStations()
    }

    fun toggleFavorite(stationId: Long) {
        if (sourceStations.none { it.station.id == stationId }) return
        favoriteStationIds = toggledFavoriteStationIds(favoriteStationIds, stationId)
        favoriteStationsStore.save(favoriteStationIds)
        recomputeDisplayedStations()
    }

    private fun applySnapshot(snapshot: NearbyStationsSnapshot) {
        val rawStations = snapshot.rankedStations.stations.map { it.station }
        val snapshotLocation = snapshot.rankedStations.locationResult
        if (snapshotLocation is UserLocationResult.Available) {
            lastAvailableLocation = snapshotLocation.point
            sourceLocationStatus = NearbyLocationUiStatus.AVAILABLE
            sourceStations = EniStationDistanceRanker.rank(rawStations, snapshotLocation.point)
            resolveLocationLabel(snapshotLocation.point)
        } else if (lastAvailableLocation != null) {
            sourceLocationStatus = NearbyLocationUiStatus.AVAILABLE
            sourceStations = EniStationDistanceRanker.rank(rawStations, lastAvailableLocation)
        } else {
            sourceLocationStatus = snapshotLocation.toUiStatus()
            sourceStations = snapshot.rankedStations.stations
        }

        sourcePricesByStationId = snapshot.pricesByStationId
        _uiState.value = _uiState.value.copy(
            locationStatus = sourceLocationStatus,
            extractionDate = snapshot.extractionDate,
            pricesExtractionDate = snapshot.pricesExtractionDate,
            lastSuccessfulUpdateEpochMillis = snapshot.lastSuccessfulUpdateEpochMillis,
            selectedFuelType = snapshot.selectedFuelType,
            pricesByStationId = snapshot.pricesByStationId,
            totalStationCount = sourceStations.size,
        )
        recomputeDisplayedStations()
    }

    private fun applyFreshLocation(point: GeoPoint) {
        lastAvailableLocation = point
        sourceLocationStatus = NearbyLocationUiStatus.AVAILABLE
        sourceStations = EniStationDistanceRanker.rank(sourceStations.map { it.station }, point)
        _uiState.value = _uiState.value.copy(
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            locationLabel = null,
        )
        recomputeDisplayedStations()
        resolveLocationLabel(point)
    }

    private fun applyUnavailableLocation(status: NearbyLocationUiStatus) {
        lastAvailableLocation = null
        sourceLocationStatus = status
        sourceStations = EniStationDistanceRanker.rank(sourceStations.map { it.station }, null)
        _uiState.value = _uiState.value.copy(locationStatus = status, locationLabel = null)
        recomputeDisplayedStations()
    }

    private fun resolveLocationLabel(point: GeoPoint) {
        locationLabelJob?.cancel()
        locationLabelJob = viewModelScope.launch {
            val label = runCatching { locationLabelResolver.resolve(point) }.getOrNull()
            if (lastAvailableLocation == point && sourceLocationStatus == NearbyLocationUiStatus.AVAILABLE) {
                _uiState.value = _uiState.value.copy(locationLabel = label)
            }
        }
    }

    private fun recomputeDisplayedStations() {
        val displayedStations = filterAndSortStations(
            stations = sourceStations,
            pricesByStationId = sourcePricesByStationId,
            locationStatus = sourceLocationStatus,
            preferences = stationPreferences,
            favoriteStationIds = favoriteStationIds,
        )
        _uiState.value = _uiState.value.copy(
            stations = displayedStations,
            favoriteStations = displayedStations.filter { it.station.id in favoriteStationIds },
            favoriteStationIds = favoriteStationIds,
            filteredStationCount = displayedStations.size,
            totalStationCount = sourceStations.size,
            scope = stationPreferences.scope,
        )
    }

    private fun persistPreferences() {
        preferencesStore.save(stationPreferences)
    }

    private fun UserLocationResult.toUiStatus(): NearbyLocationUiStatus = when (this) {
        is UserLocationResult.Available -> NearbyLocationUiStatus.AVAILABLE
        UserLocationResult.PermissionDenied -> NearbyLocationUiStatus.PERMISSION_DENIED
        UserLocationResult.Unavailable -> NearbyLocationUiStatus.UNAVAILABLE
    }

    class Factory(
        private val repository: NearbyStationsRepository,
        private val preferencesStore: StationListPreferencesStore,
        private val favoriteStationsStore: HistoryFavoriteStationsStore,
        private val locationLabelResolver: LocationLabelResolver = NoOpLocationLabelResolver,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NearbyStationsViewModel::class.java))
            return NearbyStationsViewModel(
                repository = repository,
                preferencesStore = preferencesStore,
                favoriteStationsStore = favoriteStationsStore,
                locationLabelResolver = locationLabelResolver,
            ) as T
        }
    }
}
