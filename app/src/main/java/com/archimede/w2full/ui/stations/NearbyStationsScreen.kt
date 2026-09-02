package com.archimede.w2full.ui.stations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.data.mimit.MimitStationDistance
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
        NearbyStationsViewModel.Factory(application.nearbyStationsRepository)
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
        onRetryLocation = viewModel::refresh,
        onRetryLoad = viewModel::refresh,
    )
}

@Composable
private fun NearbyStationsScreen(
    state: NearbyStationsUiState,
    onRequestLocationPermission: () -> Unit,
    onRetryLocation: () -> Unit,
    onRetryLoad: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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

        UpdateStatusCard(state)
        LocationStatusCard(
            status = state.locationStatus,
            onRequestLocationPermission = onRequestLocationPermission,
            onRetryLocation = onRetryLocation,
        )

        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(message)
                    TextButton(onClick = onRetryLoad) {
                        Text("Riprova")
                    }
                }
            }
        }

        when {
            state.stations.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.stations,
                        key = { it.station.id },
                    ) { item ->
                        StationCard(item)
                    }
                }
            }

            !state.isLoading && state.errorMessage == null -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nessuna stazione Eni disponibile.",
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(state: NearbyStationsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = lastUpdateLabel(state.lastSuccessfulUpdateEpochMillis),
                style = MaterialTheme.typography.labelLarge,
            )
            state.extractionDate?.let { extractionDate ->
                Text(
                    text = "Estrazione MIMIT: $extractionDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
                        Text("Consenti")
                    }
                }

                NearbyLocationUiStatus.UNAVAILABLE -> {
                    TextButton(onClick = onRetryLocation) {
                        Text("Riprova")
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
private fun StationCard(item: MimitStationDistance) {
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
    NearbyLocationUiStatus.AVAILABLE -> "Stazioni ordinate per distanza"
    NearbyLocationUiStatus.PERMISSION_DENIED -> "Stazioni ordinate alfabeticamente"
    NearbyLocationUiStatus.UNAVAILABLE -> "Stazioni ordinate alfabeticamente"
    null -> "Verifica della posizione in corso"
}

internal fun lastUpdateLabel(epochMillis: Long?): String {
    if (epochMillis == null) {
        return "Ultimo aggiornamento: non ancora disponibile"
    }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val dateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
    return "Ultimo aggiornamento: $dateTime"
}

private fun formatDistanceKm(distanceKm: Double): String =
    String.format(Locale.ITALY, "%.1f km", distanceKm)

private fun Context.hasAnyLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
