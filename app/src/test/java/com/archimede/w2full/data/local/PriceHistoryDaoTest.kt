package com.archimede.w2full.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PriceHistoryDaoTest {
    private lateinit var database: W2FullDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, W2FullDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun historyIsDeduplicatedAndFilteredSeriesIsChronological() = runBlocking {
        val dao = database.mimitCacheDao()
        dao.insertStations(
            listOf(
                MimitStationEntity(
                    stationId = 10,
                    manager = "Gestore",
                    brand = "Eni",
                    stationType = "Stradale",
                    name = "Eni Uno",
                    address = "Via Uno",
                    municipality = "Roma",
                    province = "RM",
                    latitude = 41.9,
                    longitude = 12.5,
                ),
            ),
        )

        val older = MimitPriceHistoryEntity(
            stationId = 10,
            fuelDescription = "Benzina",
            priceMilliEuroPerUnit = 1_750,
            isSelf = true,
            communicatedAt = "2026-09-01T08:00:00",
            importedAtEpochMillis = 100,
        )
        val newer = older.copy(
            priceMilliEuroPerUnit = 1_790,
            communicatedAt = "2026-09-02T08:00:00",
            importedAtEpochMillis = 200,
        )
        val served = newer.copy(
            priceMilliEuroPerUnit = 1_890,
            isSelf = false,
            importedAtEpochMillis = 300,
        )

        dao.insertPriceHistory(listOf(newer, older, served))
        dao.insertPriceHistory(listOf(older.copy(importedAtEpochMillis = 999)))

        val all = dao.getPriceHistory()
        assertEquals(3, all.size)
        assertEquals(listOf(10L), dao.observeStationsWithHistory().first().map { it.stationId })
        assertEquals(listOf("Benzina"), dao.observeHistoryFuelDescriptions(10).first())
        assertEquals(listOf(true, false), dao.observeHistoryServiceModes(10, "Benzina").first())

        val selfSeries = dao.observePriceHistory(10, "Benzina", true).first()
        assertEquals(listOf(1_750L, 1_790L), selfSeries.map { it.priceMilliEuroPerUnit })
        assertEquals(listOf("2026-09-01T08:00:00", "2026-09-02T08:00:00"), selfSeries.map { it.communicatedAt })
        assertEquals(100L, selfSeries.first().importedAtEpochMillis)
    }
}
