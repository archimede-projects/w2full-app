package com.archimede.w2full.ui.stations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

internal const val FAVORITE_TOUCH_TARGET_DP = 56

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
            favoriteStationsStore = application.historyFavoriteStationsStore,
        )
    }
    val viewModel: NearbyStationsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    val requestLocationPermission = { permissionLauncher.launch(LocationPermissions) }

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
        onScopeChanged = viewModel::setScope,
        onFavoriteToggle = viewModel::toggleFavorite,
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
    onScopeChanged: (StationListScope) -> Unit,
    onFavoriteToggle: (Long) -> Unit,
) {
    var filtersOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "W2Full",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "Stazioni Eni vicine", style = MaterialTheme.typography.titleLarge)
        }

        CompactUpdateRow(state = state, onRefresh = onRefresh)
        CompactLocationRow(
            status = state.locationStatus,
            onRequestLocationPermission = onRequestLocationPermission,
            onRetryLocation = onRetryLocation,
        )

        OutlinedButton(
            onClick = { filtersOpen = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(filterSummary(state), maxLines = 2)
        }

        state.errorMessage?.let { message ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                )
                TextButton(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Riprova") }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (state.isLoading && state.stations.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    if (state.stations.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = emptyStationMessage(state),
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    } else {
                        items(items = state.stations, key = { it.station.id }) { item ->
                            StationCard(
                                item = item,
                                selectedFuelType = state.selectedFuelType,
                                price = state.pricesByStationId[item.station.id],
                                isFavorite = item.station.id in state.favoriteStationIds,
                                onFavoriteToggle = onFavoriteToggle,
                            )
                        }
                    }
                }
            }
        }
    }

    if (filtersOpen) {
        StationFiltersDialog(
            state = state,
            onDismiss = { filtersOpen = false },
            onRadiusEnabledChanged = onRadiusEnabledChanged,
            onRadiusInputChanged = onRadiusInputChanged,
            onApplyRadius = onApplyRadius,
            onSortModeChanged = onSortModeChanged,
            onScopeChanged = onScopeChanged,
        )
    }
}

@Composable
private fun CompactUpdateRow(state: NearbyStationsUiState, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = lastUpdateLabel(state.lastSuccessfulUpdateEpochMillis),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            state.pricesExtractionDate?.let {
                Text(
                    text = "Prezzi MIMIT: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = onRefresh,
            enabled = !state.isLoading,
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(if (state.isLoading) "…" else "↻", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun CompactLocationRow(
    status: NearbyLocationUiStatus?,
    onRequestLocationPermission: () -> Unit,
    onRetryLocation: () -> Unit,
) {
    when (status) {
        NearbyLocationUiStatus.AVAILABLE -> Text(
            text = "● Posizione disponibile",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        NearbyLocationUiStatus.PERMISSION_DENIED -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Posizione non autorizzata", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRequestLocationPermission, modifier = Modifier.heightIn(min = 48.dp)) { Text("Consenti") }
        }
        NearbyLocationUiStatus.UNAVAILABLE -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Posizione non disponibile", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetryLocation, modifier = Modifier.heightIn(min = 48.dp)) { Text("Riprova") }
        }
        null -> Text("Posizione in aggiornamento…", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StationFiltersDialog(
    state: NearbyStationsUiState,
    onDismiss: () -> Unit,
    onRadiusEnabledChanged: (Boolean) -> Unit,
    onRadiusInputChanged: (String) -> Unit,
    onApplyRadius: () -> Unit,
    onSortModeChanged: (StationSortMode) -> Unit,
    onScopeChanged: (StationListScope) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtri stazioni") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mostra", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.scope == StationListScope.ALL,
                        onClick = { onScopeChanged(StationListScope.ALL) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        label = { Text("Tutte") },
                    )
                    FilterChip(
                        selected = state.scope == StationListScope.FAVORITES,
                        onClick = { onScopeChanged(StationListScope.FAVORITES) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        label = { Text("★ Preferite") },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (state.radiusEnabled) "Raggio: ${state.radiusKm} km" else "Nessun limite distanza")
                    Switch(checked = state.radiusEnabled, onCheckedChange = onRadiusEnabledChanged)
                }
                if (state.radiusEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.radiusInput,
                            onValueChange = onRadiusInputChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("km") },
                            singleLine = true,
                            isError = state.radiusInputError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onApplyRadius() }),
                        )
                        Button(onClick = onApplyRadius, modifier = Modifier.heightIn(min = 52.dp)) { Text("Applica") }
                    }
                    state.radiusInputError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }

                Text("Ordina per · ${state.selectedFuelType}", fontWeight = FontWeight.SemiBold)
                SortChoice("Distanza", StationSortMode.DISTANCE, state.sortMode, onSortModeChanged)
                SortChoice("Prezzo Self", StationSortMode.SELF_PRICE, state.sortMode, onSortModeChanged)
                SortChoice("Prezzo Servito", StationSortMode.SERVED_PRICE, state.sortMode, onSortModeChanged)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fatto") }
        },
    )
}

@Composable
private fun SortChoice(
    label: String,
    mode: StationSortMode,
    selectedMode: StationSortMode,
    onSortModeChanged: (StationSortMode) -> Unit,
) {
    FilterChip(
        selected = selectedMode == mode,
        onClick = { onSortModeChanged(mode) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        label = { Text(label) },
    )
}

@Composable
private fun StationCard(
    item: MimitStationDistance,
    selectedFuelType: String,
    price: MimitStationFuelPrice?,
    isFavorite: Boolean,
    onFavoriteToggle: (Long) -> Unit,
) {
    val station = item.station
    val title = station.name.ifBlank { "Stazione Eni #${station.id}" }
    val address = listOf(station.address, station.municipality, station.province)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "Indirizzo non disponibile" }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = { onFavoriteToggle(station.id) },
                    modifier = Modifier.size(FAVORITE_TOUCH_TARGET_DP.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        if (isFavorite) "★" else "☆",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(text = selectedFuelType, style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatStationFuelPrice(price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = item.distanceKm?.let(::formatDistanceKm) ?: "Distanza non disponibile",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun filterSummary(state: NearbyStationsUiState): String {
    val radius = if (state.radiusEnabled) "${state.radiusKm} km" else "Tutte le distanze"
    val sort = when (state.sortMode) {
        StationSortMode.DISTANCE -> "Distanza"
        StationSortMode.SELF_PRICE -> "Prezzo Self"
        StationSortMode.SERVED_PRICE -> "Prezzo Servito"
    }
    val scope = if (state.scope == StationListScope.FAVORITES) "Preferite" else "Tutte"
    return "$radius · $sort · $scope · ${state.selectedFuelType}  ▾"
}

private fun emptyStationMessage(state: NearbyStationsUiState): String = when {
    state.scope == StationListScope.FAVORITES -> "Nessuna stazione preferita nel filtro corrente."
    state.totalStationCount > 0 && state.radiusEnabled -> "Nessuna stazione Eni entro il raggio impostato."
    else -> "Nessuna stazione Eni disponibile."
}

internal fun locationStatusTitle(status: NearbyLocationUiStatus?): String = when (status) {
    NearbyLocationUiStatus.AVAILABLE -> "Posizione disponibile"
    NearbyLocationUiStatus.PERMISSION_DENIED -> "Permesso posizione negato"
    NearbyLocationUiStatus.UNAVAILABLE -> "Posizione non disponibile"
    null -> "Posizione in attesa"
}

internal fun locationStatusSubtitle(status: NearbyLocationUiStatus?): String = when (status) {
    NearbyLocationUiStatus.AVAILABLE -> "Stazioni ordinate per distanza"
    NearbyLocationUiStatus.PERMISSION_DENIED -> "Stazioni ordinate alfabeticamente"
    NearbyLocationUiStatus.UNAVAILABLE -> "Stazioni ordinate alfabeticamente"
    null -> "Verifica della posizione in corso"
}

internal fun lastUpdateLabel(
    epochMillis: Long?,
    nowEpochMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (epochMillis == null) return "Ultimo aggiornamento: non ancora disponibile"
    val elapsedMillis = (nowEpochMillis - epochMillis).coerceAtLeast(0L)
    val elapsedHours = elapsedMillis / 3_600_000L
    val relative = when {
        elapsedMillis < 3_600_000L -> "Aggiornato pochi minuti fa"
        elapsedHours < 24L -> if (elapsedHours == 1L) "Aggiornato 1 ora fa" else "Aggiornato $elapsedHours ore fa"
        else -> {
            val elapsedDays = elapsedMillis / 86_400_000L
            if (elapsedDays == 1L) "Aggiornato 1 giorno fa" else "Aggiornato $elapsedDays giorni fa"
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
        price.self?.let { add("Self ${formatPriceMilliEuro(it.priceMilliEuroPerUnit)} ${price.unit.label}") }
        price.served?.let { add("Servito ${formatPriceMilliEuro(it.priceMilliEuroPerUnit)} ${price.unit.label}") }
    }
    return values.joinToString(" · ").ifBlank { "Prezzo non disponibile" }
}

internal fun formatPriceMilliEuro(priceMilliEuroPerUnit: Long): String {
    val whole = priceMilliEuroPerUnit / 1_000L
    val fraction = priceMilliEuroPerUnit % 1_000L
    return String.format(Locale.ITALY, "%d,%03d", whole, fraction)
}

private fun formatDistanceKm(distanceKm: Double): String = String.format(Locale.ITALY, "%.1f km", distanceKm)

private fun Context.hasAnyLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
