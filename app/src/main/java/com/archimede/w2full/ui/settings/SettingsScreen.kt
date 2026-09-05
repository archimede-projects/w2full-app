package com.archimede.w2full.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.BuildConfig
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.ui.vehicle.VehicleSettingsRoute
import java.util.Locale

private enum class SettingsPage {
    HOME,
    VEHICLE,
    FAVORITES,
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
        SettingsPage.INFO -> InfoSettingsScreen(onBack = { page = SettingsPage.HOME })
    }
}

@Composable
private fun SettingsHome(onOpen: (SettingsPage) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("⚙ Impostazioni", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Solo le impostazioni globali. I filtri restano dove li usi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsCard(
            icon = "🚗",
            title = "Veicolo",
            subtitle = "Carburante predefinito e dati della tua auto",
            modifier = Modifier.weight(1f),
        ) { onOpen(SettingsPage.VEHICLE) }
        SettingsCard(
            icon = "★",
            title = "Stazioni preferite",
            subtitle = "Gestisci le stazioni che vuoi seguire",
            modifier = Modifier.weight(1f),
        ) { onOpen(SettingsPage.FAVORITES) }
        SettingsCard(
            icon = "ⓘ",
            title = "Informazioni",
            subtitle = "Versione, dati MIMIT e privacy locale",
            modifier = Modifier.weight(1f),
        ) { onOpen(SettingsPage.INFO) }
    }
}

@Composable
private fun SettingsCard(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackHeader("Stazioni preferite", onBack)
        Text(
            "Aggiungile o toglile anche dalla stella grande nella schermata Stazioni.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.favorites.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("Nessuna stazione preferita.", modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.favorites, key = { it.stationId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (item.address.isNotBlank()) {
                                Text(item.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            item.distanceKm?.let {
                                Text(String.format(Locale.ITALY, "%.1f km", it), color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = { onRemove(item.stationId) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text("★ Rimuovi dai preferiti")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackHeader("Informazioni", onBack)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("←") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}
