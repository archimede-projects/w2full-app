package com.archimede.w2full.ui.stations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.MimitStationFuelPrice
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

@Composable
fun NearbyStationsRoute() {
    val context = LocalContext.current
    val application = context.applicationContext as W2FullApplication
    val factory = remember(application) {
        NearbyStationsViewModel.Factory(
            repository = application.nearbyStationsRepository,
            preferencesStore = application.stationListPreferencesStore,
        )
    }
    val viewModel: NearbyStationsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refresh()
    }

    val requestLocationPermission = {
        permissionLauncher.launch(LocationPermissions)
    }

    LaunchedEffect(viewModel) {
        if (context.hasAnyLocationPermission()) {
            viewModel.loadIfNeeded()
        } else if (viewModel.consumeInitialPermissionPrompt()) {
            requestLocationPermission()
        } else {
            viewModel.loadIfNeeded()
        }
    }

    NearbyStationsScreen(
        state = state,
        onRequestLocationPermission = requestLocationPermission,
        onRetryLocation = viewModel::refreshLocation,
        onRefresh = viewModel::refresh,
        onRadiusEnabledChanged = viewModel::setRadiusEnabled,
        onRadiusInputChanged = viewModel::onRadiusInputChanged,
        onApplyRadius = viewModel::applyRadiusInput,
        onSortModeChanged = viewModel::setSortMode,
    )
}

@Composable
private fun NearbyStationsScreen(
    state: NearbyStationsUiState,
    onRequestLocationPermission: () -> Unit,
    onRetryLocation: () -> Unit,
    onRefresh: () -> Unit,
    onRadiusEnabledChanged: (Boolean) -> Unit,
    onRadiusInputChanged: (String) -> Unit,
    onApplyRadius: () -> Unit,
    onSortModeChanged: (StationSortMode) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "W2Full",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Stazioni Eni vicine",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        item {
            UpdateStatusCard(
                state = state,
                onRefresh = onRefresh,
            )
        }

        item {
            LocationStatusCard(
                status = state.locationStatus,
                onRequestLocationPermission = onRequestLocationPermission,
                onRetryLocation = onRetryLocation,
            )
        }

        item {
            StationFiltersCard(
                state = state,
                onRadiusEnabledChanged = onRadiusEnabledChanged,
                onRadiusInputChanged = onRadiusInputChanged,
                onApplyRadius = onApplyRadius,
                onSortModeChanged = onSortModeChanged,
            )
        }

        if (state.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(message)
                        TextButton(onClick = onRefresh) {
                            Text("Riprova aggiornamento")
                        }
                    }
                }
            }
        }

        if (state.stations.isEmpty() && !state.isLoading && state.errorMessage == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.totalStationCount > 0 && state.radiusEnabled) {
                            "Nessuna stazione Eni entro il raggio impostato."
                        } else {
                            "Nessuna stazione Eni disponibile."
                        },
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        } else {
            items(
                items = state.stations,
                key = { it.station.id },
            ) { item ->
                StationCard(
                    item = item,
                    selectedFuelType = state.selectedFuelType,
                    price = state.pricesByStationId[item.station.id],
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(
    state: NearbyStationsUiState,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = lastUpdateLabel(state.lastSuccessfulUpdateEpochMillis),
                style = MaterialTheme.typography.labelLarge,
            )
            state.extractionDate?.let { extractionDate ->
                Text(
                    text = "Estrazione anagrafica: $extractionDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.pricesExtractionDate?.let { extractionDate ->
                Text(
                    text = "Estrazione prezzi: $extractionDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onRefresh,
                enabled = !state.isLoading,
            ) {
                Text(if (state.isLoading) "Aggiornamento…" else "Aggiorna")
            }
        }
    }
}

@Composable
private fun LocationStatusCard(
    status: NearbyLocationUiStatus?,
    onRequestLocationPermission: () -> Unit,
    onRetryLocation: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = locationStatusTitle(status),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = locationStatusSubtitle(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (status) {
                NearbyLocationUiStatus.PERMISSION_DENIED -> {
                    Button(onClick = onRequestLocationPermission) {
                        Text("Consenti posizione")
                    }
                }

                NearbyLocationUiStatus.UNAVAILABLE -> {
                    TextButton(onClick = onRetryLocation) {
                        Text("Riprova posizione")
                    }
                }

                NearbyLocationUiStatus.AVAILABLE,
                null,
                -> Unit
            }
        }
    }
}

@Composable
private fun StationFiltersCard(
    state: NearbyStationsUiState,
    onRadiusEnabledChanged: (Boolean) -> Unit,
    onRadiusInputChanged: (String) -> Unit,
    onApplyRadius: () -> Unit,
    onSortModeChanged: (StationSortMode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Filtri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Limita distanza", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (state.radiusEnabled) {
                            "Raggio massimo: ${state.radiusKm} km"
                        } else {
                            "Nessun limite"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.radiusEnabled,
                    onCheckedChange = onRadiusEnabledChanged,
                )
            }

            if (state.radiusEnabled) {
                OutlinedTextField(
                    value = state.radiusInput,
                    onValueChange = onRadiusInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Raggio massimo (km)") },
                    singleLine = true,
                    isError = state.radiusInputError != null,
                    supportingText = state.radiusInputError?.let { message ->
                        { Text(message) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onApplyRadius() }),
                )
                Button(onClick = onApplyRadius) {
                    Text("Applica raggio")
                }

                if (state.locationStatus != NearbyLocationUiStatus.AVAILABLE) {
                    Text(
                        text = "Il raggio verrà applicato quando la posizione sarà disponibile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Ordina per", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortChip(
                    label = "Distanza",
                    mode = StationSortMode.DISTANCE,
                    selectedMode = state.sortMode,
                    onSortModeChanged = onSortModeChanged,
                )
                SortChip(
                    label = "Prezzo Self",
                    mode = StationSortMode.SELF_PRICE,
                    selectedMode = state.sortMode,
                    onSortModeChanged = onSortModeChanged,
                )
                SortChip(
                    label = "Prezzo Servito",
                    mode = StationSortMode.SERVED_PRICE,
                    selectedMode = state.sortMode,
                    onSortModeChanged = onSortModeChanged,
                )
            }

            Text(
                text = "Mostrate ${state.stations.size} di ${state.totalStationCount} stazioni · ${state.selectedFuelType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    mode: StationSortMode,
    selectedMode: StationSortMode,
    onSortModeChanged: (StationSortMode) -> Unit,
) {
    FilterChip(
        selected = selectedMode == mode,
        onClick = { onSortModeChanged(mode) },
        label = { Text(label) },
    )
}

@Composable
private fun StationCard(
    item: MimitStationDistance,
    selectedFuelType: String,
    price: MimitStationFuelPrice?,
) {
    val station = item.station
    val title = station.name.ifBlank { "Stazione Eni #${station.id}" }
    val address = listOf(
        station.address,
        station.municipality,
        station.province,
    ).filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "Indirizzo non disponibile" }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = selectedFuelType,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = formatStationFuelPrice(price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.distanceKm?.let(::formatDistanceKm) ?: "Distanza non disponibile",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

internal fun locationStatusTitle(status: NearbyLocationUiStatus?): String = when (status) {
    NearbyLocationUiStatus.AVAILABLE -> "Posizione disponibile"
    NearbyLocationUiStatus.PERMISSION_DENIED -> "Permesso posizione negato"
    NearbyLocationUiStatus.UNAVAILABLE -> "Posizione non disponibile"
    null -> "Posizione in attesa"
}

internal fun locationStatusSubtitle(status: NearbyLocationUiStatus?): String = when (status) {
    NearbyLocationUiStatus.AVAILABLE -> "Distanze disponibili per filtro e ordinamento"
    NearbyLocationUiStatus.PERMISSION_DENIED -> "Il filtro raggio richiede la posizione"
    NearbyLocationUiStatus.UNAVAILABLE -> "Il filtro raggio richiede la posizione"
    null -> "Verifica della posizione in corso"
}

internal fun lastUpdateLabel(
    epochMillis: Long?,
    nowEpochMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (epochMillis == null) {
        return "Ultimo aggiornamento: non ancora disponibile"
    }

    val elapsedMillis = (nowEpochMillis - epochMillis).coerceAtLeast(0L)
    val elapsedHours = elapsedMillis / 3_600_000L
    val relative = when {
        elapsedMillis < 3_600_000L -> "Aggiornato pochi minuti fa"
        elapsedHours < 24L -> if (elapsedHours == 1L) {
            "Aggiornato 1 ora fa"
        } else {
            "Aggiornato $elapsedHours ore fa"
        }
        else -> {
            val elapsedDays = elapsedMillis / 86_400_000L
            if (elapsedDays == 1L) {
                "Aggiornato 1 giorno fa"
            } else {
                "Aggiornato $elapsedDays giorni fa"
            }
        }
    }
    val absolute = Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    return "$relative · $absolute"
}

internal fun formatStationFuelPrice(price: MimitStationFuelPrice?): String {
    if (price == null) return "Prezzo non disponibile"
    val values = buildList {
        price.self?.let {
            add("Self ${formatPriceMilliEuro(it.priceMilliEuroPerUnit)} ${price.unit.label}")
        }
        price.served?.let {
            add("Servito ${formatPriceMilliEuro(it.priceMilliEuroPerUnit)} ${price.unit.label}")
        }
    }
    return values.joinToString(" · ").ifBlank { "Prezzo non disponibile" }
}

internal fun formatPriceMilliEuro(priceMilliEuroPerUnit: Long): String {
    val whole = priceMilliEuroPerUnit / 1_000L
    val fraction = priceMilliEuroPerUnit % 1_000L
    return String.format(Locale.ITALY, "%d,%03d", whole, fraction)
}

private fun formatDistanceKm(distanceKm: Double): String =
    String.format(Locale.ITALY, "%.1f km", distanceKm)

private fun Context.hasAnyLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
