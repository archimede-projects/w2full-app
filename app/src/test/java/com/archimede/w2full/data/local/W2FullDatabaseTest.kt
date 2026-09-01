package com.archimede.w2full.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.archimede.w2full.data.repository.RefuelingRepository
import com.archimede.w2full.domain.model.RifornimentoDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class W2FullDatabaseTest {
    private lateinit var database: W2FullDatabase
    private lateinit var repository: RefuelingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, W2FullDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RefuelingRepository(database.vehicleDao(), database.rifornimentoDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `repository performs create update read delete against Room`() {
        runBlocking {
            val id = repository.insert(
                RifornimentoDraft(
                    timestampEpochMillis = 1_000,
                    odometerKm = 10_000,
                    litersMilliliters = 40_000,
                    totalCostCents = 7_000,
                    fuelType = " Benzina ",
                    isFullTank = true,
                ),
            )

            val created = repository.observeRefuelings().first().single()
            assertEquals(id, created.id)
            assertEquals("Benzina", created.fuelType)

            repository.update(created.copy(totalCostCents = 7_500))
            val updated = repository.observeRefuelings().first().single()
            assertEquals(7_500, updated.totalCostCents)

            repository.delete(id)
            assertTrue(repository.observeRefuelings().first().isEmpty())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repository rejects odometer regression over time`() {
        runBlocking {
            repository.insert(
                RifornimentoDraft(
                    timestampEpochMillis = 1_000,
                    odometerKm = 10_000,
                    litersMilliliters = 40_000,
                    totalCostCents = 7_000,
                    fuelType = "Benzina",
                    isFullTank = true,
                ),
            )
            repository.insert(
                RifornimentoDraft(
                    timestampEpochMillis = 2_000,
                    odometerKm = 9_999,
                    litersMilliliters = 10_000,
                    totalCostCents = 2_000,
                    fuelType = "Benzina",
                    isFullTank = false,
                ),
            )
        }
    }
}
