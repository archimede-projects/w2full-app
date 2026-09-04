package com.archimede.w2full.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.ui.history.HistoryFavoriteStationsStore
import com.archimede.w2full.ui.history.HistoryPeriod
import com.archimede.w2full.ui.history.HistoryPreferences
import com.archimede.w2full.ui.history.HistoryPreferencesStore
import com.archimede.w2full.ui.stations.StationListPreferences
import com.archimede.w2full.ui.stations.StationListPreferencesStore
import com.archimede.w2full.ui.stations.StationSortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FavoriteSettingsItem(
    val stationId: Long,
    val name: String,
    val address: String,
    val distanceKm: Double?,
)

data class SettingsUiState(
    val favorites: List<FavoriteSettingsItem> = emptyList(),
    val stationPreferences: StationListPreferences = StationListPreferences(),
    val historyPreferences: HistoryPreferences = HistoryPreferences(),
)

class SettingsViewModel(
    private val nearbyStationsRepository: NearbyStationsRepository,
    private val favoriteStationsStore: HistoryFavoriteStationsStore,
    private val stationPreferencesStore: StationListPreferencesStore,
    private val historyPreferencesStore: HistoryPreferencesStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            stationPreferences = stationPreferencesStore.load(),
            historyPreferences = historyPreferencesStore.load(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var latestStations: List<MimitStationDistance> = emptyList()
    private var favoriteIds: Set<Long> = favoriteStationsStore.load()

    init {
        viewModelScope.launch {
            nearbyStationsRepository.observeStations().collectLatest { snapshot ->
                latestStations = snapshot?.rankedStations?.stations.orEmpty()
                publishFavorites()
            }
        }
        reload()
    }

    fun reload() {
        favoriteIds = favoriteStationsStore.load()
        _uiState.value = _uiState.value.copy(
            stationPreferences = stationPreferencesStore.load(),
            historyPreferences = historyPreferencesStore.load(),
        )
        publishFavorites()
    }

    fun removeFavorite(stationId: Long) {
        favoriteIds = favoriteIds - stationId
        favoriteStationsStore.save(favoriteIds)
        publishFavorites()
    }

    fun setRadiusEnabled(enabled: Boolean) {
        updateStationPreferences(_uiState.value.stationPreferences.copy(radiusEnabled = enabled))
    }

    fun setRadiusKm(radiusKm: Int) {
        if (radiusKm !in StationListPreferences.MIN_RADIUS_KM..StationListPreferences.MAX_RADIUS_KM) return
        updateStationPreferences(_uiState.value.stationPreferences.copy(radiusKm = radiusKm))
    }

    fun setStationSortMode(mode: StationSortMode) {
        updateStationPreferences(_uiState.value.stationPreferences.copy(sortMode = mode))
    }

    fun setHistoryPeriod(period: HistoryPeriod) {
        updateHistoryPreferences(_uiState.value.historyPreferences.copy(period = period))
    }

    fun setSeriesBEnabled(enabled: Boolean) {
        updateHistoryPreferences(_uiState.value.historyPreferences.copy(seriesBEnabled = enabled))
    }

    fun setShowTable(show: Boolean) {
        updateHistoryPreferences(_uiState.value.historyPreferences.copy(showTable = show))
    }

    private fun updateStationPreferences(preferences: StationListPreferences) {
        stationPreferencesStore.save(preferences)
        _uiState.value = _uiState.value.copy(stationPreferences = preferences)
    }

    private fun updateHistoryPreferences(preferences: HistoryPreferences) {
        historyPreferencesStore.save(preferences)
        _uiState.value = _uiState.value.copy(historyPreferences = preferences)
    }

    private fun publishFavorites() {
        val stationById = latestStations.associateBy { it.station.id }
        val items = favoriteIds.map { id ->
            val stationDistance = stationById[id]
            FavoriteSettingsItem(
                stationId = id,
                name = stationDistance?.station?.name?.ifBlank { "Eni #$id" } ?: "Eni #$id",
                address = stationDistance?.station?.let { station ->
                    listOf(station.address, station.municipality, station.province)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                }.orEmpty(),
                distanceKm = stationDistance?.distanceKm,
            )
        }.sortedWith(
            compareBy<FavoriteSettingsItem> { it.distanceKm ?: Double.POSITIVE_INFINITY }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.stationId },
        )
        _uiState.value = _uiState.value.copy(favorites = items)
    }

    class Factory(
        private val nearbyStationsRepository: NearbyStationsRepository,
        private val favoriteStationsStore: HistoryFavoriteStationsStore,
        private val stationPreferencesStore: StationListPreferencesStore,
        private val historyPreferencesStore: HistoryPreferencesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(
                nearbyStationsRepository = nearbyStationsRepository,
                favoriteStationsStore = favoriteStationsStore,
                stationPreferencesStore = stationPreferencesStore,
                historyPreferencesStore = historyPreferencesStore,
            ) as T
        }
    }
}