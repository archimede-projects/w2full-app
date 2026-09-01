package com.archimede.w2full.ui.refueling

import com.archimede.w2full.domain.model.RifornimentoDraft
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object RefuelingFormParser {
    fun formatDate(
        timestampEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = Instant.ofEpochMilli(timestampEpochMillis)
        .atZone(zoneId)
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun parseDraft(
        dateText: String,
        odometerText: String,
        litersText: String,
        totalCostText: String,
        fuelTypeText: String,
        isFullTank: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): RifornimentoDraft {
        val timestamp = try {
            LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            throw IllegalArgumentException("Data non valida: usa AAAA-MM-GG.")
        }
        require(timestamp > 0) { "La data deve essere successiva al 1970." }

        val odometerKm = odometerText.trim().toLongOrNull()
            ?: throw IllegalArgumentException("Odometro non valido.")
        require(odometerKm >= 0) { "L'odometro non può essere negativo." }

        val litersMilliliters = decimalToScaledLong(litersText, 3, "Litri")
        require(litersMilliliters > 0) { "I litri devono essere positivi." }

        val totalCostCents = decimalToScaledLong(totalCostText, 2, "Costo")
        require(totalCostCents > 0) { "Il costo deve essere positivo." }

        val fuelType = fuelTypeText.trim()
        require(fuelType.isNotEmpty()) { "Il tipo carburante è obbligatorio." }

        return RifornimentoDraft(
            timestampEpochMillis = timestamp,
            odometerKm = odometerKm,
            litersMilliliters = litersMilliliters,
            totalCostCents = totalCostCents,
            fuelType = fuelType,
            isFullTank = isFullTank,
        )
    }

    fun parseTankCapacityMilliliters(text: String): Long {
        val capacity = decimalToScaledLong(text, 3, "Capacità")
        require(capacity > 0) { "La capacità deve essere positiva." }
        return capacity
    }

    private fun decimalToScaledLong(
        raw: String,
        scale: Int,
        label: String,
    ): Long {
        val normalized = raw.trim().replace(',', '.')
        val decimal = normalized.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("$label non valido.")
        return try {
            decimal
                .movePointRight(scale)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("$label fuori intervallo.")
        }
    }
}
