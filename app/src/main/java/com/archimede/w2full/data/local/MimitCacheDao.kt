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

    @Query("SELECT * FROM mimit_sync_state WHERE id = 1 LIMIT 1")
    fun observeSyncState(): Flow<MimitSyncStateEntity?>

    @Query("SELECT * FROM mimit_stations ORDER BY name, municipality, address, station_id")
    suspend fun getStations(): List<MimitStationEntity>

    @Query("SELECT * FROM mimit_prices ORDER BY station_id, fuel_description, is_self, communicated_at")
    suspend fun getPrices(): List<MimitPriceEntity>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: MimitSyncStateEntity)
}
