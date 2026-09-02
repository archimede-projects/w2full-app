package com.archimede.w2full.data.local

import android.content.Context
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
    fun migration1To2PreservesVehicleAndRefuelingDataAndCreatesEmptyMimitCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "w2full-migration-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        val sqlite = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
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
            .addMigrations(W2FullDatabase.MIGRATION_1_2)
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
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
