package com.archimede.w2full.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.data.repository.PriceHistoryPoint
import com.archimede.w2full.data.repository.PriceHistoryStation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY)

@Composable
fun PriceHistoryRoute() {
    val application = LocalContext.current.applicationContext as W2FullApplication
    val factory = remember(application) {
        PriceHistoryViewModel.Factory(
            repository = application.priceHistoryRepository,
            favoriteStationsStore = application.historyFavoriteStationsStore,
            preferencesStore = application.historyPreferencesStore,
            nearbyStationsRepository = application.nearbyStationsRepository,
        )
    }
    val viewModel: PriceHistoryViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.reloadFavorites() }

    PriceHistoryScreen(
        state = state,
        onStationScopeChanged = viewModel::setStationScope,
        onStationSelected = viewModel::selectStation,
        onSeriesAFuelSelected = viewModel::selectSeriesAFuelType,
        onSeriesAServiceSelected = viewModel::selectSeriesAServiceMode,
        onSeriesBEnabledChanged = viewModel::setSeriesBEnabled,
        onSeriesBFuelSelected = viewModel::selectSeriesBFuelType,
        onSeriesBServiceSelected = viewModel::selectSeriesBServiceMode,
        onPeriodChanged = viewModel::setPeriod,
        onShowTableChanged = viewModel::setShowTable,
    )
}

@Composable
private fun PriceHistoryScreen(
    state: PriceHistoryUiState,
    onStationScopeChanged: (HistoryStationScope) -> Unit,
    onStationSelected: (Long) -> Unit,
    onSeriesAFuelSelected: (String) -> Unit,
    onSeriesAServiceSelected: (Boolean) -> Unit,
    onSeriesBEnabledChanged: (Boolean) -> Unit,
    onSeriesBFuelSelected: (String) -> Unit,
    onSeriesBServiceSelected: (Boolean) -> Unit,
    onPeriodChanged: (HistoryPeriod) -> Unit,
    onShowTableChanged: (Boolean) -> Unit,
) {
    val selectedStation = state.stations.firstOrNull { it.stationId == state.selectedStationId }
    val now = remember { LocalDateTime.now() }
    val seriesA = remember(state.seriesAPoints, state.period, now) {
        filterHistoryPointsByPeriod(state.seriesAPoints, state.period, now)
    }
    val seriesB = remember(state.seriesBPoints, state.period, state.seriesBEnabled, now) {
        if (state.seriesBEnabled) filterHistoryPointsByPeriod(state.seriesBPoints, state.period, now) else emptyList()
    }
    val labelA = seriesLabel(state.seriesAFuelType, state.seriesAIsSelf)
    val labelB = seriesLabel(state.seriesBFuelType, state.seriesBIsSelf)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
                    text = "Confronta gli andamenti MIMIT della stazione che scegli.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            HistoryFilterSection(title = "Stazioni") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.stationScope == HistoryStationScope.FAVORITES,
                        onClick = { onStationScopeChanged(HistoryStationScope.FAVORITES) },
                        label = { Text("★ Preferite") },
                    )
                    FilterChip(
                        selected = state.stationScope == HistoryStationScope.OTHERS,
                        onClick = { onStationScopeChanged(HistoryStationScope.OTHERS) },
                        label = { Text("Altre") },
                    )
                }
            }
        }

        if (state.stations.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.stationScope == HistoryStationScope.FAVORITES) {
                            "Nessuna stazione preferita con storico. Aggiungila dalla schermata Stazioni."
                        } else {
                            "Nessun'altra stazione con storico disponibile."
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            item {
                HistoryStationSelector(
                    stations = state.stations,
                    selectedStationId = state.selectedStationId,
                    distanceByStationId = state.distanceByStationId,
                    onStationSelected = onStationSelected,
                )
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Configura grafico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SeriesConfigurator(
                            title = "Serie A",
                            fuelTypes = state.fuelTypes,
                            selectedFuelType = state.seriesAFuelType,
                            serviceModes = state.seriesAServiceModes,
                            selectedIsSelf = state.seriesAIsSelf,
                            onFuelSelected = onSeriesAFuelSelected,
                            onServiceSelected = onSeriesAServiceSelected,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Mostra Serie B", style = MaterialTheme.typography.labelLarge)
                            Switch(
                                checked = state.seriesBEnabled,
                                onCheckedChange = onSeriesBEnabledChanged,
                            )
                        }

                        if (state.seriesBEnabled) {
                            SeriesConfigurator(
                                title = "Serie B",
                                fuelTypes = state.fuelTypes,
                                selectedFuelType = state.seriesBFuelType,
                                serviceModes = state.seriesBServiceModes,
                                selectedIsSelf = state.seriesBIsSelf,
                                onFuelSelected = onSeriesBFuelSelected,
                                onServiceSelected = onSeriesBServiceSelected,
                            )
                        }
                    }
                }
            }

            item {
                HistoryFilterSection(title = "Periodo") {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PeriodChip("1 mese", HistoryPeriod.ONE_MONTH, state.period, onPeriodChanged)
                        PeriodChip("3 mesi", HistoryPeriod.THREE_MONTHS, state.period, onPeriodChanged)
                        PeriodChip("6 mesi", HistoryPeriod.SIX_MONTHS, state.period, onPeriodChanged)
                        PeriodChip("1 anno", HistoryPeriod.ONE_YEAR, state.period, onPeriodChanged)
                        PeriodChip("Tutto", HistoryPeriod.ALL, state.period, onPeriodChanged)
                    }
                }
            }

            if (seriesA.isEmpty() && seriesB.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Nessun punto disponibile per le serie e il periodo selezionati.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                item {
                    PriceHistoryChartCard(
                        seriesA = seriesA,
                        seriesB = seriesB,
                        labelA = labelA,
                        labelB = labelB,
                        seriesBEnabled = state.seriesBEnabled,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Tabella dati",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Switch(checked = state.showTable, onCheckedChange = onShowTableChanged)
                    }
                }

                if (state.showTable) {
                    val rows = mergeHistorySeriesRows(seriesA, seriesB)
                    item {
                        HistoryTableHeader(labelA, if (state.seriesBEnabled) labelB else null)
                    }
                    items(rows.asReversed(), key = { it.communicatedAt.toString() }) { row ->
                        HistoryTableDataRow(
                            row = row,
                            showSeriesB = state.seriesBEnabled,
                        )
                    }
                }
            }
        }

        item { Column(modifier = Modifier.height(4.dp)) {} }
    }
}

@Composable
private fun HistoryStationSelector(
    stations: List<PriceHistoryStation>,
    selectedStationId: Long?,
    distanceByStationId: Map<Long, Double?>,
    onStationSelected: (Long) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stations, key = { it.stationId }) { station ->
            val base = station.name.ifBlank { "Eni #${station.stationId}" }
            val distance = distanceByStationId[station.stationId]
            val label = if (distance != null) "$base (${String.format(Locale.ITALY, "%.1f", distance)} km)" else base
            FilterChip(
                selected = station.stationId == selectedStationId,
                onClick = { onStationSelected(station.stationId) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun SeriesConfigurator(
    title: String,
    fuelTypes: List<String>,
    selectedFuelType: String?,
    serviceModes: List<Boolean>,
    selectedIsSelf: Boolean?,
    onFuelSelected: (String) -> Unit,
    onServiceSelected: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Carburante", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fuelTypes) { fuel ->
                FilterChip(
                    selected = fuel == selectedFuelType,
                    onClick = { onFuelSelected(fuel) },
                    label = { Text(fuel) },
                )
            }
        }
        Text("Servizio", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            serviceModes.forEach { isSelf ->
                FilterChip(
                    selected = isSelf == selectedIsSelf,
                    onClick = { onServiceSelected(isSelf) },
                    label = { Text(if (isSelf) "Self" else "Servito") },
                )
            }
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    period: HistoryPeriod,
    selected: HistoryPeriod,
    onPeriodChanged: (HistoryPeriod) -> Unit,
) {
    FilterChip(
        selected = period == selected,
        onClick = { onPeriodChanged(period) },
        label = { Text(label) },
    )
}

@Composable
private fun HistoryFilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun PriceHistoryChartCard(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
    labelA: String,
    labelB: String,
    seriesBEnabled: Boolean,
) {
    val normalized = remember(seriesA, seriesB) { normalizeHistorySeries(seriesA, seriesB) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (normalized.minPrice != null && normalized.maxPrice != null) {
                Text(
                    text = "${formatPrice(normalized.minPrice)} – ${formatPrice(normalized.maxPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            PriceHistoryChart(normalized)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("● $labelA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                if (seriesBEnabled) {
                    Text("● $labelB", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = "${seriesA.size} punti Serie A" + if (seriesBEnabled) " · ${seriesB.size} punti Serie B" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PriceHistoryChart(normalized: NormalizedHistorySeries) {
    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
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

        fun drawSeries(points: List<NormalizedPricePoint>, color: Color) {
            val canvasPoints = points.map { point ->
                Offset(
                    x = horizontalPadding + point.xFraction * plotWidth,
                    y = verticalPadding + point.yFraction * plotHeight,
                )
            }
            canvasPoints.zipWithNext().forEach { (start, end) ->
                drawLine(color = color, start = start, end = end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            canvasPoints.forEach { point ->
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = point)
            }
        }

        drawSeries(normalized.seriesA, colorA)
        drawSeries(normalized.seriesB, colorB)
    }
}

@Composable
private fun HistoryTableHeader(labelA: String, labelB: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Data · $labelA" + (labelB?.let { " · $it" } ?: ""), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HistoryTableDataRow(row: HistoryTableRow, showSeriesB: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(row.communicatedAt.format(HistoryDateFormatter), style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Serie A: ${row.seriesAPriceMilliEuroPerUnit?.let(::formatPrice) ?: "—"}" +
                    if (showSeriesB) " · Serie B: ${row.seriesBPriceMilliEuroPerUnit?.let(::formatPrice) ?: "—"}" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun seriesLabel(fuelType: String?, isSelf: Boolean?): String {
    val fuel = fuelType ?: "Serie"
    val service = when (isSelf) {
        true -> "Self"
        false -> "Servito"
        null -> ""
    }
    return listOf(fuel, service).filter { it.isNotBlank() }.joinToString(" ")
}

private fun formatPrice(priceMilliEuroPerUnit: Long): String =
    String.format(Locale.ITALY, "€ %.3f/l", priceMilliEuroPerUnit / 1_000.0)