package com.archimede.w2full.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class W2FullDatabaseMigrationTest {
    @Test
    fun migration1To4PreservesVehicleAndRefuelingDataAndCreatesMimitTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "w2full-migration-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        val sqlite = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createBaseSchema(sqlite)
        sqlite.execSQL(
            "INSERT INTO vehicles(id, name, default_fuel_type, tank_capacity_milliliters) VALUES(1, 'Auto', 'Benzina', 50000)",
        )
        sqlite.execSQL(
            """
            INSERT INTO refuel_entries(
                id, vehicle_id, timestamp_epoch_millis, odometer_km,
                liters_milliliters, total_cost_cents, fuel_type, is_full_tank
            ) VALUES(7, 1, 1000, 12345, 40000, 7000, 'Benzina', 1)
            """.trimIndent(),
        )
        sqlite.version = 1
        sqlite.close()

        val database = Room.databaseBuilder(context, W2FullDatabase::class.java, databaseName)
            .addMigrations(
                W2FullDatabase.MIGRATION_1_2,
                W2FullDatabase.MIGRATION_2_3,
                W2FullDatabase.MIGRATION_3_4,
            )
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                val vehicle = database.vehicleDao().getById(1)
                val refueling = database.rifornimentoDao().getById(7)
                assertEquals("Auto", vehicle?.name)
                assertEquals(50_000L, vehicle?.tankCapacityMilliliters)
                assertEquals(12_345L, refueling?.odometerKm)
                assertEquals(7_000L, refueling?.totalCostCents)
                assertTrue(database.mimitCacheDao().getStations().isEmpty())
                assertTrue(database.mimitCacheDao().getPrices().isEmpty())
                assertTrue(database.mimitCacheDao().getPriceHistory().isEmpty())
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration2To4PreservesMimitCacheAndCreatesEmptyDailyHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "w2full-migration-m5-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        val sqlite = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createBaseSchema(sqlite)
        createMimitV2Schema(sqlite)
        sqlite.execSQL(
            "INSERT INTO vehicles(id, name, default_fuel_type, tank_capacity_milliliters) VALUES(1, 'Auto', 'Benzina', NULL)",
        )
        sqlite.execSQL(
            """
            INSERT INTO mimit_stations(
                station_id, manager, brand, station_type, name, address,
                municipality, province, latitude, longitude
            ) VALUES(123, 'Gestore', 'Eni', 'Stradale', 'Eni Test', 'Via Test', 'Roma', 'RM', 41.9, 12.5)
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO mimit_prices(
                station_id, fuel_description, price_milli_euro_per_unit, is_self, communicated_at
            ) VALUES(123, 'Benzina', 1789, 1, '2026-09-01T08:00:00')
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO mimit_sync_state(
                id, stations_extraction_epoch_day, prices_extraction_epoch_day, last_successful_update_epoch_millis
            ) VALUES(1, 20697, 20697, 1788250000000)
            """.trimIndent(),
        )
        sqlite.version = 2
        sqlite.close()

        val database = Room.databaseBuilder(context, W2FullDatabase::class.java, databaseName)
            .addMigrations(W2FullDatabase.MIGRATION_2_3, W2FullDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                assertEquals(listOf(123L), database.mimitCacheDao().getStations().map { it.stationId })
                assertEquals(1_789L, database.mimitCacheDao().getPrices().single().priceMilliEuroPerUnit)
                assertTrue(database.mimitCacheDao().getPriceHistory().isEmpty())
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration3To4PreservesLegacyHistoryAsObservedDay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "w2full-migration-daily-history-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        val importedAt = 1_788_250_000_000L
        val sqlite = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        createBaseSchema(sqlite)
        createMimitV2Schema(sqlite)
        createMimitV3HistorySchema(sqlite)
        sqlite.execSQL(
            """
            INSERT INTO mimit_price_history(
                station_id, fuel_description, price_milli_euro_per_unit, is_self,
                communicated_at, imported_at_epoch_millis
            ) VALUES(321, 'Benzina', 2069, 1, '2026-09-01T08:00:00', $importedAt)
            """.trimIndent(),
        )
        sqlite.version = 3
        sqlite.close()

        val database = Room.databaseBuilder(context, W2FullDatabase::class.java, databaseName)
            .addMigrations(W2FullDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                val history = database.mimitCacheDao().getPriceHistory()
                assertEquals(1, history.size)
                assertEquals(321L, history.single().stationId)
                assertEquals(2_069L, history.single().priceMilliEuroPerUnit)
                assertEquals(importedAt / 86_400_000L, history.single().observedOnEpochDay)
                assertEquals("2026-09-01T08:00:00", history.single().communicatedAt)
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createBaseSchema(sqlite: SQLiteDatabase) {
        sqlite.execSQL(
            """
            CREATE TABLE `vehicles` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `default_fuel_type` TEXT NOT NULL,
                `tank_capacity_milliliters` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE `refuel_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicle_id` INTEGER NOT NULL,
                `timestamp_epoch_millis` INTEGER NOT NULL,
                `odometer_km` INTEGER NOT NULL,
                `liters_milliliters` INTEGER NOT NULL,
                `total_cost_cents` INTEGER NOT NULL,
                `fuel_type` TEXT NOT NULL,
                `is_full_tank` INTEGER NOT NULL,
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            "CREATE INDEX `idx_refuel_vehicle_odometer` ON `refuel_entries` (`vehicle_id`, `odometer_km`)",
        )
        sqlite.execSQL(
            "CREATE INDEX `idx_refuel_vehicle_timestamp` ON `refuel_entries` (`vehicle_id`, `timestamp_epoch_millis`)",
        )
    }

    private fun createMimitV2Schema(sqlite: SQLiteDatabase) {
        sqlite.execSQL(
            """
            CREATE TABLE `mimit_stations` (
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
        sqlite.execSQL(
            """
            CREATE TABLE `mimit_prices` (
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
        sqlite.execSQL(
            "CREATE INDEX `idx_mimit_prices_station` ON `mimit_prices` (`station_id`)",
        )
        sqlite.execSQL(
            """
            CREATE TABLE `mimit_sync_state` (
                `id` INTEGER NOT NULL,
                `stations_extraction_epoch_day` INTEGER NOT NULL,
                `prices_extraction_epoch_day` INTEGER NOT NULL,
                `last_successful_update_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }

    private fun createMimitV3HistorySchema(sqlite: SQLiteDatabase) {
        sqlite.execSQL(
            """
            CREATE TABLE `mimit_price_history` (
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
        sqlite.execSQL(
            """
            CREATE INDEX `idx_mimit_price_history_station_fuel_service`
            ON `mimit_price_history` (`station_id`, `fuel_description`, `is_self`, `communicated_at`)
            """.trimIndent(),
        )
    }
}
