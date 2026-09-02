package com.archimede.w2full.data.mimit

import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NearbyStationsSnapshot(
    val extractionDate: LocalDate,
    val rankedStations: RankedEniStations,
)

interface NearbyStationsRepository {
    suspend fun loadStations(): NearbyStationsSnapshot
}

class SessionNearbyStationsRepository(
    private val stationSource: MimitStationsDataSource,
    private val distanceService: EniStationDistanceService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NearbyStationsRepository {
    override suspend fun loadStations(): NearbyStationsSnapshot = withContext(ioDispatcher) {
        val dataset = stationSource.downloadStations()
        NearbyStationsSnapshot(
            extractionDate = dataset.extractionDate,
            rankedStations = distanceService.rank(dataset.rows),
        )
    }
}
