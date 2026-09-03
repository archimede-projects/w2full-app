package com.archimede.w2full.data.mimit

import java.io.Reader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MimitCsvFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

class MimitCsvParser {
    fun parseStations(reader: Reader): MimitDataset<MimitStation> = parseStations(reader.readText())

    fun parseStations(text: String): MimitDataset<MimitStation> = parseTable(
        text = text,
        expectedHeaders = STATION_HEADERS,
        rowNormalizer = ::normalizeStationFields,
        rowMapper = { fields, rowNumber ->
            MimitStation(
                id = fields[0].requiredLong("idimpianto", rowNumber),
                manager = fields[1].trim(),
                brand = fields[2].trim(),
                stationType = fields[3].trim(),
                name = fields[4].trim(),
                address = fields[5].trim(),
                municipality = fields[6].trim(),
                province = fields[7].trim(),
                latitude = fields[8].nullableDouble("Latitudine", rowNumber),
                longitude = fields[9].nullableDouble("Longitudine", rowNumber),
            )
        },
    )

    fun parsePrices(reader: Reader): MimitDataset<MimitPrice> = parsePrices(reader.readText())

    fun parsePrices(text: String): MimitDataset<MimitPrice> = parseTable(
        text = text,
        expectedHeaders = PRICE_HEADERS,
        rowMapper = { fields, rowNumber ->
            MimitPrice(
                stationId = fields[0].requiredLong("idimpianto", rowNumber),
                fuelDescription = fields[1].trim(),
                priceMilliEuroPerUnit = fields[2].priceMilliEuro(rowNumber),
                isSelf = fields[3].selfService(rowNumber),
                communicatedAt = fields[4].communicationDateTime(rowNumber),
            )
        },
    )

    private fun <T> parseTable(
        text: String,
        expectedHeaders: List<String>,
        rowNormalizer: (List<String>, Int) -> List<String> = { fields, _ -> fields },
        rowMapper: (List<String>, Int) -> T,
    ): MimitDataset<T> {
        val rows = try {
            PipeSeparatedTextParser.parse(text)
        } catch (exception: IllegalStateException) {
            throw MimitCsvFormatException(exception.message ?: "Malformed pipe-delimited content", exception)
        }

        val headerIndex = rows.indexOfFirst { row ->
            row.size == expectedHeaders.size &&
                row.map(::normalizeHeader) == expectedHeaders.map(::normalizeHeader)
        }
        if (headerIndex < 0) {
            throw MimitCsvFormatException(
                "Expected header not found: ${expectedHeaders.joinToString("|")}",
            )
        }

        val extractionDate = rows
            .take(headerIndex)
            .asSequence()
            .flatten()
            .mapNotNull(::parseExtractionDate)
            .firstOrNull()
            ?: throw MimitCsvFormatException("Missing 'Estrazione del AAAA-MM-GG' preamble")

        val parsedRows = rows
            .drop(headerIndex + 1)
            .mapIndexed { dataIndex, fields ->
                val rowNumber = headerIndex + dataIndex + 2
                val normalizedFields = rowNormalizer(fields, rowNumber)
                if (normalizedFields.size != expectedHeaders.size) {
                    throw MimitCsvFormatException(
                        "Row $rowNumber has ${normalizedFields.size} fields; expected ${expectedHeaders.size}",
                    )
                }
                rowMapper(normalizedFields, rowNumber)
            }

        return MimitDataset(extractionDate = extractionDate, rows = parsedRows)
    }

    private fun normalizeStationFields(fields: List<String>, @Suppress("UNUSED_PARAMETER") rowNumber: Int): List<String> {
        if (fields.size <= STATION_HEADERS.size) return fields

        val normalized = fields.toMutableList()
        while (normalized.size > STATION_HEADERS.size) {
            val artifactIndex = normalized.indexOfFirst { field ->
                field.trim().equals(KNOWN_UNESCAPED_TEXT_FRAGMENT, ignoreCase = true)
            }
            if (artifactIndex <= 0) break

            normalized[artifactIndex - 1] =
                normalized[artifactIndex - 1] + "|" + normalized[artifactIndex]
            normalized.removeAt(artifactIndex)
        }
        return normalized
    }

    private fun normalizeHeader(value: String): String = value
        .removePrefix("\uFEFF")
        .trim()
        .lowercase(Locale.ROOT)

    private fun parseExtractionDate(value: String): LocalDate? {
        val match = EXTRACTION_DATE_REGEX.matchEntire(value.trim()) ?: return null
        return try {
            LocalDate.parse(match.groupValues[1], DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (exception: RuntimeException) {
            throw MimitCsvFormatException("Invalid extraction date: ${match.groupValues[1]}", exception)
        }
    }

    private fun String.requiredLong(column: String, rowNumber: Int): Long =
        trim().toLongOrNull()
            ?: throw MimitCsvFormatException("Invalid $column at row $rowNumber: '$this'")

    private fun String.nullableDouble(column: String, rowNumber: Int): Double? {
        val normalized = trim()
        if (normalized.isEmpty()) return null
        return normalized.toDoubleOrNull()
            ?: throw MimitCsvFormatException("Invalid $column at row $rowNumber: '$this'")
    }

    private fun String.priceMilliEuro(rowNumber: Int): Long {
        val normalized = trim()
        return try {
            BigDecimal(normalized).movePointRight(3).longValueExact()
        } catch (exception: RuntimeException) {
            throw MimitCsvFormatException("Invalid prezzo at row $rowNumber: '$this'", exception)
        }
    }

    private fun String.selfService(rowNumber: Int): Boolean = when (trim()) {
        "1" -> true
        "0" -> false
        else -> throw MimitCsvFormatException("Invalid isSelf at row $rowNumber: '$this'")
    }

    private fun String.communicationDateTime(rowNumber: Int): LocalDateTime = try {
        LocalDateTime.parse(trim(), COMMUNICATION_DATE_TIME_FORMAT)
    } catch (exception: RuntimeException) {
        throw MimitCsvFormatException("Invalid dtComu at row $rowNumber: '$this'", exception)
    }

    private companion object {
        const val KNOWN_UNESCAPED_TEXT_FRAGMENT = "gestori.prezzibenzina.it"

        val STATION_HEADERS = listOf(
            "idimpianto",
            "Gestore",
            "Bandiera",
            "Tipo Impianto",
            "Nome Impianto",
            "Indirizzo",
            "Comune",
            "Provincia",
            "Latitudine",
            "Longitudine",
        )

        val PRICE_HEADERS = listOf(
            "idimpianto",
            "descCarburante",
            "prezzo",
            "isSelf",
            "dtComu",
        )

        val EXTRACTION_DATE_REGEX = Regex(
            pattern = "(?i)^Estrazione\\s+del\\s+(\\d{4}-\\d{2}-\\d{2})$",
        )
        val COMMUNICATION_DATE_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss")
    }
}
