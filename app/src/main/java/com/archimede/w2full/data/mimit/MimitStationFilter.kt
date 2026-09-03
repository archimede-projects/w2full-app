package com.archimede.w2full.data.mimit

import java.util.Locale

object MimitStationFilter {
    fun eniStations(stations: List<MimitStation>): List<MimitStation> =
        stations.filter { isEniBrand(it.brand) }

    fun eniStations(dataset: MimitDataset<MimitStation>): MimitDataset<MimitStation> =
        dataset.copy(rows = eniStations(dataset.rows))

    fun isEniBrand(brand: String): Boolean = normalizeBrand(brand) in ENI_BRANDS

    private fun normalizeBrand(brand: String): String = brand
        .trim()
        .replace(WHITESPACE, " ")
        .lowercase(Locale.ROOT)

    private val ENI_BRANDS = setOf("eni", "agip eni")
    private val WHITESPACE = Regex("\\s+")
}
