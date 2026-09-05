package com.archimede.w2full.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VehicleEntity::class,
        RifornimentoEntity::class,
        MimitStationEntity::class,
        MimitPriceEntity::class,
        MimitPriceHistoryEntity::class,
        MimitSyncStateEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class W2FullDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    abstract fun rifornimentoDao(): RifornimentoDao

    abstract fun mimitCacheDao(): MimitCacheDao

    companion object {
        private const val DATABASE_NAME = "w2full.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mimit_stations` (
                        `station_id` INTEGER NOT NULL,
                        `manager` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `station_type` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `municipality` TEXT NOT NULL,
                        `province` TEXT NOT NULL,
                        `latitude` REAL,
                        `longitude` REAL,
                        PRIMARY KEY(`station_id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mimit_prices` (
                        `station_id` INTEGER NOT NULL,
                        `fuel_description` TEXT NOT NULL,
                        `price_milli_euro_per_unit` INTEGER NOT NULL,
                        `is_self` INTEGER NOT NULL,
                        `communicated_at` TEXT NOT NULL,
                        PRIMARY KEY(`station_id`, `fuel_description`, `is_self`, `communicated_at`),
                        FOREIGN KEY(`station_id`) REFERENCES `mimit_stations`(`station_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_mimit_prices_station` ON `mimit_prices` (`station_id`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mimit_sync_state` (
                        `id` INTEGER NOT NULL,
                        `stations_extraction_epoch_day` INTEGER NOT NULL,
                        `prices_extraction_epoch_day` INTEGER NOT NULL,
                        `last_successful_update_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mimit_price_history` (
                        `station_id` INTEGER NOT NULL,
                        `fuel_description` TEXT NOT NULL,
                        `price_milli_euro_per_unit` INTEGER NOT NULL,
                        `is_self` INTEGER NOT NULL,
                        `communicated_at` TEXT NOT NULL,
                        `imported_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`station_id`, `fuel_description`, `is_self`, `communicated_at`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `idx_mimit_price_history_station_fuel_service`
                    ON `mimit_price_history` (`station_id`, `fuel_description`, `is_self`, `communicated_at`)
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mimit_price_history_new` (
                        `station_id` INTEGER NOT NULL,
                        `fuel_description` TEXT NOT NULL,
                        `price_milli_euro_per_unit` INTEGER NOT NULL,
                        `is_self` INTEGER NOT NULL,
                        `observed_on_epoch_day` INTEGER NOT NULL,
                        `communicated_at` TEXT NOT NULL,
                        `imported_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`station_id`, `fuel_description`, `is_self`, `observed_on_epoch_day`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `mimit_price_history_new` (
                        `station_id`, `fuel_description`, `price_milli_euro_per_unit`, `is_self`,
                        `observed_on_epoch_day`, `communicated_at`, `imported_at_epoch_millis`
                    )
                    SELECT
                        `station_id`, `fuel_description`, `price_milli_euro_per_unit`, `is_self`,
                        CAST(`imported_at_epoch_millis` / 86400000 AS INTEGER),
                        `communicated_at`, `imported_at_epoch_millis`
                    FROM `mimit_price_history`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `mimit_price_history`")
                db.execSQL("ALTER TABLE `mimit_price_history_new` RENAME TO `mimit_price_history`")
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `idx_mimit_price_history_station_fuel_service`
                    ON `mimit_price_history` (`station_id`, `fuel_description`, `is_self`, `observed_on_epoch_day`)
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var instance: W2FullDatabase? = null

        fun getInstance(context: Context): W2FullDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    W2FullDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
