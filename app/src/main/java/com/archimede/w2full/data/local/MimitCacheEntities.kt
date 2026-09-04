package com.archimede.w2full.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mimit_stations")
data class MimitStationEntity(
    @PrimaryKey
    @ColumnInfo(name = "station_id")
    val stationId: Long,
    val manager: String,
    val brand: String,
    @ColumnInfo(name = "station_type")
    val stationType: String,
    val name: String,
    val address: String,
    val municipality: String,
    val province: String,
    val latitude: Double?,
    val longitude: Double?,
)

@Entity(
    tableName = "mimit_prices",
    primaryKeys = ["station_id", "fuel_description", "is_self", "communicated_at"],
    foreignKeys = [
        ForeignKey(
            entity = MimitStationEntity::class,
            parentColumns = ["station_id"],
            childColumns = ["station_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_mimit_prices_station", value = ["station_id"]),
    ],
)
data class MimitPriceEntity(
    @ColumnInfo(name = "station_id")
    val stationId: Long,
    @ColumnInfo(name = "fuel_description")
    val fuelDescription: String,
    @ColumnInfo(name = "price_milli_euro_per_unit")
    val priceMilliEuroPerUnit: Long,
    @ColumnInfo(name = "is_self")
    val isSelf: Boolean,
    @ColumnInfo(name = "communicated_at")
    val communicatedAt: String,
)

@Entity(
    tableName = "mimit_price_history",
    primaryKeys = ["station_id", "fuel_description", "is_self", "communicated_at"],
    indices = [
        Index(
            name = "idx_mimit_price_history_station_fuel_service",
            value = ["station_id", "fuel_description", "is_self", "communicated_at"],
        ),
    ],
)
data class MimitPriceHistoryEntity(
    @ColumnInfo(name = "station_id")
    val stationId: Long,
    @ColumnInfo(name = "fuel_description")
    val fuelDescription: String,
    @ColumnInfo(name = "price_milli_euro_per_unit")
    val priceMilliEuroPerUnit: Long,
    @ColumnInfo(name = "is_self")
    val isSelf: Boolean,
    @ColumnInfo(name = "communicated_at")
    val communicatedAt: String,
    @ColumnInfo(name = "imported_at_epoch_millis")
    val importedAtEpochMillis: Long,
)

@Entity(tableName = "mimit_sync_state")
data class MimitSyncStateEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "stations_extraction_epoch_day")
    val stationsExtractionEpochDay: Long,
    @ColumnInfo(name = "prices_extraction_epoch_day")
    val pricesExtractionEpochDay: Long,
    @ColumnInfo(name = "last_successful_update_epoch_millis")
    val lastSuccessfulUpdateEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
