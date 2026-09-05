package com.archimede.w2full.alerts

import com.archimede.w2full.data.local.MimitCacheDao
import com.archimede.w2full.data.repository.PriceAlertRepository
import com.archimede.w2full.location.LastForegroundLocationStore
import com.archimede.w2full.location.StoredUserLocation
import java.time.Clock

enum class PriceAlertEvaluationOutcome {
    INACTIVE,
    SKIPPED_LOCATION,
    NO_MATCH,
    UNCHANGED,
    NOTIFIED,
    NOTIFICATION_BLOCKED,
}

class PriceAlertEvaluator(
    private val repository: PriceAlertRepository,
    private val cacheDao: MimitCacheDao,
    private val locationStore: LastForegroundLocationStore,
    private val notifier: PriceAlertNotifier,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun evaluate(): PriceAlertEvaluationOutcome {
        val rule = repository.loadRuleOrDefault()
        if (!rule.isActive) return PriceAlertEvaluationOutcome.INACTIVE

        val now = clock.millis()
        val storedLocation = locationStore.load()?.takeIf { it.isFresh(now) }
        if (rule.radiusKm != null && storedLocation == null) {
            return PriceAlertEvaluationOutcome.SKIPPED_LOCATION
        }

        val selection = selectPriceAlertCandidates(
            rule = rule,
            stations = cacheDao.getStations(),
            prices = cacheDao.getPrices(),
            location = storedLocation?.point,
        )
        if (selection.candidates.isEmpty()) {
            if (rule.lastNotifiedFingerprint != null) {
                repository.updateNotificationState(
                    fingerprint = null,
                    notifiedAtEpochMillis = rule.lastNotifiedAtEpochMillis,
                )
            }
            return PriceAlertEvaluationOutcome.NO_MATCH
        }

        val fingerprint = requireNotNull(selection.fingerprint)
        if (fingerprint == rule.lastNotifiedFingerprint) {
            return PriceAlertEvaluationOutcome.UNCHANGED
        }
        if (!notifier.notificationsAllowed()) {
            return PriceAlertEvaluationOutcome.NOTIFICATION_BLOCKED
        }
        if (!notifier.notify(rule, selection.candidates)) {
            return PriceAlertEvaluationOutcome.NOTIFICATION_BLOCKED
        }

        repository.updateNotificationState(fingerprint, now)
        return PriceAlertEvaluationOutcome.NOTIFIED
    }

    private fun StoredUserLocation.isFresh(nowEpochMillis: Long): Boolean {
        val age = nowEpochMillis - observedAtEpochMillis
        return age in 0..MAX_LOCATION_AGE_MILLIS
    }

    companion object {
        const val MAX_LOCATION_AGE_MILLIS = 24L * 60L * 60L * 1000L
    }
}
