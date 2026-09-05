package com.archimede.w2full.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.data.local.W2FullDatabase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PriceAlertRepositoryTest {
    private lateinit var database: W2FullDatabase
    private lateinit var repository: PriceAlertRepository

    @org.junit.Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, W2FullDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.vehicleDao().insertIfAbsent(VehicleEntity(1, "Auto", "Gasolio", null))
        repository = PriceAlertRepository(
            database.priceAlertDao(),
            database.vehicleDao(),
            database.mimitCacheDao(),
            Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun defaultUsesVehicleFuelAndConfigChangeResetsFingerprint() = runBlocking {
        assertEquals("Gasolio", repository.loadRuleOrDefault().fuelDescription)
        val config = PriceAlertConfig("Gasolio", 1_800, isSelf = true, radiusKm = 25)
        repository.save(config, isActive = true)
        repository.updateNotificationState("abc", 123L)

        repository.save(config, isActive = true)
        assertEquals("abc", repository.loadRuleOrDefault().lastNotifiedFingerprint)

        repository.save(config.copy(maxPriceMilliEuroPerUnit = 1_750), isActive = true)
        assertNull(repository.loadRuleOrDefault().lastNotifiedFingerprint)
        assertNull(repository.loadRuleOrDefault().lastNotifiedAtEpochMillis)
    }

    @Test
    fun thresholdParserAcceptsThreeDecimalsAndRejectsOutOfRangeOrExtraPrecision() {
        assertEquals(1_789L, parsePriceAlertInputToMilliEuro("1,789"))
        assertEquals(500L, parsePriceAlertInputToMilliEuro("0.500"))
        assertNull(parsePriceAlertInputToMilliEuro("0,499"))
        assertNull(parsePriceAlertInputToMilliEuro("5,001"))
        assertNull(parsePriceAlertInputToMilliEuro("1,7895"))
    }
}
