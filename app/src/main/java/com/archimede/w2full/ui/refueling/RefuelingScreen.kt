package com.archimede.w2full.ui.refueling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.domain.calculation.FuelMetricsCalculator
import com.archimede.w2full.domain.model.Rifornimento
import java.time.LocalDate
import java.util.Locale

@Composable
fun RefuelingRoute() {
    val application = LocalContext.current.applicationContext as W2FullApplication
    val factory = remember(application) {
        RefuelingViewModel.Factory(application.refuelingRepository)
    }
    val viewModel: RefuelingViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RefuelingScreen(
        state = state,
        onInsert = viewModel::insert,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete,
        onUpdateTankCapacity = viewModel::updateTankCapacity,
        onClearError = viewModel::clearError,
    )
}

@Composable
private fun RefuelingScreen(
    state: RefuelingUiState,
    onInsert: (com.archimede.w2full.domain.model.RifornimentoDraft) -> Unit,
    onUpdate: (Rifornimento, com.archimede.w2full.domain.model.RifornimentoDraft) -> Unit,
    onDelete: (Long) -> Unit,
    onUpdateTankCapacity: (Long) -> Unit,
    onClearError: () -> Unit,
) {
    var editing by remember { mutableStateOf<Rifornimento?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Rifornimento?>(null) }
    var showTankDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                text = "Registro rifornimenti",
                style = MaterialTheme.typography.titleLarge,
            )

            MetricsRow(state)

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Serbatoio", style = MaterialTheme.typography.labelLarge)
                        Text(
                            state.vehicle?.tankCapacityMilliliters
                                ?.let { formatLiters(it) }
                                ?: "Capacità non configurata",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showTankDialog = true }) {
                        Text("Configura")
                    }
                }
            }

            state.errorMessage?.let { error ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onClearError) { Text("OK") }
                    }
                }
            }

            if (state.refuelings.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nessun rifornimento. Usa + per registrare il primo.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.refuelings, key = { it.id }) { refueling ->
                        RefuelingCard(
                            refueling = refueling,
                            onEdit = { editing = refueling },
                            onDelete = { deleting = refueling },
                        )
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = 40.dp)) }
                }
            }
        }
    }

    if (showCreateDialog) {
        RefuelingEditorDialog(
            existing = null,
            defaultFuelType = state.vehicle?.defaultFuelType ?: "Benzina",
            onDismiss = { showCreateDialog = false },
            onSave = { draft ->
                onInsert(draft)
                showCreateDialog = false
            },
        )
    }

    editing?.let { existing ->
        RefuelingEditorDialog(
            existing = existing,
            defaultFuelType = existing.fuelType,
            onDismiss = { editing = null },
            onSave = { draft ->
                onUpdate(existing, draft)
                editing = null
            },
        )
    }

    deleting?.let { refueling ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminare il rifornimento?") },
            text = { Text("${RefuelingFormParser.formatDate(refueling.timestampEpochMillis)} · ${refueling.odometerKm} km") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(refueling.id)
                        deleting = null
                    },
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Annulla") }
            },
        )
    }

    if (showTankDialog) {
        TankCapacityDialog(
            currentMilliliters = state.vehicle?.tankCapacityMilliliters,
            onDismiss = { showTankDialog = false },
            onSave = { capacity ->
                onUpdateTankCapacity(capacity)
                showTankDialog = false
            },
        )
    }
}

@Composable
private fun MetricsRow(state: RefuelingUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricCard(
            label = "Consumo",
            value = state.metrics.averageConsumptionLPer100Km
                ?.let { formatDecimal(it, 2) }
                ?.plus(" L/100")
                ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Costo/km",
            value = state.metrics.costPerKmEuro
                ?.let { "€ ${formatDecimal(it, 3)}" }
                ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Autonomia",
            value = state.metrics.estimatedRangeKm
                ?.let { "${formatDecimal(it, 0)} km" }
                ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RefuelingCard(
    refueling: Rifornimento,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = RefuelingFormParser.formatDate(refueling.timestampEpochMillis),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (refueling.isFullTank) "Pieno" else "Parziale",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text("${refueling.odometerKm} km · ${formatLiters(refueling.litersMilliliters)} · ${formatEuro(refueling.totalCostCents)}")
            val price = FuelMetricsCalculator.pricePerLiterEuro(refueling)
            Text(
                text = buildString {
                    append(refueling.fuelType)
                    price?.let { append(" · € ${formatDecimal(it, 3)}/L") }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) { Text("Modifica") }
                TextButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}

@Composable
private fun RefuelingEditorDialog(
    existing: Rifornimento?,
    defaultFuelType: String,
    onDismiss: () -> Unit,
    onSave: (com.archimede.w2full.domain.model.RifornimentoDraft) -> Unit,
) {
    var dateText by remember(existing) {
        mutableStateOf(
            existing?.let { RefuelingFormParser.formatDate(it.timestampEpochMillis) }
                ?: LocalDate.now().toString(),
        )
    }
    var odometerText by remember(existing) { mutableStateOf(existing?.odometerKm?.toString().orEmpty()) }
    var litersText by remember(existing) {
        mutableStateOf(existing?.let { formatDecimal(it.litersMilliliters / 1000.0, 3) }.orEmpty())
    }
    var costText by remember(existing) {
        mutableStateOf(existing?.let { formatDecimal(it.totalCostCents / 100.0, 2) }.orEmpty())
    }
    var fuelTypeText by remember(existing, defaultFuelType) {
        mutableStateOf(existing?.fuelType ?: defaultFuelType)
    }
    var isFullTank by remember(existing) { mutableStateOf(existing?.isFullTank ?: true) }
    var formError by remember(existing) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuovo rifornimento" else "Modifica rifornimento") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Data (AAAA-MM-GG)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("Odometro km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = it },
                    label = { Text("Litri") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Costo totale €") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fuelTypeText,
                    onValueChange = { fuelTypeText = it },
                    label = { Text("Carburante") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it },
                    )
                    Text("Serbatoio riportato a pieno")
                }
                formError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        RefuelingFormParser.parseDraft(
                            dateText = dateText,
                            odometerText = odometerText,
                            litersText = litersText,
                            totalCostText = costText,
                            fuelTypeText = fuelTypeText,
                            isFullTank = isFullTank,
                        )
                    }.onSuccess {
                        formError = null
                        onSave(it)
                    }.onFailure {
                        formError = it.message ?: "Dati non validi."
                    }
                },
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Composable
private fun TankCapacityDialog(
    currentMilliliters: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var capacityText by remember(currentMilliliters) {
        mutableStateOf(
            currentMilliliters?.let { formatDecimal(it / 1000.0, 3) }.orEmpty(),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capacità serbatoio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = { Text("Litri") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(
                    "Serve per stimare carburante residuo e autonomia.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching { RefuelingFormParser.parseTankCapacityMilliliters(capacityText) }
                        .onSuccess {
                            error = null
                            onSave(it)
                        }
                        .onFailure { error = it.message ?: "Capacità non valida." }
                },
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

private fun formatLiters(milliliters: Long): String =
    "${formatDecimal(milliliters / 1000.0, 3)} L"

private fun formatEuro(cents: Long): String =
    "€ ${formatDecimal(cents / 100.0, 2)}"

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.ITALY, "%.${decimals}f", value)
