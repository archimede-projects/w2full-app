package com.archimede.w2full.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val HistoryDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)

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
) {
    var stationPickerOpen by remember { mutableStateOf(false) }
    var compareOpen by remember { mutableStateOf(false) }
    var dataOpen by remember { mutableStateOf(false) }

    val selectedStation = state.stations.firstOrNull { it.stationId == state.selectedStationId }
    val now = LocalDateTime.now()
    val seriesA = remember(state.seriesAPoints, state.period, now.toLocalDate()) {
        filterHistoryPointsByPeriod(state.seriesAPoints, state.period, now)
    }
    val seriesB = remember(state.seriesBPoints, state.period, state.seriesBEnabled, now.toLocalDate()) {
        if (state.seriesBEnabled) filterHistoryPointsByPeriod(state.seriesBPoints, state.period, now) else emptyList()
    }
    val labelA = seriesLabel(state.seriesAFuelType, state.seriesAIsSelf)
    val labelB = seriesLabel(state.seriesBFuelType, state.seriesBIsSelf)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Storico prezzi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (selectedStation == null) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (state.stationScope == HistoryStationScope.FAVORITES) {
                            "Nessuna preferita con storico"
                        } else {
                            "Nessuna stazione con storico"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Puoi cambiare gruppo e scegliere una stazione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { stationPickerOpen = true },
                        modifier = Modifier.padding(top = 12.dp).heightIn(min = 52.dp),
                    ) { Text("Cambia stazione") }
                }
            }
        } else {
            StationSummary(
                station = selectedStation,
                distanceKm = state.distanceByStationId[selectedStation.stationId],
                isFavorite = selectedStation.stationId in state.favoriteStationIds,
                onChange = { stationPickerOpen = true },
            )

            PriceSummary(seriesA = seriesA, labelA = labelA)

            HistoryChartPanel(
                seriesA = seriesA,
                seriesB = seriesB,
                labelA = labelA,
                labelB = labelB,
                seriesBEnabled = state.seriesBEnabled,
                resetKey = state.period,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            PeriodRow(selected = state.period, onPeriodChanged = onPeriodChanged)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { compareOpen = true },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Text(if (state.seriesBEnabled) "Confronto ✓" else "Confronta")
                }
                OutlinedButton(
                    onClick = { dataOpen = true },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Text("Mostra dati")
                }
            }
        }
    }

    if (stationPickerOpen) {
        StationPickerDialog(
            state = state,
            onScopeChanged = onStationScopeChanged,
            onStationSelected = {
                onStationSelected(it)
                stationPickerOpen = false
            },
            onDismiss = { stationPickerOpen = false },
        )
    }

    if (compareOpen) {
        CompareDialog(
            state = state,
            onSeriesAFuelSelected = onSeriesAFuelSelected,
            onSeriesAServiceSelected = onSeriesAServiceSelected,
            onSeriesBEnabledChanged = onSeriesBEnabledChanged,
            onSeriesBFuelSelected = onSeriesBFuelSelected,
            onSeriesBServiceSelected = onSeriesBServiceSelected,
            onDismiss = { compareOpen = false },
        )
    }

    if (dataOpen) {
        HistoryDataDialog(
            seriesA = seriesA,
            seriesB = seriesB,
            labelA = labelA,
            labelB = labelB,
            showSeriesB = state.seriesBEnabled,
            onDismiss = { dataOpen = false },
        )
    }
}

@Composable
private fun StationSummary(
    station: PriceHistoryStation,
    distanceKm: Double?,
    isFavorite: Boolean,
    onChange: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    (if (isFavorite) "★ " else "") + station.name.ifBlank { "Eni #${station.stationId}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                val subtitle = buildList {
                    if (station.municipality.isNotBlank()) add(station.municipality)
                    distanceKm?.let { add(String.format(Locale.ITALY, "%.1f km", it)) }
                }.joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onChange, modifier = Modifier.heightIn(min = 52.dp)) { Text("Cambia") }
        }
    }
}

@Composable
private fun PriceSummary(seriesA: List<PriceHistoryPoint>, labelA: String) {
    val latest = seriesA.lastOrNull()
    val previous = seriesA.getOrNull(seriesA.lastIndex - 1)
    val min = seriesA.minOfOrNull { it.priceMilliEuroPerUnit }
    val max = seriesA.maxOfOrNull { it.priceMilliEuroPerUnit }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(labelA, style = MaterialTheme.typography.labelLarge)
            Text(
                latest?.let { formatPrice(it.priceMilliEuroPerUnit) } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(previousDeltaLabel(latest, previous), style = MaterialTheme.typography.bodySmall)
            if (min != null && max != null) {
                Text("min ${formatPrice(min)} · max ${formatPrice(max)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HistoryChartPanel(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
    labelA: String,
    labelB: String,
    seriesBEnabled: Boolean,
    resetKey: Any?,
    modifier: Modifier = Modifier,
) {
    val hasTrend = seriesA.size >= 2 || seriesB.size >= 2
    Card(modifier = modifier) {
        if (!hasTrend) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Storico in costruzione", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Servono almeno due osservazioni giornaliere per disegnare un andamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (seriesA.isNotEmpty() || seriesB.isNotEmpty()) {
                    Text(
                        "${seriesA.size} punto/i $labelA" + if (seriesBEnabled) " · ${seriesB.size} punto/i $labelB" else "",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            InteractivePriceHistoryChart(
                seriesA = seriesA,
                seriesB = seriesB,
                labelA = labelA,
                labelB = labelB,
                showSeriesB = seriesBEnabled,
                resetKey = resetKey,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        }
    }
}

@Composable
private fun PeriodRow(selected: HistoryPeriod, onPeriodChanged: (HistoryPeriod) -> Unit) {
    val periods = listOf(
        "7g" to HistoryPeriod.SEVEN_DAYS,
        "30g" to HistoryPeriod.THIRTY_DAYS,
        "3m" to HistoryPeriod.THREE_MONTHS,
        "1a" to HistoryPeriod.ONE_YEAR,
        "Tutto" to HistoryPeriod.ALL,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        periods.forEach { (label, period) ->
            FilterChip(
                selected = selected == period || (label == "30g" && selected == HistoryPeriod.ONE_MONTH),
                onClick = { onPeriodChanged(period) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun StationPickerDialog(
    state: PriceHistoryUiState,
    onScopeChanged: (HistoryStationScope) -> Unit,
    onStationSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambia stazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.stationScope == HistoryStationScope.FAVORITES,
                        onClick = { onScopeChanged(HistoryStationScope.FAVORITES) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        label = { Text("★ Preferite") },
                    )
                    FilterChip(
                        selected = state.stationScope == HistoryStationScope.OTHERS,
                        onClick = { onScopeChanged(HistoryStationScope.OTHERS) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        label = { Text("Altre") },
                    )
                }
                if (state.stations.isEmpty()) {
                    Text(
                        if (state.stationScope == HistoryStationScope.FAVORITES) {
                            "Nessuna preferita con storico."
                        } else {
                            "Nessun'altra stazione con storico."
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(state.stations, key = { it.stationId }) { station ->
                            val distance = state.distanceByStationId[station.stationId]
                            OutlinedButton(
                                onClick = { onStationSelected(station.stationId) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(station.name.ifBlank { "Eni #${station.stationId}" }, maxLines = 1)
                                    distance?.let {
                                        Text(String.format(Locale.ITALY, "%.1f km", it), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Chiudi") } },
    )
}

@Composable
private fun CompareDialog(
    state: PriceHistoryUiState,
    onSeriesAFuelSelected: (String) -> Unit,
    onSeriesAServiceSelected: (Boolean) -> Unit,
    onSeriesBEnabledChanged: (Boolean) -> Unit,
    onSeriesBFuelSelected: (String) -> Unit,
    onSeriesBServiceSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configura confronto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactSeriesEditor(
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
                    Text("Seconda serie", fontWeight = FontWeight.SemiBold)
                    Switch(checked = state.seriesBEnabled, onCheckedChange = onSeriesBEnabledChanged)
                }
                if (state.seriesBEnabled) {
                    CompactSeriesEditor(
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
        },
        confirmButton = { Button(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fatto") } },
    )
}

@Composable
private fun CompactSeriesEditor(
    title: String,
    fuelTypes: List<String>,
    selectedFuelType: String?,
    serviceModes: List<Boolean>,
    selectedIsSelf: Boolean?,
    onFuelSelected: (String) -> Unit,
    onServiceSelected: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        FuelDropdown(fuelTypes, selectedFuelType, onFuelSelected)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            serviceModes.forEach { isSelf ->
                FilterChip(
                    selected = selectedIsSelf == isSelf,
                    onClick = { onServiceSelected(isSelf) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    label = { Text(if (isSelf) "Self" else "Servito") },
                )
            }
        }
    }
}

@Composable
private fun FuelDropdown(
    fuelTypes: List<String>,
    selectedFuelType: String?,
    onFuelSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = fuelTypes.isNotEmpty(),
        ) {
            Text((selectedFuelType ?: "Carburante") + "  ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fuelTypes.forEach { fuel ->
                DropdownMenuItem(
                    text = { Text(fuel) },
                    onClick = {
                        expanded = false
                        onFuelSelected(fuel)
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryDataDialog(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
    labelA: String,
    labelB: String,
    showSeriesB: Boolean,
    onDismiss: () -> Unit,
) {
    val rows = remember(seriesA, seriesB) { mergeHistorySeriesRows(seriesA, seriesB) }.asReversed()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dati storici") },
        text = {
            if (rows.isEmpty()) {
                Text("Nessuna osservazione nel periodo selezionato.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(rows, key = { it.observedOn.toEpochDay() }) { row ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(row.observedOn.format(HistoryDayFormatter), fontWeight = FontWeight.SemiBold)
                                Text("$labelA: ${row.seriesAPriceMilliEuroPerUnit?.let(::formatPrice) ?: "—"}")
                                if (showSeriesB) {
                                    Text("$labelB: ${row.seriesBPriceMilliEuroPerUnit?.let(::formatPrice) ?: "—"}")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Chiudi") } },
    )
}

private fun previousDeltaLabel(latest: PriceHistoryPoint?, previous: PriceHistoryPoint?): String {
    if (latest == null) return "Nessun dato"
    if (previous == null) return "Prima osservazione"
    val delta = latest.priceMilliEuroPerUnit - previous.priceMilliEuroPerUnit
    return when {
        delta == 0L -> "= precedente"
        delta > 0L -> "+${String.format(Locale.ITALY, "%.3f", delta / 1_000.0)} €"
        else -> "${String.format(Locale.ITALY, "%.3f", delta / 1_000.0)} €"
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
