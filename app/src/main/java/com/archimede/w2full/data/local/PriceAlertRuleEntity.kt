package com.archimede.w2full.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alert_rule")
data class PriceAlertRuleEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "fuel_description")
    val fuelDescription: String,
    @ColumnInfo(name = "max_price_milli_euro_per_unit")
    val maxPriceMilliEuroPerUnit: Long,
    @ColumnInfo(name = "is_self")
    val isSelf: Boolean,
    val brand: String,
    @ColumnInfo(name = "radius_km")
    val radiusKm: Int?,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "last_notified_fingerprint")
    val lastNotifiedFingerprint: String?,
    @ColumnInfo(name = "last_notified_at_epoch_millis")
    val lastNotifiedAtEpochMillis: Long?,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
