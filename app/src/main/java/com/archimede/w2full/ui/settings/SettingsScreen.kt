package com.archimede.w2full.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.BuildConfig
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.ui.history.HistoryPeriod
import com.archimede.w2full.ui.stations.StationListPreferences
import com.archimede.w2full.ui.stations.StationSortMode
import com.archimede.w2full.ui.vehicle.VehicleSettingsRoute
import java.util.Locale

private enum class SettingsPage {
    HOME,
    VEHICLE,
    FAVORITES,
    STATIONS,
    HISTORY,
    INFO,
}

@Composable
fun SettingsRoute() {
    val application = LocalContext.current.applicationContext as W2FullApplication
    val factory = remember(application) {
        SettingsViewModel.Factory(
            nearbyStationsRepository = application.nearbyStationsRepository,
            favoriteStationsStore = application.historyFavoriteStationsStore,
            stationPreferencesStore = application.stationListPreferencesStore,
            historyPreferencesStore = application.historyPreferencesStore,
        )
    }
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }

    LaunchedEffect(page) {
        if (page == SettingsPage.HOME || page == SettingsPage.FAVORITES) viewModel.reload()
    }

    when (page) {
        SettingsPage.HOME -> SettingsHome(onOpen = { page = it })
        SettingsPage.VEHICLE -> VehicleSettingsRoute(onBack = { page = SettingsPage.HOME })
        SettingsPage.FAVORITES -> FavoritesSettingsScreen(
            state = state,
            onBack = { page = SettingsPage.HOME },
            onRemove = viewModel::removeFavorite,
        )
        SettingsPage.STATIONS -> StationDefaultsScreen(
            state = state,
            onBack = { page = SettingsPage.HOME },
            onRadiusEnabled = viewModel::setRadiusEnabled,
            onRadiusKm = viewModel::setRadiusKm,
            onSortMode = viewModel::setStationSortMode,
        )
        SettingsPage.HISTORY -> HistoryDefaultsScreen(
            state = state,
            onBack = { page = SettingsPage.HOME },
            onPeriod = viewModel::setHistoryPeriod,
            onSeriesBEnabled = viewModel::setSeriesBEnabled,
            onShowTable = viewModel::setShowTable,
        )
        SettingsPage.INFO -> InfoSettingsScreen(onBack = { page = SettingsPage.HOME })
    }
}

@Composable
private fun SettingsHome(onOpen: (SettingsPage) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("⚙ Impostazioni", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Tutte le configurazioni di W2Full in un unico posto.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsCard("🚗", "Veicolo", "Carburante predefinito e dati veicolo") { onOpen(SettingsPage.VEHICLE) }
        SettingsCard("★", "Stazioni preferite", "Visualizza e rimuovi le tue preferite") { onOpen(SettingsPage.FAVORITES) }
        SettingsCard("⛽", "Stazioni", "Raggio e ordinamento predefiniti") { onOpen(SettingsPage.STATIONS) }
        SettingsCard("📈", "Storico", "Periodo e opzioni del grafico") { onOpen(SettingsPage.HISTORY) }
        SettingsCard("ⓘ", "Informazioni", "Versione dell'app") { onOpen(SettingsPage.INFO) }
    }
}

@Composable
private fun SettingsCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$icon  $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onClick) { Text("Apri") }
        }
    }
}

@Composable
private fun FavoritesSettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackHeader("Stazioni preferite", onBack)
        if (state.favorites.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("Nessuna stazione preferita. Usa la stella nella schermata Stazioni.", modifier = Modifier.padding(16.dp))
            }
        } else {
            state.favorites.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (item.address.isNotBlank()) {
                            Text(item.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item.distanceKm?.let {
                            Text(String.format(Locale.ITALY, "%.1f km", it), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { onRemove(item.stationId) }) { Text("Rimuovi dai preferiti") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationDefaultsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRadiusEnabled: (Boolean) -> Unit,
    onRadiusKm: (Int) -> Unit,
    onSortMode: (StationSortMode) -> Unit,
) {
    val prefs = state.stationPreferences
    var radiusText by remember(prefs.radiusKm) { mutableStateOf(prefs.radiusKm.toString()) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackHeader("Impostazioni Stazioni", onBack)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Limita distanza")
                    Switch(checked = prefs.radiusEnabled, onCheckedChange = onRadiusEnabled)
                }
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all(Char::isDigit)) radiusText = value
                    },
                    label = { Text("Raggio massimo (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    radiusText.toIntOrNull()
                        ?.takeIf { it in StationListPreferences.MIN_RADIUS_KM..StationListPreferences.MAX_RADIUS_KM }
                        ?.let(onRadiusKm)
                }) { Text("Salva raggio") }
                Text("Ordinamento predefinito", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DefaultSortChip("Distanza", StationSortMode.DISTANCE, prefs.sortMode, onSortMode)
                    DefaultSortChip("Prezzo Self", StationSortMode.SELF_PRICE, prefs.sortMode, onSortMode)
                    DefaultSortChip("Prezzo Servito", StationSortMode.SERVED_PRICE, prefs.sortMode, onSortMode)
                }
            }
        }
        Text("Questi valori restano modificabili rapidamente anche nella schermata Stazioni.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DefaultSortChip(
    label: String,
    mode: StationSortMode,
    selected: StationSortMode,
    onSortMode: (StationSortMode) -> Unit,
) {
    FilterChip(selected = mode == selected, onClick = { onSortMode(mode) }, label = { Text(label) })
}

@Composable
private fun HistoryDefaultsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onPeriod: (HistoryPeriod) -> Unit,
    onSeriesBEnabled: (Boolean) -> Unit,
    onShowTable: (Boolean) -> Unit,
) {
    val prefs = state.historyPreferences
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackHeader("Impostazioni Storico", onBack)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Periodo predefinito", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryPeriod.values().forEach { period ->
                        FilterChip(
                            selected = period == prefs.period,
                            onClick = { onPeriod(period) },
                            label = { Text(historyPeriodLabel(period)) },
                        )
                    }
                }
                SettingsSwitch("Seconda serie attiva", prefs.seriesBEnabled, onSeriesBEnabled)
                SettingsSwitch("Mostra tabella dati", prefs.showTable, onShowTable)
            }
        }
        Text(
            "Carburante e servizio di Serie A e Serie B si scelgono direttamente nello Storico, perché dipendono dai dati disponibili della stazione.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoSettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackHeader("Informazioni", onBack)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("W2Full", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Versione ${BuildConfig.VERSION_NAME}")
                Text("Dati carburanti: dataset ufficiali MIMIT", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dati e preferenze restano locali sul dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onBack) { Text("← Impostazioni") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

private fun historyPeriodLabel(period: HistoryPeriod): String = when (period) {
    HistoryPeriod.ONE_MONTH -> "1 mese"
    HistoryPeriod.THREE_MONTHS -> "3 mesi"
    HistoryPeriod.SIX_MONTHS -> "6 mesi"
    HistoryPeriod.ONE_YEAR -> "1 anno"
    HistoryPeriod.ALL -> "Tutto"
}