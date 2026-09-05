package com.archimede.w2full.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MimitCacheDao {
    @Query("SELECT * FROM mimit_stations ORDER BY name, municipality, address, station_id")
    fun observeStations(): Flow<List<MimitStationEntity>>

    @Query("SELECT * FROM mimit_prices ORDER BY station_id, fuel_description, is_self, communicated_at")
    fun observePrices(): Flow<List<MimitPriceEntity>>

    @Query("SELECT DISTINCT fuel_description FROM mimit_prices ORDER BY fuel_description COLLATE NOCASE")
    fun observeFuelDescriptions(): Flow<List<String>>

    @Query("SELECT * FROM mimit_sync_state WHERE id = 1 LIMIT 1")
    fun observeSyncState(): Flow<MimitSyncStateEntity?>

    @Query(
        """
        SELECT DISTINCT s.*
        FROM mimit_stations s
        INNER JOIN mimit_price_history h ON h.station_id = s.station_id
        ORDER BY s.name COLLATE NOCASE, s.municipality COLLATE NOCASE, s.address COLLATE NOCASE, s.station_id
        """,
    )
    fun observeStationsWithHistory(): Flow<List<MimitStationEntity>>

    @Query(
        """
        SELECT DISTINCT fuel_description
        FROM mimit_price_history
        WHERE station_id = :stationId
        ORDER BY fuel_description COLLATE NOCASE
        """,
    )
    fun observeHistoryFuelDescriptions(stationId: Long): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT is_self
        FROM mimit_price_history
        WHERE station_id = :stationId AND fuel_description = :fuelDescription
        ORDER BY is_self DESC
        """,
    )
    fun observeHistoryServiceModes(stationId: Long, fuelDescription: String): Flow<List<Boolean>>

    @Query(
        """
        SELECT *
        FROM mimit_price_history
        WHERE station_id = :stationId
          AND fuel_description = :fuelDescription
          AND is_self = :isSelf
        ORDER BY observed_on_epoch_day ASC, communicated_at ASC, imported_at_epoch_millis ASC
        """,
    )
    fun observePriceHistory(
        stationId: Long,
        fuelDescription: String,
        isSelf: Boolean,
    ): Flow<List<MimitPriceHistoryEntity>>

    @Query("SELECT * FROM mimit_stations ORDER BY name, municipality, address, station_id")
    suspend fun getStations(): List<MimitStationEntity>

    @Query("SELECT * FROM mimit_prices ORDER BY station_id, fuel_description, is_self, communicated_at")
    suspend fun getPrices(): List<MimitPriceEntity>

    @Query(
        """
        SELECT * FROM mimit_price_history
        ORDER BY station_id, fuel_description, is_self, observed_on_epoch_day, communicated_at, imported_at_epoch_millis
        """,
    )
    suspend fun getPriceHistory(): List<MimitPriceHistoryEntity>

    @Query("SELECT * FROM mimit_sync_state WHERE id = 1 LIMIT 1")
    suspend fun getSyncState(): MimitSyncStateEntity?

    @Query("DELETE FROM mimit_prices")
    suspend fun clearPrices()

    @Query("DELETE FROM mimit_stations")
    suspend fun clearStations()

    @Query("DELETE FROM mimit_sync_state")
    suspend fun clearSyncState()

    @Insert
    suspend fun insertStations(stations: List<MimitStationEntity>)

    @Insert
    suspend fun insertPrices(prices: List<MimitPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPriceHistory(history: List<MimitPriceHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: MimitSyncStateEntity)
}
