package com.archimede.w2full.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archimede.w2full.W2FullApplication
import com.archimede.w2full.alerts.PriceAlertEvaluationOutcome
import com.archimede.w2full.alerts.PriceAlertEvaluator
import com.archimede.w2full.data.repository.PriceAlertConfig
import com.archimede.w2full.data.repository.PriceAlertRepository
import com.archimede.w2full.data.repository.parsePriceAlertInputToMilliEuro
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PriceAlertSettingsUiState(
    val isLoading: Boolean = true,
    val fuelOptions: List<String> = emptyList(),
    val fuelDescription: String = PriceAlertRepository.DEFAULT_FUEL,
    val thresholdInput: String = "1,800",
    val isSelf: Boolean = true,
    val radiusEnabled: Boolean = true,
    val radiusInput: String = PriceAlertRepository.DEFAULT_RADIUS_KM.toString(),
    val isActive: Boolean = false,
    val message: String? = null,
)

class PriceAlertSettingsViewModel(
    private val repository: PriceAlertRepository,
    private val evaluator: PriceAlertEvaluator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PriceAlertSettingsUiState())
    val uiState: StateFlow<PriceAlertSettingsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val rule = repository.loadRuleOrDefault()
            val fuels = repository.availableFuelDescriptions()
            _uiState.value = PriceAlertSettingsUiState(
                isLoading = false,
                fuelOptions = fuels.ifEmpty { listOf(rule.fuelDescription) },
                fuelDescription = rule.fuelDescription,
                thresholdInput = formatThreshold(rule.maxPriceMilliEuroPerUnit),
                isSelf = rule.isSelf,
                radiusEnabled = rule.radiusKm != null,
                radiusInput = (rule.radiusKm ?: PriceAlertRepository.DEFAULT_RADIUS_KM).toString(),
                isActive = rule.isActive,
            )
        }
    }

    fun setFuel(value: String) {
        _uiState.value = _uiState.value.copy(fuelDescription = value, message = null)
    }

    fun setThresholdInput(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() || it == ',' || it == '.' }) {
            _uiState.value = _uiState.value.copy(thresholdInput = value, message = null)
        }
    }

    fun setSelf(value: Boolean) {
        _uiState.value = _uiState.value.copy(isSelf = value, message = null)
    }

    fun setRadiusEnabled(value: Boolean) {
        _uiState.value = _uiState.value.copy(radiusEnabled = value, message = null)
    }

    fun setRadiusInput(value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) {
            _uiState.value = _uiState.value.copy(radiusInput = value, message = null)
        }
    }

    fun setActive(value: Boolean, permissionDenied: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            isActive = value,
            message = if (permissionDenied) "Permesso notifiche non concesso: avviso disattivato." else null,
        )
    }

    fun save(notificationsAllowed: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val price = parsePriceAlertInputToMilliEuro(state.thresholdInput)
            if (price == null) {
                _uiState.value = state.copy(message = "Inserisci una soglia tra 0,500 e 5,000 €/L.")
                return@launch
            }
            val radius = if (state.radiusEnabled) state.radiusInput.toIntOrNull() else null
            if (state.radiusEnabled && (radius == null || radius !in PriceAlertRepository.MIN_RADIUS_KM..PriceAlertRepository.MAX_RADIUS_KM)) {
                _uiState.value = state.copy(message = "Inserisci un raggio da 1 a 200 km.")
                return@launch
            }
            val active = state.isActive && notificationsAllowed
            val saved = repository.save(
                config = PriceAlertConfig(
                    fuelDescription = state.fuelDescription,
                    maxPriceMilliEuroPerUnit = price,
                    isSelf = state.isSelf,
                    radiusKm = radius,
                ),
                isActive = active,
            )
            val outcome = if (saved.isActive) evaluator.evaluate() else PriceAlertEvaluationOutcome.INACTIVE
            _uiState.value = state.copy(
                isActive = saved.isActive,
                thresholdInput = formatThreshold(saved.maxPriceMilliEuroPerUnit),
                message = when {
                    state.isActive && !notificationsAllowed -> "Permesso notifiche necessario: configurazione salvata ma avviso disattivato."
                    outcome == PriceAlertEvaluationOutcome.NOTIFIED -> "Configurazione salvata. Trovato un prezzo sotto soglia."
                    outcome == PriceAlertEvaluationOutcome.SKIPPED_LOCATION -> "Salvato. Apri Stazioni per aggiornare la posizione usata dal raggio."
                    outcome == PriceAlertEvaluationOutcome.NOTIFICATION_BLOCKED -> "Salvato, ma Android non consente ancora le notifiche."
                    else -> "Configurazione salvata."
                },
            )
        }
    }

    private fun formatThreshold(milliEuro: Long): String =
        String.format(Locale.ITALY, "%.3f", milliEuro / 1000.0)

    class Factory(
        private val repository: PriceAlertRepository,
        private val evaluator: PriceAlertEvaluator,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PriceAlertSettingsViewModel::class.java))
            return PriceAlertSettingsViewModel(repository, evaluator) as T
        }
    }
}

@Composable
fun PriceAlertSettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as W2FullApplication
    val factory = remember(application) {
        PriceAlertSettingsViewModel.Factory(
            repository = application.priceAlertRepository,
            evaluator = application.priceAlertEvaluator,
        )
    }
    val viewModel: PriceAlertSettingsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setActive(granted, permissionDenied = !granted) }
    val notificationsAllowed = { context.notificationsAllowedForM6() }

    PriceAlertSettingsScreen(
        state = state,
        onBack = onBack,
        onFuelSelected = viewModel::setFuel,
        onThresholdChanged = viewModel::setThresholdInput,
        onSelfChanged = viewModel::setSelf,
        onRadiusEnabledChanged = viewModel::setRadiusEnabled,
        onRadiusChanged = viewModel::setRadiusInput,
        onActiveChanged = { enabled ->
            if (enabled && !notificationsAllowed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setActive(enabled)
            }
        },
        onSave = { viewModel.save(notificationsAllowed()) },
        notificationsAllowed = notificationsAllowed(),
    )
}

@Composable
private fun PriceAlertSettingsScreen(
    state: PriceAlertSettingsUiState,
    onBack: () -> Unit,
    onFuelSelected: (String) -> Unit,
    onThresholdChanged: (String) -> Unit,
    onSelfChanged: (Boolean) -> Unit,
    onRadiusEnabledChanged: (Boolean) -> Unit,
    onRadiusChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    notificationsAllowed: Boolean,
) {
    var fuelMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("←") }
            Text("Avviso prezzo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Avviso locale", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (notificationsAllowed) "Notifiche Android disponibili" else "Permesso notifiche non concesso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.isActive, onCheckedChange = onActiveChanged)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Carburante", fontWeight = FontWeight.Medium)
            Column(horizontalAlignment = Alignment.End) {
                OutlinedButton(
                    onClick = { fuelMenuOpen = true },
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 150.dp),
                ) { Text(state.fuelDescription, maxLines = 1) }
                DropdownMenu(expanded = fuelMenuOpen, onDismissRequest = { fuelMenuOpen = false }) {
                    state.fuelOptions.forEach { fuel ->
                        DropdownMenuItem(
                            text = { Text(fuel) },
                            onClick = {
                                onFuelSelected(fuel)
                                fuelMenuOpen = false
                            },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Modalità", modifier = Modifier.widthIn(min = 86.dp), fontWeight = FontWeight.Medium)
            FilterChip(
                selected = state.isSelf,
                onClick = { onSelfChanged(true) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text("Self") },
            )
            FilterChip(
                selected = !state.isSelf,
                onClick = { onSelfChanged(false) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text("Servito") },
            )
        }

        OutlinedTextField(
            value = state.thresholdInput,
            onValueChange = onThresholdChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Soglia massima €/L") },
            supportingText = { Text("Da 0,500 a 5,000 €/L") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Limita per raggio", fontWeight = FontWeight.Medium)
                Text("Usa l'ultima posizione rilevata in Stazioni", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = state.radiusEnabled, onCheckedChange = onRadiusEnabledChanged)
        }

        if (state.radiusEnabled) {
            OutlinedTextField(
                value = state.radiusInput,
                onValueChange = onRadiusChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Raggio km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = onSave,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Salva avviso") }
    }
}

private fun Context.notificationsAllowedForM6(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
