package com.archimede.w2full.data.mimit

import java.time.LocalDate
import java.time.LocalDateTime

data class MimitDataset<T>(
    val extractionDate: LocalDate,
    val rows: List<T>,
)

data class MimitStation(
    val id: Long,
    val manager: String,
    val brand: String,
    val stationType: String,
    val name: String,
    val address: String,
    val municipality: String,
    val province: String,
    val latitude: Double?,
    val longitude: Double?,
)

data class MimitPrice(
    val stationId: Long,
    val fuelDescription: String,
    val priceMilliEuroPerUnit: Long,
    val isSelf: Boolean,
    val communicatedAt: LocalDateTime,
)
