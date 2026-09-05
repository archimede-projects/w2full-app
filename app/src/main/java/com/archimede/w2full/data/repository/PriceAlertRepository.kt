package com.archimede.w2full.data.repository

import com.archimede.w2full.data.local.MimitCacheDao
import com.archimede.w2full.data.local.PriceAlertDao
import com.archimede.w2full.data.local.PriceAlertRuleEntity
import com.archimede.w2full.data.local.VehicleDao
import com.archimede.w2full.domain.model.Rifornimento
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PriceAlertRule(
    val fuelDescription: String,
    val maxPriceMilliEuroPerUnit: Long,
    val isSelf: Boolean,
    val brand: String = BRAND_ENI,
    val radiusKm: Int?,
    val isActive: Boolean,
    val lastNotifiedFingerprint: String?,
    val lastNotifiedAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val BRAND_ENI = "Eni"
    }
}

data class PriceAlertConfig(
    val fuelDescription: String,
    val maxPriceMilliEuroPerUnit: Long,
    val isSelf: Boolean,
    val radiusKm: Int?,
)

class PriceAlertRepository(
    private val alertDao: PriceAlertDao,
    private val vehicleDao: VehicleDao,
    private val cacheDao: MimitCacheDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun observeRule(): Flow<PriceAlertRule?> = alertDao.observeRule().map { it?.toDomain() }

    suspend fun loadRuleOrDefault(): PriceAlertRule = alertDao.getRule()?.toDomain() ?: defaultRule()

    suspend fun availableFuelDescriptions(): List<String> {
        val defaultFuel = vehicleDao.getById(Rifornimento.DEFAULT_VEHICLE_ID)
            ?.defaultFuelType
            ?.trim()
            .orEmpty()
            .ifBlank { DEFAULT_FUEL }
        return (cacheDao.getPrices().map { it.fuelDescription.trim() } + defaultFuel)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    suspend fun save(config: PriceAlertConfig, isActive: Boolean): PriceAlertRule {
        require(isValidPriceAlertConfig(config)) { "Invalid price alert configuration" }
        val previous = alertDao.getRule()
        val configChanged = previous == null ||
            !previous.fuelDescription.equals(config.fuelDescription.trim(), ignoreCase = true) ||
            previous.maxPriceMilliEuroPerUnit != config.maxPriceMilliEuroPerUnit ||
            previous.isSelf != config.isSelf ||
            previous.radiusKm != config.radiusKm
        val entity = PriceAlertRuleEntity(
            fuelDescription = config.fuelDescription.trim(),
            maxPriceMilliEuroPerUnit = config.maxPriceMilliEuroPerUnit,
            isSelf = config.isSelf,
            brand = PriceAlertRule.BRAND_ENI,
            radiusKm = config.radiusKm,
            isActive = isActive,
            lastNotifiedFingerprint = if (configChanged) null else previous?.lastNotifiedFingerprint,
            lastNotifiedAtEpochMillis = if (configChanged) null else previous?.lastNotifiedAtEpochMillis,
            updatedAtEpochMillis = clock.millis(),
        )
        alertDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun updateNotificationState(
        fingerprint: String?,
        notifiedAtEpochMillis: Long?,
    ): PriceAlertRule? {
        val current = alertDao.getRule() ?: return null
        val updated = current.copy(
            lastNotifiedFingerprint = fingerprint,
            lastNotifiedAtEpochMillis = notifiedAtEpochMillis,
            updatedAtEpochMillis = clock.millis(),
        )
        alertDao.upsert(updated)
        return updated.toDomain()
    }

    private suspend fun defaultRule(): PriceAlertRule {
        val fuel = vehicleDao.getById(Rifornimento.DEFAULT_VEHICLE_ID)
            ?.defaultFuelType
            ?.trim()
            .orEmpty()
            .ifBlank { DEFAULT_FUEL }
        return PriceAlertRule(
            fuelDescription = fuel,
            maxPriceMilliEuroPerUnit = DEFAULT_MAX_PRICE_MILLI_EURO,
            isSelf = true,
            radiusKm = DEFAULT_RADIUS_KM,
            isActive = false,
            lastNotifiedFingerprint = null,
            lastNotifiedAtEpochMillis = null,
            updatedAtEpochMillis = clock.millis(),
        )
    }

    private fun PriceAlertRuleEntity.toDomain(): PriceAlertRule = PriceAlertRule(
        fuelDescription = fuelDescription,
        maxPriceMilliEuroPerUnit = maxPriceMilliEuroPerUnit,
        isSelf = isSelf,
        brand = brand,
        radiusKm = radiusKm,
        isActive = isActive,
        lastNotifiedFingerprint = lastNotifiedFingerprint,
        lastNotifiedAtEpochMillis = lastNotifiedAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    companion object {
        const val MIN_PRICE_MILLI_EURO = 500L
        const val MAX_PRICE_MILLI_EURO = 5_000L
        const val DEFAULT_MAX_PRICE_MILLI_EURO = 1_800L
        const val MIN_RADIUS_KM = 1
        const val MAX_RADIUS_KM = 200
        const val DEFAULT_RADIUS_KM = 25
        const val DEFAULT_FUEL = "Benzina"
    }
}

fun isValidPriceAlertConfig(config: PriceAlertConfig): Boolean =
    config.fuelDescription.isNotBlank() &&
        config.maxPriceMilliEuroPerUnit in PriceAlertRepository.MIN_PRICE_MILLI_EURO..PriceAlertRepository.MAX_PRICE_MILLI_EURO &&
        (config.radiusKm == null || config.radiusKm in PriceAlertRepository.MIN_RADIUS_KM..PriceAlertRepository.MAX_RADIUS_KM)

fun parsePriceAlertInputToMilliEuro(input: String): Long? {
    val normalized = input.trim().replace(',', '.')
    val value = normalized.toBigDecimalOrNull() ?: return null
    if (value.scale() > 3) return null
    val milli = value.multiply(BigDecimal(1000)).setScale(0, RoundingMode.UNNECESSARY)
    return runCatching { milli.longValueExact() }.getOrNull()
        ?.takeIf { it in PriceAlertRepository.MIN_PRICE_MILLI_EURO..PriceAlertRepository.MAX_PRICE_MILLI_EURO }
}
