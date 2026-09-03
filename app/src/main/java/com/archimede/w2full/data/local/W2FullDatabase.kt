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
        MimitSyncStateEntity::class,
    ],
    version = 2,
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

        @Volatile
        private var instance: W2FullDatabase? = null

        fun getInstance(context: Context): W2FullDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    W2FullDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
