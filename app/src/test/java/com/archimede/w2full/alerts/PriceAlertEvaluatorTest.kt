package com.archimede.w2full.alerts

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.archimede.w2full.data.local.MimitPriceEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.data.repository.PriceAlertConfig
import com.archimede.w2full.data.repository.PriceAlertRepository
import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.InMemoryLastForegroundLocationStore
import com.archimede.w2full.location.StoredUserLocation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
class PriceAlertEvaluatorTest {
    private lateinit var database: W2FullDatabase
    private lateinit var repository: PriceAlertRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, W2FullDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.vehicleDao().insertIfAbsent(VehicleEntity(1, "Auto", "Benzina", 50_000))
        database.mimitCacheDao().insertStations(
            listOf(
                station(1, 44.84, 11.30),
                station(2, 44.85, 11.31),
            ),
        )
        database.mimitCacheDao().insertPrices(
            listOf(price(1, 1_750), price(2, 1_900)),
        )
        repository = PriceAlertRepository(
            alertDao = database.priceAlertDao(),
            vehicleDao = database.vehicleDao(),
            cacheDao = database.mimitCacheDao(),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameQualifyingSetNotifiesOnceAndEmptySetResetsFingerprint() = runBlocking {
        repository.save(
            PriceAlertConfig("Benzina", 1_800, isSelf = true, radiusKm = null),
            isActive = true,
        )
        val notifier = FakeNotifier()
        val evaluator = PriceAlertEvaluator(
            repository,
            database.mimitCacheDao(),
            InMemoryLastForegroundLocationStore(),
            notifier,
            clock,
        )

        assertEquals(PriceAlertEvaluationOutcome.NOTIFIED, evaluator.evaluate())
        assertEquals(1, notifier.calls)
        assertEquals(PriceAlertEvaluationOutcome.UNCHANGED, evaluator.evaluate())
        assertEquals(1, notifier.calls)

        database.mimitCacheDao().clearPrices()
        assertEquals(PriceAlertEvaluationOutcome.NO_MATCH, evaluator.evaluate())
        assertEquals(null, repository.loadRuleOrDefault().lastNotifiedFingerprint)

        database.mimitCacheDao().insertPrices(listOf(price(1, 1_750)))
        assertEquals(PriceAlertEvaluationOutcome.NOTIFIED, evaluator.evaluate())
        assertEquals(2, notifier.calls)
    }

    @Test
    fun staleOrMissingLocationSkipsRadiusWithoutAntiSpamMutation() = runBlocking {
        repository.save(
            PriceAlertConfig("Benzina", 1_800, isSelf = true, radiusKm = 25),
            isActive = true,
        )
        val notifier = FakeNotifier()
        val stale = StoredUserLocation(
            GeoPoint(44.836, 11.293),
            clock.millis() - PriceAlertEvaluator.MAX_LOCATION_AGE_MILLIS - 1,
        )
        val evaluator = PriceAlertEvaluator(
            repository,
            database.mimitCacheDao(),
            InMemoryLastForegroundLocationStore(stale),
            notifier,
            clock,
        )
        assertEquals(PriceAlertEvaluationOutcome.SKIPPED_LOCATION, evaluator.evaluate())
        assertEquals(0, notifier.calls)
        assertEquals(null, repository.loadRuleOrDefault().lastNotifiedFingerprint)
    }

    @Test
    fun blockedNotificationDoesNotAdvanceFingerprint() = runBlocking {
        repository.save(
            PriceAlertConfig("Benzina", 1_800, isSelf = true, radiusKm = null),
            isActive = true,
        )
        val notifier = FakeNotifier(allowed = false)
        val evaluator = PriceAlertEvaluator(
            repository,
            database.mimitCacheDao(),
            InMemoryLastForegroundLocationStore(),
            notifier,
            clock,
        )
        assertEquals(PriceAlertEvaluationOutcome.NOTIFICATION_BLOCKED, evaluator.evaluate())
        assertEquals(null, repository.loadRuleOrDefault().lastNotifiedFingerprint)
    }

    private class FakeNotifier(private val allowed: Boolean = true) : PriceAlertNotifier {
        var calls = 0
        override fun notificationsAllowed(): Boolean = allowed
        override fun notify(rule: com.archimede.w2full.data.repository.PriceAlertRule, candidates: List<PriceAlertCandidate>): Boolean {
            calls += 1
            return allowed
        }
    }

    private fun station(id: Long, lat: Double, lon: Double) = MimitStationEntity(
        stationId = id,
        manager = "Gestore",
        brand = "Eni",
        stationType = "Stradale",
        name = "Eni $id",
        address = "Via $id",
        municipality = "Mirandola",
        province = "MO",
        latitude = lat,
        longitude = lon,
    )

    private fun price(id: Long, value: Long) = MimitPriceEntity(
        stationId = id,
        fuelDescription = "Benzina",
        priceMilliEuroPerUnit = value,
        isSelf = true,
        communicatedAt = "2026-09-05T08:00:00",
    )
}
