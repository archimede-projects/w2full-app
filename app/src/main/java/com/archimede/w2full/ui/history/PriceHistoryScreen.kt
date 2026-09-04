package com.archimede.w2full.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.data.repository.PriceHistoryPoint
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY)

@Composable
fun PriceHistoryRoute() {
    val application = LocalContext.current.applicationContext as W2FullApplication
    val factory = remember(application) {
        PriceHistoryViewModel.Factory(application.priceHistoryRepository)
    }
    val viewModel: PriceHistoryViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PriceHistoryScreen(
        state = state,
        onStationSelected = viewModel::selectStation,
        onFuelSelected = viewModel::selectFuelType,
        onServiceSelected = viewModel::selectServiceMode,
    )
}

@Composable
private fun PriceHistoryScreen(
    state: PriceHistoryUiState,
    onStationSelected: (Long) -> Unit,
    onFuelSelected: (String) -> Unit,
    onServiceSelected: (Boolean) -> Unit,
) {
    val selectedStation = state.stations.firstOrNull { it.stationId == state.selectedStationId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Storico prezzi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Andamento locale dei prezzi importati dai dataset MIMIT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.stations.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nessuno storico disponibile. Esegui almeno un aggiornamento MIMIT dalla schermata Stazioni.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            return@LazyColumn
        }

        item {
            HistoryFilterSection(title = "Stazione") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.stations, key = { it.stationId }) { station ->
                        FilterChip(
                            selected = station.stationId == state.selectedStationId,
                            onClick = { onStationSelected(station.stationId) },
                            label = {
                                Text(station.name.ifBlank { "Eni #${station.stationId}" })
                            },
                        )
                    }
                }
            }
        }

        selectedStation?.let { station ->
            item {
                Text(
                    text = listOf(station.address, station.municipality, station.province)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            HistoryFilterSection(title = "Carburante") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.fuelTypes) { fuel ->
                        FilterChip(
                            selected = fuel == state.selectedFuelType,
                            onClick = { onFuelSelected(fuel) },
                            label = { Text(fuel) },
                        )
                    }
                }
            }
        }

        item {
            HistoryFilterSection(title = "Servizio") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.serviceModes) { isSelf ->
                        FilterChip(
                            selected = isSelf == state.selectedIsSelf,
                            onClick = { onServiceSelected(isSelf) },
                            label = { Text(if (isSelf) "Self" else "Servito") },
                        )
                    }
                }
            }
        }

        if (state.points.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nessun punto disponibile per la selezione corrente.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            item {
                PriceHistoryChartCard(state.points)
            }
            item {
                Text(
                    text = "Rilevazioni",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.points.asReversed(), key = { "${it.communicatedAt}-${it.importedAtEpochMillis}" }) { point ->
                HistoryPointRow(point)
            }
        }

        item { Column(modifier = Modifier.height(4.dp)) {} }
    }
}

@Composable
private fun HistoryFilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun PriceHistoryChartCard(points: List<PriceHistoryPoint>) {
    val minPrice = points.minOf { it.priceMilliEuroPerUnit }
    val maxPrice = points.maxOf { it.priceMilliEuroPerUnit }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${formatPrice(minPrice)} – ${formatPrice(maxPrice)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PriceHistoryChart(points)
            Text(
                text = if (points.size == 1) {
                    "1 rilevazione"
                } else {
                    "${points.size} rilevazioni · dalla più vecchia alla più recente"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PriceHistoryChart(points: List<PriceHistoryPoint>) {
    val normalized = remember(points) {
        normalizePriceHistory(points.map { it.priceMilliEuroPerUnit })
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        val horizontalPadding = 12.dp.toPx()
        val verticalPadding = 14.dp.toPx()
        val plotWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
        val plotHeight = (size.height - verticalPadding * 2).coerceAtLeast(1f)

        repeat(3) { index ->
            val y = verticalPadding + plotHeight * index / 2f
            drawLine(
                color = gridColor,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val canvasPoints = normalized.map { point ->
            Offset(
                x = horizontalPadding + point.xFraction * plotWidth,
                y = verticalPadding + point.yFraction * plotHeight,
            )
        }

        canvasPoints.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        canvasPoints.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = point,
            )
        }
    }
}

@Composable
private fun HistoryPointRow(point: PriceHistoryPoint) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = formatPrice(point.priceMilliEuroPerUnit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = point.communicatedAt.format(HistoryDateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatPrice(priceMilliEuroPerUnit: Long): String =
    String.format(Locale.ITALY, "€ %.3f/l", priceMilliEuroPerUnit / 1_000.0)
